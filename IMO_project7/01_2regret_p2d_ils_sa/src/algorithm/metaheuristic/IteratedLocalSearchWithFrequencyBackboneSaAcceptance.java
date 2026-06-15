package algorithm.metaheuristic;

import algorithm.CycleImprover;
import algorithm.IterationCountingAlgorithm;
import algorithm.OptimizationAlgorithm;
import algorithm.SolutionObjective;
import algorithm.localsearch.Cycle;
import model.Instance;
import model.Solution;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

/**
 * ILS-SA z dynamicznym backbone liczonym przez czestosc krawedzi w elite archive.
 *
 * <p>Dla archiveCapacity=3 i minEdgeFrequency=3 dostajemy klasyczne commonEdges top3.
 * Dla wiekszego archive chronione sa krawedzie powtarzajace sie w kilku dobrych lokalnych optimach.</p>
 */
public final class IteratedLocalSearchWithFrequencyBackboneSaAcceptance implements OptimizationAlgorithm, IterationCountingAlgorithm, SaAcceptanceStatistics {
    private final String name;
    private final OptimizationAlgorithm startAlgorithm;
    private final CycleImprover localSearch;
    private final long timeLimitNanos;
    private final Random random;
    private final double initialTemperature;
    private final double finalTemperature;
    private final CoolingSchedule coolingSchedule;
    private final int swapEdgesCount;
    private final int archiveCapacity;
    private final int minEdgeFrequency;
    private final double fixedProtectedBreakProbability;
    private final boolean adaptiveProtection;

    private int lastIterationCount;
    private int acceptedBetterCount;
    private int acceptedWorseCount;
    private int rejectedWorseCount;
    private int bestFoundIteration;
    private int eliteUpdateCount;
    private int lastProtectedEdgesCount;
    private int minProtectedEdgesCount;
    private int maxProtectedEdgesCount;
    private long protectedEdgesSum;
    private int protectedEdgesMeasurements;

