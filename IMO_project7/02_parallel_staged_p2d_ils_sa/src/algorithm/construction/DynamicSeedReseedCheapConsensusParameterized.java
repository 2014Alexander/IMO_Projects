package algorithm.construction;

import algorithm.OptimizationAlgorithm;
import algorithm.SolutionObjective;
import algorithm.improvement.PhaseTwoDelete;
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
 * Dynamic seed-reseed cheap consensus variants.
 *
 * HYBRID_ANCHOR_FAST_RESEED:
 *   initial pool = exact(start, nearest), exact(start, farthest), FAST_R2(start),
 *   then update top3 and add pairs of FAST_R2(seed) from scored vertices.
 *
 * EXACT_DYNAMIC_RESEED:
 *   initial pool = exact(start, nearest), exact(start, farthest),
 *   then update top2 and add pairs of deterministic exact(seed, nearest(seed)).
 */
public final class DynamicSeedReseedCheapConsensusParameterized implements OptimizationAlgorithm {
    private static final int LS_CANDIDATES = 5;
    private static final int ELITE_SIZE = 3;
    private static final int LOCAL_SEARCH_MAX_ITERS = 100;
    private static final int TOP_K = 3;
    private static final double EXPLORATION_PROBABILITY = 0.30;

    private final String name;
    private final Mode mode;
    private final int targetPoolSize;
    private final Random random;
    private final PhaseTwoDelete phaseTwoDelete;

    public enum Mode {
        HYBRID_ANCHOR_FAST_RESEED,
        EXACT_DYNAMIC_RESEED,
        EXACT_DYNAMIC_RESEED_STAGED_CYCLE_P2D
    }

