package algorithm.construction;

import algorithm.OptimizationAlgorithm;
import model.Instance;
import model.Solution;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Fast randomized 2-regret with the same topK/probability selection rule as
 * Randomized2RegretTopKProbability, but with incremental maintenance of the
 * two best insertion edges for every unused vertex.
 */
public final class FastRandomized2RegretTopKProbability implements OptimizationAlgorithm {
    private final String name;
    private final int topK;
    private final double explorationProbability;
    private final Random random;
    private RunStats lastStats = new RunStats(0, 0, 0, 0, 0);

    public FastRandomized2RegretTopKProbability(String name, int topK, double explorationProbability, Random random) {
        this.name = Objects.requireNonNull(name, "name");
        this.topK = topK;
        this.explorationProbability = explorationProbability;
        this.random = Objects.requireNonNull(random, "random");
    }

    @Override
    public String name() {
        return name;
    }

    public RunStats lastStats() {
        return lastStats;
    }

    @Override
    public Solution solve(Instance instance, int startVertexId) {
        int[][] distances = instance.distanceMatrix.distances;
        int[] profits = extractProfits(instance);
        int vertexCount = instance.size;
        long costEvaluations = 0;
        long fullRescans = 0;
        long newEdgeChecks = 0;
        long top2Invalidations = 0;
        long iterations = 0;

        List<Integer> cycle = new ArrayList<>(vertexCount);
        List<Integer> notUsed = new ArrayList<>(vertexCount - 1);

        cycle.add(startVertexId);
        for (int vertexId = 0; vertexId < vertexCount; vertexId++) {
            if (vertexId != startVertexId) {
                notUsed.add(vertexId);
            }
        }

        int secondVertex = -1;
        int secondVertexIndexInNotUsed = -1;
        int smallestDistanceFromStart = Integer.MAX_VALUE;
        for (int notUsedIndex = 0; notUsedIndex < notUsed.size(); notUsedIndex++) {
            int vertexId = notUsed.get(notUsedIndex);
            int distanceFromStart = distances[startVertexId][vertexId];
            if (distanceFromStart < smallestDistanceFromStart) {
                smallestDistanceFromStart = distanceFromStart;
                secondVertex = vertexId;
                secondVertexIndexInNotUsed = notUsedIndex;
            }
        }

        cycle.add(secondVertex);
        notUsed.remove(secondVertexIndexInNotUsed);

        Top2[] stats = new Top2[vertexCount];
        for (int vertexId : notUsed) {
            Top2 top2 = recomputeTop2(cycle, vertexId, distances, profits);
            costEvaluations += cycle.size();
            fullRescans++;
            stats[vertexId] = top2;
        }

        while (!notUsed.isEmpty()) {
            iterations++;
            List<RegretCandidate> candidates = buildCandidates(notUsed, stats);
            candidates.sort(Comparator.comparingInt(RegretCandidate::regret).reversed());
            RegretCandidate selected = chooseCandidate(candidates);

            Top2 selectedTop2 = stats[selected.vertexId()];
            int removedFirst = selectedTop2.bestFirst;
            int removedSecond = selectedTop2.bestSecond;
            int removedEdgeIndex = selectedTop2.bestEdgeIndex;
            int insertPosition = removedEdgeIndex + 1;

            cycle.add(insertPosition, selected.vertexId());
            notUsed.remove(selected.notUsedIndex());
            stats[selected.vertexId()] = null;

            for (int vertexId : notUsed) {
                Top2 current = stats[vertexId];
                boolean invalid = current.usesEdge(removedFirst, removedSecond);
                if (invalid) {
                    top2Invalidations++;
                    Top2 refreshed = recomputeTop2(cycle, vertexId, distances, profits);
                    costEvaluations += cycle.size();
                    fullRescans++;
                    stats[vertexId] = refreshed;
                } else {
                    Top2 mapped = current.afterInsertionAt(removedEdgeIndex);
                    int costFirstNew = insertionCost(distances, profits, removedFirst, vertexId, selected.vertexId());
                    int costSecondNew = insertionCost(distances, profits, selected.vertexId(), vertexId, removedSecond);
                    costEvaluations += 2;
                    newEdgeChecks += 2;
                    stats[vertexId] = updateWithNewEdges(
                            mapped,
                            removedEdgeIndex,
                            removedFirst,
                            selected.vertexId(),
                            costFirstNew,
                            selected.vertexId(),
                            removedSecond,
                            costSecondNew
                    );
                }
            }
        }

        lastStats = new RunStats(costEvaluations, fullRescans, newEdgeChecks, top2Invalidations, iterations);
        return new Solution(instance.name, startVertexId, cycle);
    }

    private List<RegretCandidate> buildCandidates(List<Integer> notUsed, Top2[] stats) {
        List<RegretCandidate> candidates = new ArrayList<>(notUsed.size());
        for (int notUsedIndex = 0; notUsedIndex < notUsed.size(); notUsedIndex++) {
            int vertexId = notUsed.get(notUsedIndex);
            Top2 top2 = stats[vertexId];
            int regret = top2.secondBestCost - top2.bestCost;
            candidates.add(new RegretCandidate(vertexId, notUsedIndex, regret));
        }
        return candidates;
    }

    private RegretCandidate chooseCandidate(List<RegretCandidate> candidates) {
        int availableExplorationCandidates = Math.min(topK, candidates.size()) - 1;

        if (availableExplorationCandidates > 0 && random.nextDouble() < explorationProbability) {
            int selectedRankIndex = 1 + random.nextInt(availableExplorationCandidates);
            return candidates.get(selectedRankIndex);
        }

        return candidates.get(0);
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

    private static Top2 updateWithNewEdges(
            Top2 mapped,
            int removedEdgeIndex,
            int firstNewA,
            int firstNewB,
            int firstNewCost,
            int secondNewA,
            int secondNewB,
            int secondNewCost
    ) {
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
        return distances[firstVertex][insertedVertex]
                + distances[insertedVertex][secondVertex]
                - distances[firstVertex][secondVertex]
                - profits[insertedVertex];
    }

    private static int[] extractProfits(Instance instance) {
        int[] profits = new int[instance.size];
        for (int vertexId = 0; vertexId < instance.size; vertexId++) {
            profits[vertexId] = instance.vertices[vertexId].profit;
        }
        return profits;
    }

    private record RegretCandidate(int vertexId, int notUsedIndex, int regret) {}
    private record EdgeCandidate(int edgeIndex, int first, int second, int cost) {}

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
            return new Top2(
                    best.edgeIndex, best.first, best.second, best.cost,
                    second.edgeIndex, second.first, second.second, second.cost
            );
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

        Top2(
                int bestEdgeIndex,
                int bestFirst,
                int bestSecond,
                int bestCost,
                int secondBestEdgeIndex,
                int secondBestFirst,
                int secondBestSecond,
                int secondBestCost
        ) {
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
            return (bestFirst == first && bestSecond == second)
                    || (secondBestFirst == first && secondBestSecond == second);
        }

        Top2 afterInsertionAt(int removedEdgeIndex) {
            return new Top2(
                    mapIndex(bestEdgeIndex, removedEdgeIndex), bestFirst, bestSecond, bestCost,
                    mapIndex(secondBestEdgeIndex, removedEdgeIndex), secondBestFirst, secondBestSecond, secondBestCost
            );
        }

        private static int mapIndex(int oldIndex, int removedEdgeIndex) {
            return oldIndex < removedEdgeIndex ? oldIndex : oldIndex + 1;
        }
    }

    public record RunStats(long costEvaluations, long fullRescans, long newEdgeChecks, long top2Invalidations, long iterations) {}
}
