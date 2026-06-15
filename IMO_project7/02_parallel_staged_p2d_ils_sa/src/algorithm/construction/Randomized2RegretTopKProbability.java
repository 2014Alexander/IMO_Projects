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
 * Zrandomizowany konstruktor 2-regret z parametrami topK oraz prawdopodobienstwem eksploracji.
 * W kazdym kroku wybiera najlepszy ruch albo losowy ruch z dalszych pozycji rankingu.
 */
public final class Randomized2RegretTopKProbability implements OptimizationAlgorithm {
    private final String name;
    private final int topK;
    private final double explorationProbability;
    private final Random random;

    public Randomized2RegretTopKProbability(String name, int topK, double explorationProbability, Random random) {
        this.name = name;
        this.topK = topK;
        this.explorationProbability = explorationProbability;
        this.random = Objects.requireNonNull(random, "random");
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Solution solve(Instance instance, int startVertexId) {
        int[][] distances = instance.distanceMatrix.distances;
        int[] profits = extractProfits(instance);
        int vertexCount = instance.size;

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

        while (!notUsed.isEmpty()) {
            List<RegretCandidate> candidates = buildCandidates(distances, profits, cycle, notUsed);
            candidates.sort(Comparator.comparingInt(RegretCandidate::regret).reversed());

            RegretCandidate selected = chooseCandidate(candidates);
            cycle.add(selected.bestPosition(), selected.vertexId());
            notUsed.remove(selected.notUsedIndex());
        }

        return new Solution(instance.name, startVertexId, cycle);
    }

    private RegretCandidate chooseCandidate(List<RegretCandidate> candidates) {
        int availableExplorationCandidates = Math.min(topK, candidates.size()) - 1;

        if (availableExplorationCandidates > 0 && random.nextDouble() < explorationProbability) {
            int selectedRankIndex = 1 + random.nextInt(availableExplorationCandidates);
            return candidates.get(selectedRankIndex);
        }

        return candidates.get(0);
    }

    private static List<RegretCandidate> buildCandidates(
            int[][] distances,
            int[] profits,
            List<Integer> cycle,
            List<Integer> notUsed
    ) {
        List<RegretCandidate> candidates = new ArrayList<>(notUsed.size());

        for (int notUsedIndex = 0; notUsedIndex < notUsed.size(); notUsedIndex++) {
            int vertexId = notUsed.get(notUsedIndex);

            int bestCost = Integer.MAX_VALUE;
            int secondBestCost = Integer.MAX_VALUE;
            int bestPosition = -1;

            for (int cycleIndex = 0; cycleIndex < cycle.size(); cycleIndex++) {
                int firstVertex = cycle.get(cycleIndex);
                int secondVertex = cycle.get((cycleIndex + 1) % cycle.size());
                int increaseLength = distances[firstVertex][vertexId]
                        + distances[vertexId][secondVertex]
                        - distances[firstVertex][secondVertex];
                int cost = increaseLength - profits[vertexId];

                if (cost < bestCost) {
                    secondBestCost = bestCost;
                    bestCost = cost;
                    bestPosition = cycleIndex + 1;
                } else if (cost < secondBestCost) {
                    secondBestCost = cost;
                }
            }

            int regret = secondBestCost - bestCost;
            candidates.add(new RegretCandidate(vertexId, notUsedIndex, bestPosition, regret));
        }

        return candidates;
    }

    private static int[] extractProfits(Instance instance) {
        int[] profits = new int[instance.size];

        for (int vertexId = 0; vertexId < instance.size; vertexId++) {
            profits[vertexId] = instance.vertices[vertexId].profit;
        }

        return profits;
    }

    private record RegretCandidate(int vertexId, int notUsedIndex, int bestPosition, int regret) {
    }
}
