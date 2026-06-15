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
 * ILS-SA z dynamicznym backbone liczonym z elity wymuszajacej roznorodnosc.
 */
public final class IteratedLocalSearchWithDiverseDynamicEliteBackboneSaAcceptance implements OptimizationAlgorithm, IterationCountingAlgorithm, SaAcceptanceStatistics {
    public enum EliteMode {
        DIVERSE_MIN_DIST,
        ARCHIVE5_DIVERSE_TOP3_ALL_COMMON,
        ARCHIVE5_DIVERSE_TOP3_MAJORITY2OF3,
        BEST_FAR_COMPATIBLE
    }

    private final String name;
    private final OptimizationAlgorithm startAlgorithm;
    private final CycleImprover localSearch;
    private final long timeLimitNanos;
    private final Random random;
    private final double initialTemperature;
    private final double finalTemperature;
    private final CoolingSchedule coolingSchedule;
    private final int swapEdgesCount;
    private final double protectedBreakProbability;
    private final EliteMode eliteMode;
    private final double minDistanceThreshold;

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

    public IteratedLocalSearchWithDiverseDynamicEliteBackboneSaAcceptance(
            String name,
            OptimizationAlgorithm startAlgorithm,
            CycleImprover localSearch,
            long timeLimitNanos,
            long seed,
            double initialTemperature,
            double finalTemperature,
            CoolingSchedule coolingSchedule,
            int swapEdgesCount,
            double protectedBreakProbability,
            EliteMode eliteMode,
            double minDistanceThreshold
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
        this.protectedBreakProbability = protectedBreakProbability;
        this.eliteMode = Objects.requireNonNull(eliteMode, "eliteMode");
        this.minDistanceThreshold = minDistanceThreshold;
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

        DiverseEliteArchive elite = new DiverseEliteArchive(eliteMode, minDistanceThreshold);
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

        for (int attempt = 0; attempt < 50; attempt++) {
            int firstPosition = random.nextInt(cycle.size());
            int secondPosition = random.nextInt(cycle.size());
            if (areAdjacentEdges(cycle, firstPosition, secondPosition)) {
                continue;
            }

            boolean breaksProtected = protectedEdges.contains(edgeAt(cycle, firstPosition))
                    || protectedEdges.contains(edgeAt(cycle, secondPosition));
            if (!breaksProtected || random.nextDouble() < protectedBreakProbability) {
                int leftPosition = Math.min(firstPosition, secondPosition);
                int rightPosition = Math.max(firstPosition, secondPosition);
                cycle.reverseFragment(leftPosition + 1, rightPosition);
                return;
            }
        }
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
    public int lastIterationCount() {
        return lastIterationCount;
    }

    @Override
    public int acceptedBetterCount() {
        return acceptedBetterCount;
    }

    @Override
    public int acceptedWorseCount() {
        return acceptedWorseCount;
    }

    @Override
    public int rejectedWorseCount() {
        return rejectedWorseCount;
    }

    @Override
    public int bestFoundIteration() {
        return bestFoundIteration;
    }

    public int eliteUpdateCount() {
        return eliteUpdateCount;
    }

    public int lastProtectedEdgesCount() {
        return lastProtectedEdgesCount;
    }

    public int minProtectedEdgesCount() {
        return minProtectedEdgesCount;
    }

    public int maxProtectedEdgesCount() {
        return maxProtectedEdgesCount;
    }

    public double avgProtectedEdgesOnUpdate() {
        if (protectedEdgesMeasurements == 0) {
            return 0.0;
        }
        return (double) protectedEdgesSum / (double) protectedEdgesMeasurements;
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

    private static double edgeSimilarity(EliteItem a, EliteItem b) {
        int minSize = Math.min(a.edges().size(), b.edges().size());
        if (minSize == 0) {
            return 0.0;
        }
        int common = 0;
        Set<Long> smaller = a.edges().size() <= b.edges().size() ? a.edges() : b.edges();
        Set<Long> larger = a.edges().size() <= b.edges().size() ? b.edges() : a.edges();
        for (Long edge : smaller) {
            if (larger.contains(edge)) {
                common++;
            }
        }
        return (double) common / (double) minSize;
    }

    private static double edgeDistance(EliteItem a, EliteItem b) {
        return 1.0 - edgeSimilarity(a, b);
    }

    private static Set<Long> allCommonEdges(List<EliteItem> selected) {
        if (selected.size() < 3) {
            return Set.of();
        }
        Set<Long> common = new HashSet<>(selected.get(0).edges());
        for (int i = 1; i < selected.size(); i++) {
            common.retainAll(selected.get(i).edges());
        }
        return common;
    }

    private static Set<Long> majority2Of3Edges(List<EliteItem> selected) {
        if (selected.size() < 3) {
            return Set.of();
        }
        Map<Long, Integer> counts = new HashMap<>();
        for (EliteItem item : selected) {
            for (Long edge : item.edges()) {
                counts.merge(edge, 1, Integer::sum);
            }
        }
        Set<Long> protectedEdges = new HashSet<>();
        for (Map.Entry<Long, Integer> entry : counts.entrySet()) {
            if (entry.getValue() >= 2) {
                protectedEdges.add(entry.getKey());
            }
        }
        return protectedEdges;
    }

    private static final class DiverseEliteArchive {
        private static final int DIVERSE_CAPACITY = 3;
        private static final int ARCHIVE_CAPACITY = 5;

        private final EliteMode mode;
        private final double minDistanceThreshold;
        private final List<EliteItem> items;

        private DiverseEliteArchive(EliteMode mode, double minDistanceThreshold) {
            this.mode = mode;
            this.minDistanceThreshold = minDistanceThreshold;
            this.items = new ArrayList<>(mode == EliteMode.DIVERSE_MIN_DIST ? DIVERSE_CAPACITY : ARCHIVE_CAPACITY);
        }

        private boolean offer(Cycle cycle, int objective) {
            Set<Long> edges = edgesOf(cycle);
            String key = edgeSetKey(edges);
            for (EliteItem item : items) {
                if (item.key().equals(key)) {
                    return false;
                }
            }
            EliteItem candidate = new EliteItem(new Cycle(cycle), objective, edges, key);
            if (mode == EliteMode.DIVERSE_MIN_DIST) {
                return offerToDiverseElite(candidate);
            }
            return offerToTopArchive(candidate);
        }

        private boolean offerToDiverseElite(EliteItem candidate) {
            if (items.size() < DIVERSE_CAPACITY) {
                items.add(candidate);
                sortByObjective();
                return true;
            }

            int similarIndex = mostSimilarIndex(candidate);
            if (edgeDistance(candidate, items.get(similarIndex)) < minDistanceThreshold) {
                if (candidate.objective() > items.get(similarIndex).objective()) {
                    items.set(similarIndex, candidate);
                    sortByObjective();
                    return true;
                }
                return false;
            }

            int worstIndex = worstIndex();
            if (candidate.objective() > items.get(worstIndex).objective()) {
                items.set(worstIndex, candidate);
                sortByObjective();
                return true;
            }
            return false;
        }

        private boolean offerToTopArchive(EliteItem candidate) {
            if (items.size() < ARCHIVE_CAPACITY) {
                items.add(candidate);
                sortByObjective();
                return true;
            }
            int worstIndex = worstIndex();
            if (candidate.objective() > items.get(worstIndex).objective()) {
                items.set(worstIndex, candidate);
                sortByObjective();
                return true;
            }
            return false;
        }

        private Set<Long> protectedEdges() {
            if (mode == EliteMode.DIVERSE_MIN_DIST) {
                if (items.size() < DIVERSE_CAPACITY) {
                    return Set.of();
                }
                return allCommonEdges(items);
            }

            if (items.size() < 3) {
                return Set.of();
            }

            List<EliteItem> selected = switch (mode) {
                case ARCHIVE5_DIVERSE_TOP3_ALL_COMMON, ARCHIVE5_DIVERSE_TOP3_MAJORITY2OF3 -> selectArchive5DiverseTop3();
                case BEST_FAR_COMPATIBLE -> selectBestFarCompatible();
                case DIVERSE_MIN_DIST -> throw new IllegalStateException("handled above");
            };

            if (mode == EliteMode.ARCHIVE5_DIVERSE_TOP3_MAJORITY2OF3) {
                return majority2Of3Edges(selected);
            }
            return allCommonEdges(selected);
        }

        private List<EliteItem> selectArchive5DiverseTop3() {
            List<EliteItem> selected = new ArrayList<>(3);
            EliteItem first = items.get(0);
            selected.add(first);

            EliteItem second = null;
            double secondScore = Double.NEGATIVE_INFINITY;
            for (EliteItem item : items) {
                if (item == first) {
                    continue;
                }
                double score = item.objective() + 1000.0 * edgeDistance(item, first);
                if (score > secondScore) {
                    secondScore = score;
                    second = item;
                }
            }
            if (second == null) {
                return selected;
            }
            selected.add(second);

            EliteItem third = null;
            double thirdScore = Double.NEGATIVE_INFINITY;
            for (EliteItem item : items) {
                if (item == first || item == second) {
                    continue;
                }
                double diversity = Math.min(edgeDistance(item, first), edgeDistance(item, second));
                double score = item.objective() + 1000.0 * diversity;
                if (score > thirdScore) {
                    thirdScore = score;
                    third = item;
                }
            }
            if (third != null) {
                selected.add(third);
            }
            return selected;
        }

        private List<EliteItem> selectBestFarCompatible() {
            List<EliteItem> selected = new ArrayList<>(3);
            EliteItem best = items.get(0);
            selected.add(best);

            EliteItem far = null;
            double farScore = Double.NEGATIVE_INFINITY;
            for (EliteItem item : items) {
                if (item == best) {
                    continue;
                }
                double score = 1000.0 * edgeDistance(item, best) + 0.01 * item.objective();
                if (score > farScore) {
                    farScore = score;
                    far = item;
                }
            }
            if (far == null) {
                return selected;
            }
            selected.add(far);

            EliteItem compatible = null;
            double compatibleScore = Double.NEGATIVE_INFINITY;
            for (EliteItem item : items) {
                if (item == best || item == far) {
                    continue;
                }
                double bridge = Math.min(edgeSimilarity(item, best), edgeSimilarity(item, far));
                double score = item.objective() + 1000.0 * bridge;
                if (score > compatibleScore) {
                    compatibleScore = score;
                    compatible = item;
                }
            }
            if (compatible != null) {
                selected.add(compatible);
            }
            return selected;
        }

        private int mostSimilarIndex(EliteItem candidate) {
            int index = 0;
            double similarity = edgeSimilarity(candidate, items.get(0));
            for (int i = 1; i < items.size(); i++) {
                double current = edgeSimilarity(candidate, items.get(i));
                if (current > similarity) {
                    similarity = current;
                    index = i;
                }
            }
            return index;
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

        private void sortByObjective() {
            items.sort(Comparator.comparingInt(EliteItem::objective).reversed());
        }
    }

    private record EliteItem(Cycle cycle, int objective, Set<Long> edges, String key) {
    }
}