    public IteratedLocalSearchWithFrequencyBackboneSaAcceptance(
            String name,
            OptimizationAlgorithm startAlgorithm,
            CycleImprover localSearch,
            long timeLimitNanos,
            long seed,
            double initialTemperature,
            double finalTemperature,
            CoolingSchedule coolingSchedule,
            int swapEdgesCount,
            int archiveCapacity,
            int minEdgeFrequency,
            double fixedProtectedBreakProbability,
            boolean adaptiveProtection
    ) {
        this.name = Objects.requireNonNull(name, "name");
        this.startAlgorithm = Objects.requireNonNull(startAlgorithm, "startAlgorithm");
        this.localSearch = Objects.requireNonNull(localSearch, "localSearch");
        this.timeLimitNanos = timeLimitNanos;
        this.random = new Random(seed);
        this.initialTemperature = initialTemperature;
        this.finalTemperature = finalTemperature;
        this.coolingSchedule = Objects.requireNonNull(coolingSchedule, "coolingSchedule");
        this.swapEdgesCount = swapEdgesCount;
        this.archiveCapacity = archiveCapacity;
        this.minEdgeFrequency = minEdgeFrequency;
        this.fixedProtectedBreakProbability = fixedProtectedBreakProbability;
        this.adaptiveProtection = adaptiveProtection;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Solution solve(Instance instance, int startVertexId) {
        resetStats();

        long startTime = System.nanoTime();
        long endTime = startTime + timeLimitNanos;

        Solution start = startAlgorithm.solve(instance, startVertexId);
        Cycle currentCycle = new Cycle(start.cycle(), instance.size);
        localSearch.improve(instance, currentCycle);
        int currentObjective = SolutionObjective.calculate(instance, currentCycle);

        Cycle bestCycle = new Cycle(currentCycle);
        int bestObjective = currentObjective;

        FrequencyEliteArchive elite = new FrequencyEliteArchive(archiveCapacity, minEdgeFrequency);
        Set<Long> protectedEdges = Set.of();
        if (elite.offer(currentCycle, currentObjective)) {
            protectedEdges = elite.protectedEdges();
            recordBackboneUpdate(protectedEdges.size());
        }

        while (System.nanoTime() < endTime) {
            long now = System.nanoTime();
            double temperature = temperature(now - startTime);

            Cycle candidateCycle = new Cycle(currentCycle);
            applyProtectedRandomSwapEdges(candidateCycle, protectedEdges, random);
            localSearch.improve(instance, candidateCycle);
            int candidateObjective = SolutionObjective.calculate(instance, candidateCycle);
            int delta = candidateObjective - currentObjective;

            lastIterationCount++;

            if (elite.offer(candidateCycle, candidateObjective)) {
                protectedEdges = elite.protectedEdges();
                recordBackboneUpdate(protectedEdges.size());
            }

            boolean accepted = false;
            if (delta > 0) {
                acceptedBetterCount++;
                accepted = true;
            } else if (random.nextDouble() < Math.exp(delta / temperature)) {
                acceptedWorseCount++;
                accepted = true;
            } else {
                rejectedWorseCount++;
            }

            if (accepted) {
                currentCycle = candidateCycle;
                currentObjective = candidateObjective;

                if (currentObjective > bestObjective) {
                    bestCycle = new Cycle(currentCycle);
                    bestObjective = currentObjective;
                    bestFoundIteration = lastIterationCount;
                }
            }
        }

        return new Solution(start.instanceName(), start.startVertexId(), bestCycle.toList());
    }

    private void resetStats() {
        lastIterationCount = 0;
        acceptedBetterCount = 0;
        acceptedWorseCount = 0;
        rejectedWorseCount = 0;
        bestFoundIteration = 0;
        eliteUpdateCount = 0;
        lastProtectedEdgesCount = 0;
        minProtectedEdgesCount = 0;
        maxProtectedEdgesCount = 0;
        protectedEdgesSum = 0;
        protectedEdgesMeasurements = 0;
    }

    private void recordBackboneUpdate(int protectedEdgesCount) {
        eliteUpdateCount++;
        lastProtectedEdgesCount = protectedEdgesCount;
        if (protectedEdgesMeasurements == 0) {
            minProtectedEdgesCount = protectedEdgesCount;
            maxProtectedEdgesCount = protectedEdgesCount;
        } else {
            minProtectedEdgesCount = Math.min(minProtectedEdgesCount, protectedEdgesCount);
            maxProtectedEdgesCount = Math.max(maxProtectedEdgesCount, protectedEdgesCount);
        }
        protectedEdgesSum += protectedEdgesCount;
        protectedEdgesMeasurements++;
    }

    private void applyProtectedRandomSwapEdges(Cycle cycle, Set<Long> protectedEdges, Random random) {
        for (int move = 0; move < swapEdgesCount; move++) {
            applyOneProtectedSwapEdges(cycle, protectedEdges, random);
        }
    }

    private void applyOneProtectedSwapEdges(Cycle cycle, Set<Long> protectedEdges, Random random) {
        if (cycle.size() < 4) {
            return;
        }

        if (protectedEdges.isEmpty()) {
            applyNormalSwapEdges(cycle, random);
            return;
        }

        double breakProbability = protectedBreakProbability(protectedEdges.size());
        for (int attempt = 0; attempt < 50; attempt++) {
            int firstPosition = random.nextInt(cycle.size());
            int secondPosition = random.nextInt(cycle.size());
            if (areAdjacentEdges(cycle, firstPosition, secondPosition)) {
                continue;
            }

            boolean breaksProtected = protectedEdges.contains(edgeAt(cycle, firstPosition))
                    || protectedEdges.contains(edgeAt(cycle, secondPosition));
            if (!breaksProtected || random.nextDouble() < breakProbability) {
                int leftPosition = Math.min(firstPosition, secondPosition);
                int rightPosition = Math.max(firstPosition, secondPosition);
                cycle.reverseFragment(leftPosition + 1, rightPosition);
                return;
            }
        }
    }

    private double protectedBreakProbability(int protectedEdgesCount) {
        if (!adaptiveProtection) {
            return fixedProtectedBreakProbability;
        }
        if (protectedEdgesCount > 130) {
            return 0.60;
        }
        if (protectedEdgesCount >= 70) {
            return 0.30;
        }
        return 0.15;
    }

    private static void applyNormalSwapEdges(Cycle cycle, Random random) {
        int firstPosition;
        int secondPosition;
        do {
            firstPosition = random.nextInt(cycle.size());
            secondPosition = random.nextInt(cycle.size());
        } while (areAdjacentEdges(cycle, firstPosition, secondPosition));

        int leftPosition = Math.min(firstPosition, secondPosition);
        int rightPosition = Math.max(firstPosition, secondPosition);
        cycle.reverseFragment(leftPosition + 1, rightPosition);
    }

    private static boolean areAdjacentEdges(Cycle cycle, int firstPosition, int secondPosition) {
        return firstPosition == secondPosition || cycle.areAdjacentPositions(firstPosition, secondPosition);
    }

    private static long edgeAt(Cycle cycle, int position) {
        int a = cycle.cycle[position];
        int b = cycle.cycle[cycle.nextIndex(position)];
        return edgeKey(a, b);
    }

    private double temperature(long elapsedNanos) {
        double progress = (double) elapsedNanos / (double) timeLimitNanos;
        if (progress > 1.0) {
            progress = 1.0;
        }
        if (coolingSchedule == CoolingSchedule.LINEAR) {
            return initialTemperature + progress * (finalTemperature - initialTemperature);
        }
        return initialTemperature * Math.pow(finalTemperature / initialTemperature, progress);
    }

    @Override
    public int lastIterationCount() { return lastIterationCount; }
    @Override
    public int acceptedBetterCount() { return acceptedBetterCount; }
    @Override
    public int acceptedWorseCount() { return acceptedWorseCount; }
    @Override
    public int rejectedWorseCount() { return rejectedWorseCount; }
    @Override
    public int bestFoundIteration() { return bestFoundIteration; }

    public int eliteUpdateCount() { return eliteUpdateCount; }
    public int lastProtectedEdgesCount() { return lastProtectedEdgesCount; }
    public int minProtectedEdgesCount() { return minProtectedEdgesCount; }
    public int maxProtectedEdgesCount() { return maxProtectedEdgesCount; }
    public double avgProtectedEdgesOnUpdate() {
        return protectedEdgesMeasurements == 0 ? 0.0 : (double) protectedEdgesSum / protectedEdgesMeasurements;
    }

    private static long edgeKey(int a, int b) {
        int u = Math.min(a, b);
        int v = Math.max(a, b);
        return ((long) u << 32) | (v & 0xffffffffL);
    }

    private static Set<Long> edgesOf(Cycle cycle) {
        Set<Long> edges = new HashSet<>();
        for (int i = 0; i < cycle.size(); i++) {
            edges.add(edgeAt(cycle, i));
        }
        return edges;
    }

    private static String edgeSetKey(Set<Long> edges) {
        List<Long> sorted = new ArrayList<>(edges);
        sorted.sort(Long::compareTo);
        StringBuilder builder = new StringBuilder(sorted.size() * 12);
        for (Long edge : sorted) {
            builder.append(edge).append(';');
        }
        return builder.toString();
    }

    private static final class FrequencyEliteArchive {
        private final int capacity;
        private final int minFrequency;
        private final List<EliteItem> items;

        private FrequencyEliteArchive(int capacity, int minFrequency) {
            this.capacity = capacity;
            this.minFrequency = minFrequency;
            this.items = new ArrayList<>(capacity);
        }

        private boolean offer(Cycle cycle, int objective) {
            Set<Long> edges = edgesOf(cycle);
            String key = edgeSetKey(edges);
            for (EliteItem item : items) {
                if (item.key().equals(key)) {
                    return false;
                }
            }
            EliteItem newItem = new EliteItem(new Cycle(cycle), objective, edges, key);
            if (items.size() < capacity) {
                items.add(newItem);
                sortItems();
                return true;
            }
            int worstIndex = worstIndex();
            if (objective > items.get(worstIndex).objective()) {
                items.set(worstIndex, newItem);
                sortItems();
                return true;
            }
            return false;
        }

        private Set<Long> protectedEdges() {
            if (items.size() < minFrequency) {
                return Set.of();
            }
            Map<Long, Integer> counts = new HashMap<>();
            for (EliteItem item : items) {
                for (Long edge : item.edges()) {
                    counts.merge(edge, 1, Integer::sum);
                }
            }
            Set<Long> protectedEdges = new HashSet<>();
            for (Map.Entry<Long, Integer> entry : counts.entrySet()) {
                if (entry.getValue() >= minFrequency) {
                    protectedEdges.add(entry.getKey());
                }
            }
            return protectedEdges;
        }

        private int worstIndex() {
            int index = 0;
            int objective = items.get(0).objective();
            for (int i = 1; i < items.size(); i++) {
                if (items.get(i).objective() < objective) {
                    index = i;
                    objective = items.get(i).objective();
                }
            }
            return index;
        }

        private void sortItems() {
            items.sort(Comparator.comparingInt(EliteItem::objective).reversed());
        }
    }

    private record EliteItem(Cycle cycle, int objective, Set<Long> edges, String key) {}
}