    public DynamicSeedReseedCheapConsensusParameterized(String name, Mode mode, int targetPoolSize, long seed) {
        this.name = Objects.requireNonNull(name, "name");
        this.mode = Objects.requireNonNull(mode, "mode");
        if (targetPoolSize < 2) {
            throw new IllegalArgumentException("targetPoolSize must be >= 2");
        }
        this.targetPoolSize = targetPoolSize;
        this.random = new Random(seed);
        this.phaseTwoDelete = new PhaseTwoDelete();
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Solution solve(Instance instance, int startVertexId) {
        return switch (mode) {
            case HYBRID_ANCHOR_FAST_RESEED -> solveHybridAnchorFastReseed(instance, startVertexId);
            case EXACT_DYNAMIC_RESEED -> solveExactDynamicReseed(instance, startVertexId);
            case EXACT_DYNAMIC_RESEED_STAGED_CYCLE_P2D -> solveExactDynamicReseedStagedCycleP2D(instance, startVertexId);
        };
    }

    private Solution solveHybridAnchorFastReseed(Instance instance, int startVertexId) {
        List<ScoredSolution> pool = new ArrayList<>(targetPoolSize + 2);
        Set<Integer> usedSeedVertices = new HashSet<>();
        usedSeedVertices.add(startVertexId);

        Solution nearest = exactWithSecondAndP2D(instance, startVertexId, nearestVertex(instance, startVertexId, -1));
        pool.add(new ScoredSolution(nearest, SolutionObjective.calculate(instance, nearest), 0));

        Solution farthest = exactWithSecondAndP2D(instance, startVertexId, farthestVertex(instance, startVertexId, -1));
        pool.add(new ScoredSolution(farthest, SolutionObjective.calculate(instance, farthest), 1));

        OptimizationAlgorithm fastR2 = new FastRandomized2RegretParameterizedWithPhaseTwoDelete(
                name + "_initial_fast_r2", TOP_K, EXPLORATION_PROBABILITY, new Random(random.nextLong())
        );
        Solution initialRandomized = fastR2.solve(instance, startVertexId);
        pool.add(new ScoredSolution(initialRandomized, SolutionObjective.calculate(instance, initialRandomized), 2));

        int runIndex = 3;
        while (pool.size() < targetPoolSize) {
            sortPool(pool);
            List<Integer> seeds = bestSeedVertices(instance, topCycles(pool, 3), 2, usedSeedVertices);
            if (seeds.isEmpty()) {
                break;
            }
            for (int seedVertex : seeds) {
                if (pool.size() >= targetPoolSize) {
                    break;
                }
                usedSeedVertices.add(seedVertex);
                OptimizationAlgorithm construction = new FastRandomized2RegretParameterizedWithPhaseTwoDelete(
                        name + "_fast_seed_" + seedVertex, TOP_K, EXPLORATION_PROBABILITY, new Random(random.nextLong())
                );
                Solution solution = construction.solve(instance, seedVertex);
                solution = new Solution(instance.name, startVertexId, solution.cycle());
                pool.add(new ScoredSolution(solution, SolutionObjective.calculate(instance, solution), runIndex++));
            }
        }

        return consensusFromPool(instance, startVertexId, pool);
    }

    private Solution solveExactDynamicReseed(Instance instance, int startVertexId) {
        List<ScoredSolution> pool = new ArrayList<>(targetPoolSize + 2);
        Set<Integer> usedSeedVertices = new HashSet<>();
        usedSeedVertices.add(startVertexId);

        Solution nearest = exactWithSecondAndP2D(instance, startVertexId, nearestVertex(instance, startVertexId, -1));
        pool.add(new ScoredSolution(nearest, SolutionObjective.calculate(instance, nearest), 0));

        Solution farthest = exactWithSecondAndP2D(instance, startVertexId, farthestVertex(instance, startVertexId, -1));
        pool.add(new ScoredSolution(farthest, SolutionObjective.calculate(instance, farthest), 1));

        int runIndex = 2;
        while (pool.size() < targetPoolSize) {
            sortPool(pool);
            List<Integer> seeds = bestSeedVertices(instance, topCycles(pool, 2), 2, usedSeedVertices);
            if (seeds.isEmpty()) {
                break;
            }
            for (int seedVertex : seeds) {
                if (pool.size() >= targetPoolSize) {
                    break;
                }
                usedSeedVertices.add(seedVertex);
                int second = nearestVertex(instance, seedVertex, -1);
                Solution solution = exactWithSecondAndP2D(instance, seedVertex, second);
                solution = new Solution(instance.name, startVertexId, solution.cycle());
                pool.add(new ScoredSolution(solution, SolutionObjective.calculate(instance, solution), runIndex++));
            }
        }

        return consensusFromPool(instance, startVertexId, pool);
    }

    private Solution solveExactDynamicReseedStagedCycleP2D(Instance instance, int startVertexId) {
        List<ScoredSolution> pool = new ArrayList<>(targetPoolSize + 2);
        Set<Integer> usedSeedVertices = new HashSet<>();
        usedSeedVertices.add(startVertexId);

        Solution nearest = stagedExactWithSecondAndP2D(instance, startVertexId, nearestVertex(instance, startVertexId, -1));
        pool.add(new ScoredSolution(nearest, SolutionObjective.calculate(instance, nearest), 0));

        Solution farthest = stagedExactWithSecondAndP2D(instance, startVertexId, farthestVertex(instance, startVertexId, -1));
        pool.add(new ScoredSolution(farthest, SolutionObjective.calculate(instance, farthest), 1));

        int runIndex = 2;
        while (pool.size() < targetPoolSize) {
            sortPool(pool);
            List<Integer> seeds = bestSeedVertices(instance, topCycles(pool, 2), 2, usedSeedVertices);
            if (seeds.isEmpty()) {
                break;
            }
            for (int seedVertex : seeds) {
                if (pool.size() >= targetPoolSize) {
                    break;
                }
                usedSeedVertices.add(seedVertex);
                int second = nearestVertex(instance, seedVertex, -1);
                Solution solution = stagedExactWithSecondAndP2D(instance, seedVertex, second);
                solution = new Solution(instance.name, startVertexId, solution.cycle());
                pool.add(new ScoredSolution(solution, SolutionObjective.calculate(instance, solution), runIndex++));
            }
        }

        return consensusFromPool(instance, startVertexId, pool);
    }

    private Solution consensusFromPool(Instance instance, int startVertexId, List<ScoredSolution> pool) {
        sortPool(pool);

        List<ScoredSolution> improved = new ArrayList<>(LS_CANDIDATES);
        for (int i = 0; i < Math.min(LS_CANDIDATES, pool.size()); i++) {
            ScoredSolution item = pool.get(i);
            Solution lsSolution = localSearchBasic(instance, item.solution(), LOCAL_SEARCH_MAX_ITERS);
            improved.add(new ScoredSolution(lsSolution, SolutionObjective.calculate(instance, lsSolution), item.runIndex()));
        }
        sortPool(improved);

        List<List<Integer>> eliteCycles = new ArrayList<>(ELITE_SIZE);
        for (int i = 0; i < Math.min(ELITE_SIZE, improved.size()); i++) {
            eliteCycles.add(improved.get(i).solution().cycle());
        }

        if (eliteCycles.isEmpty()) {
            return exactWithSecondAndP2D(instance, startVertexId, nearestVertex(instance, startVertexId, -1));
        }

        List<Integer> reference = eliteCycles.get(0);
        Set<Long> commonEdges = commonEdgesAmong(eliteCycles);
        List<List<Integer>> fragments = orderedFragmentsFromReference(reference, commonEdges);

        List<Integer> repairedCycle = completeOrderedFragments(instance, startVertexId, fragments);
        Solution repaired = new Solution(instance.name, startVertexId, repairedCycle);
        Solution cleaned = phaseTwoDelete.improve(instance, repaired);
        Solution finalSolution = localSearchBasic(instance, cleaned, LOCAL_SEARCH_MAX_ITERS);

        Solution bestParent = improved.get(0).solution();
        if (SolutionObjective.calculate(instance, bestParent) > SolutionObjective.calculate(instance, finalSolution)) {
            return bestParent;
        }
        return finalSolution;
    }

    private Solution exactWithSecondAndP2D(Instance instance, int firstVertex, int secondVertex) {
        Solution constructed = exactTwoRegretWithSecond(instance, firstVertex, secondVertex);
        return phaseTwoDelete.improve(instance, constructed);
    }

    private Solution stagedExactWithSecondAndP2D(Instance instance, int firstVertex, int secondVertex) {
        Cycle cycle = createInitialCycle(instance, firstVertex, secondVertex);
        boolean[] inCycle = new boolean[instance.size];
        rebuildInCycle(inCycle, cycle);
        int[] stages = new int[] {15, 35, 55, 75, 100};
        for (int percentage : stages) {
            int targetSize = (instance.size * percentage + 99) / 100;
            insertUntilFast(instance, cycle, inCycle, targetSize);
            phaseTwoDelete.improve(instance, cycle);
            rebuildInCycle(inCycle, cycle);
        }
        return new Solution(instance.name, firstVertex, cycle.toList());
    }

    private static Cycle createInitialCycle(Instance instance, int firstVertex, int secondVertex) {
        Cycle cycle = new Cycle(instance.size);
        cycle.append(firstVertex);
        int actualSecond = secondVertex;
        if (actualSecond < 0 || actualSecond == firstVertex) {
            actualSecond = nearestVertex(instance, firstVertex, -1);
        }
        if (actualSecond >= 0 && actualSecond != firstVertex) {
            cycle.append(actualSecond);
        }
        return cycle;
    }

    private static void rebuildInCycle(boolean[] inCycle, Cycle cycle) {
        java.util.Arrays.fill(inCycle, false);
        for (int i = 0; i < cycle.size(); i++) {
            inCycle[cycle.cycle[i]] = true;
        }
    }

    private static void insertUntilFast(Instance instance, Cycle cycle, boolean[] inCycle, int targetSize) {
        if (cycle.size() >= targetSize) {
            return;
        }
        int n = instance.size;
        int[][] distances = instance.distanceMatrix.distances;
        int[] profits = profits(instance);

        int[] bestCost = new int[n];
        int[] secondBestCost = new int[n];
        int[] bestEdgeIndex = new int[n];
        int[] secondBestEdgeIndex = new int[n];
        int[] bestFirst = new int[n];
        int[] bestSecond = new int[n];
        int[] secondBestFirst = new int[n];
        int[] secondBestSecond = new int[n];

        for (int vertex = 0; vertex < n; vertex++) {
            if (!inCycle[vertex]) {
                recomputeTop2(cycle, vertex, distances, profits, bestCost, secondBestCost, bestEdgeIndex,
                        secondBestEdgeIndex, bestFirst, bestSecond, secondBestFirst, secondBestSecond);
            }
        }

        int[] candidateEdgeIndex = new int[4];
        int[] candidateFirst = new int[4];
        int[] candidateSecond = new int[4];
        int[] candidateCost = new int[4];

        while (cycle.size() < targetSize) {
            int bestVertex = -1;
            int bestRegret = Integer.MIN_VALUE;
            int selectedBestCost = Integer.MAX_VALUE;

            for (int vertex = 0; vertex < n; vertex++) {
                if (inCycle[vertex]) {
                    continue;
                }
                int regret = secondBestCost[vertex] - bestCost[vertex];
                if (regret > bestRegret
                        || (regret == bestRegret && bestCost[vertex] < selectedBestCost)
                        || (regret == bestRegret && bestCost[vertex] == selectedBestCost && vertex < bestVertex)) {
                    bestRegret = regret;
                    selectedBestCost = bestCost[vertex];
                    bestVertex = vertex;
                }
            }

            if (bestVertex == -1) {
                return;
            }

            int removedEdgeIndex = bestEdgeIndex[bestVertex];
            int removedFirst = bestFirst[bestVertex];
            int removedSecond = bestSecond[bestVertex];

            cycle.insertAfter(removedEdgeIndex, bestVertex);
            inCycle[bestVertex] = true;

            for (int vertex = 0; vertex < n; vertex++) {
                if (inCycle[vertex]) {
                    continue;
                }

                boolean top2UsesRemovedEdge =
                        (bestFirst[vertex] == removedFirst && bestSecond[vertex] == removedSecond)
                                || (secondBestFirst[vertex] == removedFirst && secondBestSecond[vertex] == removedSecond);

                if (top2UsesRemovedEdge) {
                    recomputeTop2(cycle, vertex, distances, profits, bestCost, secondBestCost, bestEdgeIndex,
                            secondBestEdgeIndex, bestFirst, bestSecond, secondBestFirst, secondBestSecond);
                } else {
                    int mappedBestEdgeIndex = bestEdgeIndex[vertex] < removedEdgeIndex ? bestEdgeIndex[vertex] : bestEdgeIndex[vertex] + 1;
                    int mappedSecondBestEdgeIndex = secondBestEdgeIndex[vertex] < removedEdgeIndex ? secondBestEdgeIndex[vertex] : secondBestEdgeIndex[vertex] + 1;

                    int firstNewCost = insertionCost(distances, profits, removedFirst, vertex, bestVertex);
                    int secondNewCost = insertionCost(distances, profits, bestVertex, vertex, removedSecond);

                    candidateEdgeIndex[0] = mappedBestEdgeIndex;
                    candidateFirst[0] = bestFirst[vertex];
                    candidateSecond[0] = bestSecond[vertex];
                    candidateCost[0] = bestCost[vertex];

                    candidateEdgeIndex[1] = mappedSecondBestEdgeIndex;
                    candidateFirst[1] = secondBestFirst[vertex];
                    candidateSecond[1] = secondBestSecond[vertex];
                    candidateCost[1] = secondBestCost[vertex];

                    candidateEdgeIndex[2] = removedEdgeIndex;
                    candidateFirst[2] = removedFirst;
                    candidateSecond[2] = bestVertex;
                    candidateCost[2] = firstNewCost;

                    candidateEdgeIndex[3] = removedEdgeIndex + 1;
                    candidateFirst[3] = bestVertex;
                    candidateSecond[3] = removedSecond;
                    candidateCost[3] = secondNewCost;

                    sortFourCandidatesByEdgeIndex(candidateEdgeIndex, candidateFirst, candidateSecond, candidateCost);
                    selectTop2FromCandidates(vertex, candidateEdgeIndex, candidateFirst, candidateSecond, candidateCost,
                            bestCost, secondBestCost, bestEdgeIndex, secondBestEdgeIndex, bestFirst, bestSecond,
                            secondBestFirst, secondBestSecond);
                }
            }
        }
    }

    private static void recomputeTop2(Cycle cycle, int vertexId, int[][] distances, int[] profits,
                                      int[] bestCost, int[] secondBestCost, int[] bestEdgeIndex, int[] secondBestEdgeIndex,
                                      int[] bestFirst, int[] bestSecond, int[] secondBestFirst, int[] secondBestSecond) {
        int currentBestCost = Integer.MAX_VALUE;
        int currentSecondBestCost = Integer.MAX_VALUE;
        int currentBestEdgeIndex = -1;
        int currentSecondBestEdgeIndex = -1;
        int currentBestFirst = -1;
        int currentBestSecond = -1;
        int currentSecondBestFirst = -1;
        int currentSecondBestSecond = -1;
        int cycleSize = cycle.size();
        int[] cycleVertices = cycle.cycle;
        for (int edgeIndex = 0; edgeIndex < cycleSize; edgeIndex++) {
            int first = cycleVertices[edgeIndex];
            int second = cycleVertices[edgeIndex + 1 == cycleSize ? 0 : edgeIndex + 1];
            int cost = insertionCost(distances, profits, first, vertexId, second);
            if (cost < currentBestCost) {
                currentSecondBestCost = currentBestCost;
                currentSecondBestEdgeIndex = currentBestEdgeIndex;
                currentSecondBestFirst = currentBestFirst;
                currentSecondBestSecond = currentBestSecond;
                currentBestCost = cost;
                currentBestEdgeIndex = edgeIndex;
                currentBestFirst = first;
                currentBestSecond = second;
            } else if (cost < currentSecondBestCost) {
                currentSecondBestCost = cost;
                currentSecondBestEdgeIndex = edgeIndex;
                currentSecondBestFirst = first;
                currentSecondBestSecond = second;
            }
        }
        bestCost[vertexId] = currentBestCost;
        secondBestCost[vertexId] = currentSecondBestCost;
        bestEdgeIndex[vertexId] = currentBestEdgeIndex;
        secondBestEdgeIndex[vertexId] = currentSecondBestEdgeIndex;
        bestFirst[vertexId] = currentBestFirst;
        bestSecond[vertexId] = currentBestSecond;
        secondBestFirst[vertexId] = currentSecondBestFirst;
        secondBestSecond[vertexId] = currentSecondBestSecond;
    }

    private static void selectTop2FromCandidates(int vertexId, int[] candidateEdgeIndex, int[] candidateFirst,
                                                 int[] candidateSecond, int[] candidateCost,
                                                 int[] bestCost, int[] secondBestCost, int[] bestEdgeIndex,
                                                 int[] secondBestEdgeIndex, int[] bestFirst, int[] bestSecond,
                                                 int[] secondBestFirst, int[] secondBestSecond) {
        int currentBestCost = Integer.MAX_VALUE;
        int currentSecondBestCost = Integer.MAX_VALUE;
        int currentBestEdgeIndex = -1;
        int currentSecondBestEdgeIndex = -1;
        int currentBestFirst = -1;
        int currentBestSecond = -1;
        int currentSecondBestFirst = -1;
        int currentSecondBestSecond = -1;
        for (int i = 0; i < 4; i++) {
            int cost = candidateCost[i];
            if (cost < currentBestCost) {
                currentSecondBestCost = currentBestCost;
                currentSecondBestEdgeIndex = currentBestEdgeIndex;
                currentSecondBestFirst = currentBestFirst;
                currentSecondBestSecond = currentBestSecond;
                currentBestCost = cost;
                currentBestEdgeIndex = candidateEdgeIndex[i];
                currentBestFirst = candidateFirst[i];
                currentBestSecond = candidateSecond[i];
            } else if (cost < currentSecondBestCost) {
                currentSecondBestCost = cost;
                currentSecondBestEdgeIndex = candidateEdgeIndex[i];
                currentSecondBestFirst = candidateFirst[i];
                currentSecondBestSecond = candidateSecond[i];
            }
        }
        bestCost[vertexId] = currentBestCost;
        secondBestCost[vertexId] = currentSecondBestCost;
        bestEdgeIndex[vertexId] = currentBestEdgeIndex;
        secondBestEdgeIndex[vertexId] = currentSecondBestEdgeIndex;
        bestFirst[vertexId] = currentBestFirst;
        bestSecond[vertexId] = currentBestSecond;
        secondBestFirst[vertexId] = currentSecondBestFirst;
        secondBestSecond[vertexId] = currentSecondBestSecond;
    }

    private static void sortFourCandidatesByEdgeIndex(int[] candidateEdgeIndex, int[] candidateFirst, int[] candidateSecond, int[] candidateCost) {
        for (int i = 1; i < 4; i++) {
            int edgeIndexValue = candidateEdgeIndex[i];
            int firstValue = candidateFirst[i];
            int secondValue = candidateSecond[i];
            int costValue = candidateCost[i];
            int j = i - 1;
            while (j >= 0 && candidateEdgeIndex[j] > edgeIndexValue) {
                candidateEdgeIndex[j + 1] = candidateEdgeIndex[j];
                candidateFirst[j + 1] = candidateFirst[j];
                candidateSecond[j + 1] = candidateSecond[j];
                candidateCost[j + 1] = candidateCost[j];
                j--;
            }
            candidateEdgeIndex[j + 1] = edgeIndexValue;
            candidateFirst[j + 1] = firstValue;
            candidateSecond[j + 1] = secondValue;
            candidateCost[j + 1] = costValue;
        }
    }

    private static Solution exactTwoRegretWithSecond(Instance instance, int firstVertex, int secondVertex) {
        int[][] distances = instance.distanceMatrix.distances;
        int[] profits = profits(instance);
        int vertexCount = instance.size;

        List<Integer> cycle = new ArrayList<>(vertexCount);
        List<Integer> notUsed = new ArrayList<>(vertexCount - 2);

        cycle.add(firstVertex);
        if (secondVertex >= 0 && secondVertex != firstVertex) {
            cycle.add(secondVertex);
        }
        for (int vertexId = 0; vertexId < vertexCount; vertexId++) {
            if (vertexId != firstVertex && vertexId != secondVertex) {
                notUsed.add(vertexId);
            }
        }
        if (cycle.size() < 2 && !notUsed.isEmpty()) {
            int nearest = nearestVertex(instance, firstVertex, -1);
            cycle.add(nearest);
            notUsed.remove(Integer.valueOf(nearest));
        }

        Top2[] stats = new Top2[vertexCount];
        for (int vertexId : notUsed) {
            stats[vertexId] = recomputeTop2(cycle, vertexId, distances, profits);
        }

        while (!notUsed.isEmpty()) {
            int bestVertex = -1;
            int bestVertexIndexInNotUsed = -1;
            int bestRegret = Integer.MIN_VALUE;
            int bestCost = Integer.MAX_VALUE;

            for (int notUsedIndex = 0; notUsedIndex < notUsed.size(); notUsedIndex++) {
                int vertexId = notUsed.get(notUsedIndex);
                Top2 top2 = stats[vertexId];
                int regret = top2.secondBestCost - top2.bestCost;
                if (regret > bestRegret || (regret == bestRegret && top2.bestCost < bestCost) || (regret == bestRegret && top2.bestCost == bestCost && vertexId < bestVertex)) {
                    bestRegret = regret;
                    bestCost = top2.bestCost;
                    bestVertex = vertexId;
                    bestVertexIndexInNotUsed = notUsedIndex;
                }
            }

            Top2 selectedTop2 = stats[bestVertex];
            int removedFirst = selectedTop2.bestFirst;
            int removedSecond = selectedTop2.bestSecond;
            int removedEdgeIndex = selectedTop2.bestEdgeIndex;
            int insertPosition = removedEdgeIndex + 1;

            cycle.add(insertPosition, bestVertex);
            notUsed.remove(bestVertexIndexInNotUsed);
            stats[bestVertex] = null;

            for (int vertexId : notUsed) {
                Top2 current = stats[vertexId];
                if (current.usesEdge(removedFirst, removedSecond)) {
                    stats[vertexId] = recomputeTop2(cycle, vertexId, distances, profits);
                } else {
                    Top2 mapped = current.afterInsertionAt(removedEdgeIndex);
                    int costFirstNew = insertionCost(distances, profits, removedFirst, vertexId, bestVertex);
                    int costSecondNew = insertionCost(distances, profits, bestVertex, vertexId, removedSecond);
                    stats[vertexId] = updateWithNewEdges(mapped, removedEdgeIndex, removedFirst, bestVertex, costFirstNew, bestVertex, removedSecond, costSecondNew);
                }
            }
        }

        return new Solution(instance.name, firstVertex, cycle);
    }

    private static Top2 recomputeTop2(List<Integer> cycle, int vertexId, int[][] distances, int[] profits) {
        Top2Builder builder = new Top2Builder();
        for (int edgeIndex = 0; edgeIndex < cycle.size(); edgeIndex++) {
            int first = cycle.get(edgeIndex);
            int second = cycle.get((edgeIndex + 1) % cycle.size());
            int cost = insertionCost(distances, profits, first, vertexId, second);
            builder.accept(new EdgeCandidate(edgeIndex, first, second, cost));
        }
        return builder.toTop2();
    }

    private static Top2 updateWithNewEdges(Top2 mapped, int removedEdgeIndex, int firstNewA, int firstNewB, int firstNewCost, int secondNewA, int secondNewB, int secondNewCost) {
        EdgeCandidate[] candidates = new EdgeCandidate[] {
                new EdgeCandidate(mapped.bestEdgeIndex, mapped.bestFirst, mapped.bestSecond, mapped.bestCost),
                new EdgeCandidate(mapped.secondBestEdgeIndex, mapped.secondBestFirst, mapped.secondBestSecond, mapped.secondBestCost),
                new EdgeCandidate(removedEdgeIndex, firstNewA, firstNewB, firstNewCost),
                new EdgeCandidate(removedEdgeIndex + 1, secondNewA, secondNewB, secondNewCost)
        };
        sortByEdgeIndex(candidates);
        Top2Builder builder = new Top2Builder();
        for (EdgeCandidate candidate : candidates) {
            builder.accept(candidate);
        }
        return builder.toTop2();
    }

    private static void sortByEdgeIndex(EdgeCandidate[] candidates) {
        for (int i = 1; i < candidates.length; i++) {
            EdgeCandidate value = candidates[i];
            int j = i - 1;
            while (j >= 0 && candidates[j].edgeIndex > value.edgeIndex) {
                candidates[j + 1] = candidates[j];
                j--;
            }
            candidates[j + 1] = value;
        }
    }

    private static int insertionCost(int[][] distances, int[] profits, int firstVertex, int insertedVertex, int secondVertex) {
        return distances[firstVertex][insertedVertex] + distances[insertedVertex][secondVertex] - distances[firstVertex][secondVertex] - profits[insertedVertex];
    }

    private static List<Integer> bestSeedVertices(Instance instance, List<List<Integer>> cycles, int count, Set<Integer> forbiddenSeeds) {
        if (cycles.isEmpty() || count <= 0) {
            return List.of();
        }
        int[][] distances = instance.distanceMatrix.distances;
        int[] profits = profits(instance);
        Map<Integer, VertexScore> scores = new HashMap<>();
        for (List<Integer> cycle : cycles) {
            int size = cycle.size();
            for (int position = 0; position < size; position++) {
                int vertex = cycle.get(position);
                if (forbiddenSeeds.contains(vertex)) {
                    continue;
                }
                int prev = cycle.get(position == 0 ? size - 1 : position - 1);
                int next = cycle.get(position + 1 == size ? 0 : position + 1);
                int localContribution = profits[vertex] - (distances[prev][vertex] + distances[vertex][next] - distances[prev][next]);
                scores.computeIfAbsent(vertex, ignored -> new VertexScore(vertex)).accept(localContribution);
            }
        }
        return scores.values().stream()
                .sorted(Comparator
                        .comparingLong(VertexScore::score).reversed()
                        .thenComparingInt(VertexScore::vertex))
                .limit(count)
                .map(VertexScore::vertex)
                .toList();
    }

    private static List<List<Integer>> topCycles(List<ScoredSolution> scored, int count) {
        List<List<Integer>> top = new ArrayList<>(Math.min(count, scored.size()));
        for (int i = 0; i < Math.min(count, scored.size()); i++) {
            top.add(scored.get(i).solution().cycle());
        }
        return top;
    }

    private static void sortPool(List<ScoredSolution> pool) {
        pool.sort(Comparator
                .comparingInt(ScoredSolution::objective).reversed()
                .thenComparingInt(ScoredSolution::runIndex));
    }

    private static int nearestVertex(Instance instance, int vertexId, int forbidden) {
        int bestVertex = -1;
        int bestDistance = Integer.MAX_VALUE;
        for (int v = 0; v < instance.size; v++) {
            if (v == vertexId || v == forbidden) {
                continue;
            }
            int distance = instance.distanceMatrix.distances[vertexId][v];
            if (distance < bestDistance || (distance == bestDistance && v < bestVertex)) {
                bestDistance = distance;
                bestVertex = v;
            }
        }
        return bestVertex;
    }

    private static int farthestVertex(Instance instance, int vertexId, int forbidden) {
        int bestVertex = -1;
        int bestDistance = Integer.MIN_VALUE;
        for (int v = 0; v < instance.size; v++) {
            if (v == vertexId || v == forbidden) {
                continue;
            }
            int distance = instance.distanceMatrix.distances[vertexId][v];
            if (distance > bestDistance || (distance == bestDistance && v < bestVertex)) {
                bestDistance = distance;
                bestVertex = v;
            }
        }
        return bestVertex;
    }

    private static Set<Long> commonEdgesAmong(List<List<Integer>> cycles) {
        Set<Long> common = null;
        for (List<Integer> cycle : cycles) {
            Set<Long> edges = edgesOf(cycle);
            if (common == null) {
                common = edges;
            } else {
                common.retainAll(edges);
            }
        }
        return common == null ? Set.of() : common;
    }

    private static Set<Long> edgesOf(List<Integer> cycle) {
        Set<Long> edges = new HashSet<>();
        int size = cycle.size();
        for (int i = 0; i < size; i++) {
            int a = cycle.get(i);
            int b = cycle.get((i + 1) % size);
            edges.add(edgeKey(a, b));
        }
        return edges;
    }

    private static List<List<Integer>> orderedFragmentsFromReference(List<Integer> reference, Set<Long> commonEdges) {
        int n = reference.size();
        List<List<Integer>> fragments = new ArrayList<>();
        if (n < 2 || commonEdges.isEmpty()) {
            return fragments;
        }
        boolean[] common = new boolean[n];
        boolean allCommon = true;
        for (int i = 0; i < n; i++) {
            int a = reference.get(i);
            int b = reference.get((i + 1) % n);
            common[i] = commonEdges.contains(edgeKey(a, b));
            allCommon &= common[i];
        }
        if (allCommon) {
            fragments.add(new ArrayList<>(reference));
            return fragments;
        }
        int start = 0;
        for (int i = 0; i < n; i++) {
            if (!common[i]) {
                start = (i + 1) % n;
                break;
            }
        }
        int steps = 0;
        while (steps < n) {
            int index = (start + steps) % n;
            if (!common[index]) {
                steps++;
                continue;
            }
            List<Integer> fragment = new ArrayList<>();
            fragment.add(reference.get(index));
            while (steps < n && common[(start + steps) % n]) {
                int edgeIndex = (start + steps) % n;
                fragment.add(reference.get((edgeIndex + 1) % n));
                steps++;
            }
            if (fragment.size() >= 2) {
                fragments.add(fragment);
            }
        }
        return fragments;
    }

    private static List<Integer> completeOrderedFragments(Instance instance, int startVertexId, List<List<Integer>> fragments) {
        if (fragments.isEmpty()) {
            return exactTwoRegretWithSecond(instance, startVertexId, nearestVertex(instance, startVertexId, -1)).cycle();
        }
        List<Integer> cycle = concatFragmentsReferenceOrder(fragments);
        boolean[] used = new boolean[instance.size];
        for (int vertex : cycle) {
            used[vertex] = true;
        }
        if (cycle.size() < 2) {
            cycle.clear();
            cycle.add(startVertexId);
            used = new boolean[instance.size];
            used[startVertexId] = true;
            int nearest = nearestVertex(instance, startVertexId, -1);
            cycle.add(nearest);
            used[nearest] = true;
        }
        Set<Long> protectedEdges = protectedInternalEdges(fragments);
        List<Integer> unused = new ArrayList<>();
        for (int vertex = 0; vertex < instance.size; vertex++) {
            if (!used[vertex]) {
                unused.add(vertex);
            }
        }
        int[][] distances = instance.distanceMatrix.distances;
        int[] profits = profits(instance);
        while (!unused.isEmpty()) {
            Insertion selected = bestProtectedTwoRegretInsertion(distances, profits, cycle, unused, protectedEdges);
            int vertex = unused.remove(selected.unusedIndex());
            cycle.add(selected.insertAfterPosition() + 1, vertex);
        }
        return cycle;
    }

    private static List<Integer> concatFragmentsReferenceOrder(List<List<Integer>> fragments) {
        List<Integer> cycle = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        for (List<Integer> fragment : fragments) {
            for (int vertex : fragment) {
                if (seen.add(vertex)) {
                    cycle.add(vertex);
                }
            }
        }
        return cycle;
    }

    private static Set<Long> protectedInternalEdges(List<List<Integer>> fragments) {
        Set<Long> protectedEdges = new HashSet<>();
        for (List<Integer> fragment : fragments) {
            for (int i = 0; i + 1 < fragment.size(); i++) {
                protectedEdges.add(edgeKey(fragment.get(i), fragment.get(i + 1)));
            }
        }
        return protectedEdges;
    }

    private static Insertion bestProtectedTwoRegretInsertion(int[][] distances, int[] profits, List<Integer> cycle, List<Integer> unused, Set<Long> protectedEdges) {
        boolean hasUnprotectedEdge = hasUnprotectedEdge(cycle, protectedEdges);
        int selectedUnusedIndex = -1;
        int selectedInsertAfter = -1;
        int selectedRegret = Integer.MIN_VALUE;
        int selectedBestCost = Integer.MAX_VALUE;
        int selectedVertex = Integer.MAX_VALUE;
        for (int unusedIndex = 0; unusedIndex < unused.size(); unusedIndex++) {
            int vertex = unused.get(unusedIndex);
            int bestCost = Integer.MAX_VALUE;
            int secondBestCost = Integer.MAX_VALUE;
            int bestInsertAfter = -1;
            for (int position = 0; position < cycle.size(); position++) {
                int a = cycle.get(position);
                int b = cycle.get((position + 1) % cycle.size());
                if (hasUnprotectedEdge && protectedEdges.contains(edgeKey(a, b))) {
                    continue;
                }
                int cost = distances[a][vertex] + distances[vertex][b] - distances[a][b] - profits[vertex];
                if (cost < bestCost) {
                    secondBestCost = bestCost;
                    bestCost = cost;
                    bestInsertAfter = position;
                } else if (cost < secondBestCost) {
                    secondBestCost = cost;
                }
            }
            int regret = secondBestCost - bestCost;
            if (regret > selectedRegret || (regret == selectedRegret && bestCost < selectedBestCost) || (regret == selectedRegret && bestCost == selectedBestCost && vertex < selectedVertex)) {
                selectedUnusedIndex = unusedIndex;
                selectedInsertAfter = bestInsertAfter;
                selectedRegret = regret;
                selectedBestCost = bestCost;
                selectedVertex = vertex;
            }
        }
        return new Insertion(selectedUnusedIndex, selectedInsertAfter);
    }

    private static boolean hasUnprotectedEdge(List<Integer> cycle, Set<Long> protectedEdges) {
        for (int i = 0; i < cycle.size(); i++) {
            int a = cycle.get(i);
            int b = cycle.get((i + 1) % cycle.size());
            if (!protectedEdges.contains(edgeKey(a, b))) {
                return true;
            }
        }
        return false;
    }

    private static Solution localSearchBasic(Instance instance, Solution solution, int maxIterations) {
        List<Integer> cycle = new ArrayList<>(solution.cycle());
        localSearchBasic(instance, cycle, maxIterations);
        return new Solution(solution.instanceName(), solution.startVertexId(), cycle);
    }

    private static void localSearchBasic(Instance instance, List<Integer> cycle, int maxIterations) {
        int[][] distances = instance.distanceMatrix.distances;
        int[] profits = profits(instance);
        boolean[] selected = new boolean[instance.size];
        for (int vertex : cycle) {
            selected[vertex] = true;
        }
        for (int iteration = 0; iteration < maxIterations; iteration++) {
            Move bestMove = findBestLocalSearchMove(distances, profits, cycle, selected);
            if (bestMove == null || bestMove.gain() <= 0) {
                return;
            }
            applyMove(cycle, selected, bestMove);
        }
    }

    private static Move findBestLocalSearchMove(int[][] distances, int[] profits, List<Integer> cycle, boolean[] selected) {
        int size = cycle.size();
        Move bestMove = null;
        int bestGain = 0;
        if (size > 2) {
            for (int position = 0; position < size; position++) {
                int vertex = cycle.get(position);
                int previous = cycle.get(position == 0 ? size - 1 : position - 1);
                int next = cycle.get(position + 1 == size ? 0 : position + 1);
                int gain = distances[previous][vertex] + distances[vertex][next] - distances[previous][next] - profits[vertex];
                if (gain > bestGain) {
                    bestGain = gain;
                    bestMove = Move.delete(gain, position);
                }
            }
        }
        for (int vertex = 0; vertex < selected.length; vertex++) {
            if (selected[vertex]) {
                continue;
            }
            for (int edgePosition = 0; edgePosition < size; edgePosition++) {
                int a = cycle.get(edgePosition);
                int b = cycle.get((edgePosition + 1) % size);
                int gain = profits[vertex] - (distances[a][vertex] + distances[vertex][b] - distances[a][b]);
                if (gain > bestGain) {
                    bestGain = gain;
                    bestMove = Move.insert(gain, vertex, edgePosition);
                }
            }
            for (int position = 0; position < size; position++) {
                int oldVertex = cycle.get(position);
                int previous = cycle.get(position == 0 ? size - 1 : position - 1);
                int next = cycle.get(position + 1 == size ? 0 : position + 1);
                int gain = profits[vertex] - profits[oldVertex]
                        - (distances[previous][vertex] + distances[vertex][next] - distances[previous][oldVertex] - distances[oldVertex][next]);
                if (gain > bestGain) {
                    bestGain = gain;
                    bestMove = Move.replace(gain, vertex, position);
                }
            }
        }
        if (size >= 4) {
            for (int i = 0; i < size; i++) {
                int iNext = (i + 1) % size;
                for (int j = i + 2; j < size; j++) {
                    if (i == 0 && j == size - 1) {
                        continue;
                    }
                    int jNext = (j + 1) % size;
                    int a = cycle.get(i);
                    int b = cycle.get(iNext);
                    int c = cycle.get(j);
                    int d = cycle.get(jNext);
                    int gain = distances[a][b] + distances[c][d] - distances[a][c] - distances[b][d];
                    if (gain > bestGain) {
                        bestGain = gain;
                        bestMove = Move.twoOpt(gain, i, j);
                    }
                }
            }
        }
        return bestMove;
    }

    private static void applyMove(List<Integer> cycle, boolean[] selected, Move move) {
        switch (move.type()) {
            case DELETE -> {
                int vertex = cycle.remove(move.position());
                selected[vertex] = false;
            }
            case INSERT -> {
                cycle.add(move.position() + 1, move.vertex());
                selected[move.vertex()] = true;
            }
            case REPLACE -> {
                int oldVertex = cycle.set(move.position(), move.vertex());
                selected[oldVertex] = false;
                selected[move.vertex()] = true;
            }
            case TWO_OPT -> reverse(cycle, move.left() + 1, move.right());
        }
    }

    private static void reverse(List<Integer> cycle, int left, int right) {
        while (left < right) {
            int tmp = cycle.get(left);
            cycle.set(left, cycle.get(right));
            cycle.set(right, tmp);
            left++;
            right--;
        }
    }

    private static int[] profits(Instance instance) {
        int[] profits = new int[instance.size];
        for (int vertex = 0; vertex < instance.size; vertex++) {
            profits[vertex] = instance.vertices[vertex].profit;
        }
        return profits;
    }

    private static long edgeKey(int a, int b) {
        int u = Math.min(a, b);
        int v = Math.max(a, b);
        return ((long) u << 32) | (v & 0xffffffffL);
    }

    private record ScoredSolution(Solution solution, int objective, int runIndex) {}
    private record Insertion(int unusedIndex, int insertAfterPosition) {}
    private record EdgeCandidate(int edgeIndex, int first, int second, int cost) {}

    private static final class VertexScore {
        private final int vertex;
        private int frequency;
        private int bestLocalContribution = Integer.MIN_VALUE;

        VertexScore(int vertex) {
            this.vertex = vertex;
        }

        void accept(int localContribution) {
            frequency++;
            bestLocalContribution = Math.max(bestLocalContribution, localContribution);
        }

        int vertex() {
            return vertex;
        }

        long score() {
            return frequency * 1_000_000L + bestLocalContribution;
        }
    }

    private static final class Top2Builder {
        private EdgeCandidate best;
        private EdgeCandidate second;
        void accept(EdgeCandidate candidate) {
            if (best == null || candidate.cost < best.cost) {
                second = best;
                best = candidate;
            } else if (second == null || candidate.cost < second.cost) {
                second = candidate;
            }
        }
        Top2 toTop2() {
            return new Top2(best.edgeIndex, best.first, best.second, best.cost, second.edgeIndex, second.first, second.second, second.cost);
        }
    }

    private static final class Top2 {
        final int bestEdgeIndex;
        final int bestFirst;
        final int bestSecond;
        final int bestCost;
        final int secondBestEdgeIndex;
        final int secondBestFirst;
        final int secondBestSecond;
        final int secondBestCost;
        Top2(int bestEdgeIndex, int bestFirst, int bestSecond, int bestCost, int secondBestEdgeIndex, int secondBestFirst, int secondBestSecond, int secondBestCost) {
            this.bestEdgeIndex = bestEdgeIndex;
            this.bestFirst = bestFirst;
            this.bestSecond = bestSecond;
            this.bestCost = bestCost;
            this.secondBestEdgeIndex = secondBestEdgeIndex;
            this.secondBestFirst = secondBestFirst;
            this.secondBestSecond = secondBestSecond;
            this.secondBestCost = secondBestCost;
        }
        boolean usesEdge(int first, int second) {
            return (bestFirst == first && bestSecond == second) || (secondBestFirst == first && secondBestSecond == second);
        }
        Top2 afterInsertionAt(int removedEdgeIndex) {
            return new Top2(mapIndex(bestEdgeIndex, removedEdgeIndex), bestFirst, bestSecond, bestCost,
                    mapIndex(secondBestEdgeIndex, removedEdgeIndex), secondBestFirst, secondBestSecond, secondBestCost);
        }
        private static int mapIndex(int oldIndex, int removedEdgeIndex) {
            return oldIndex < removedEdgeIndex ? oldIndex : oldIndex + 1;
        }
    }

    private enum MoveType { DELETE, INSERT, REPLACE, TWO_OPT }

    private record Move(MoveType type, int gain, int vertex, int position, int left, int right) {
        static Move delete(int gain, int position) { return new Move(MoveType.DELETE, gain, -1, position, -1, -1); }
        static Move insert(int gain, int vertex, int edgePosition) { return new Move(MoveType.INSERT, gain, vertex, edgePosition, -1, -1); }
        static Move replace(int gain, int vertex, int position) { return new Move(MoveType.REPLACE, gain, vertex, position, -1, -1); }
        static Move twoOpt(int gain, int left, int right) { return new Move(MoveType.TWO_OPT, gain, -1, -1, left, right); }
    }
}
