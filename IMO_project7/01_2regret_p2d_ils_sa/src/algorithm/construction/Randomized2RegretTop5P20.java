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
 * Randomized original 2-regret constructor.
 *
 * <p>At every construction step it computes the same 2-regret ranking as the
 * original TwoRegretCost. With probability 80% it chooses the best ranked
 * vertex. With probability 20% it chooses randomly from ranks 2-5 when such
 * candidates exist. The selected vertex is inserted into its best position.</p>
 */
public final class Randomized2RegretTop5P20 implements OptimizationAlgorithm {
    private static final double EXPLORATION_PROBABILITY = 0.20;
    private static final int MAX_EXPLORATION_RANK = 5;

    private final Random random;

    public Randomized2RegretTop5P20() {
        this(new Random());
    }

    public Randomized2RegretTop5P20(Random random) {
        this.random = Objects.requireNonNull(random, "random");
    }

    @Override
    public String name() {
        return "randomized_2regret_top5p20";
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
        int availableExplorationCandidates = Math.min(MAX_EXPLORATION_RANK, candidates.size()) - 1;

        if (availableExplorationCandidates > 0 && random.nextDouble() < EXPLORATION_PROBABILITY) {
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
