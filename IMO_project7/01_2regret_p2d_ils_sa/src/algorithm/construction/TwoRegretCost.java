package algorithm.construction;

import algorithm.OptimizationAlgorithm;
import model.Instance;
import model.Solution;

import java.util.ArrayList;
import java.util.List;

/**
 * Konstrukcyjny algorytm 2-regret bazujący na koszcie wstawienia wierzchołka.
 */
public final class TwoRegretCost implements OptimizationAlgorithm {

    /**
     * Tworzy algorytm 2-regret.
     */
    public TwoRegretCost() {
    }

    /**
     * Zwraca nazwę algorytmu konstrukcyjnego.
     *
     * @return nazwa algorytmu
     */
    @Override
    public String name() {
        return "2-Regret";
    }

    /**
     * Buduje rozwiązanie, wybierając w każdej iteracji wierzchołek
     * o największym żalu między najlepszym i drugim najlepszym miejscem wstawienia.
     *
     * @param instance      instancja problemu
     * @param startVertexId wierzchołek startowy
     * @return rozwiązanie konstrukcyjne
     */
    @Override
    public Solution solve(Instance instance, int startVertexId) {
        int[][] distances = instance.distanceMatrix.distances;
        int[] profit = extractProfits(instance);
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
            int bestVertex = -1;
            int bestVertexIndexInNotUsed = -1;
            int bestRegret = Integer.MIN_VALUE;
            int bestPositionForBestVertex = -1;

            for (int notUsedIndex = 0; notUsedIndex < notUsed.size(); notUsedIndex++) {
                int vertexId = notUsed.get(notUsedIndex);

                int bestCost = Integer.MAX_VALUE;
                int secondBestCost = Integer.MAX_VALUE;
                int bestPosition = -1;

                for (int cycleIndex = 0; cycleIndex < cycle.size(); cycleIndex++) {
                    int firstVertex = cycle.get(cycleIndex);
                    int secondVertexInEdge = cycle.get((cycleIndex + 1) % cycle.size());
                    int increaseLength = distances[firstVertex][vertexId]
                            + distances[vertexId][secondVertexInEdge]
                            - distances[firstVertex][secondVertexInEdge];
                    int cost = increaseLength - profit[vertexId];

                    if (cost < bestCost) {
                        secondBestCost = bestCost;
                        bestCost = cost;
                        bestPosition = cycleIndex + 1;
                    } else if (cost < secondBestCost) {
                        secondBestCost = cost;
                    }
                }

                int regret = secondBestCost - bestCost;

                if (regret > bestRegret) {
                    bestRegret = regret;
                    bestVertex = vertexId;
                    bestVertexIndexInNotUsed = notUsedIndex;
                    bestPositionForBestVertex = bestPosition;
                }
            }

            cycle.add(bestPositionForBestVertex, bestVertex);
            notUsed.remove(bestVertexIndexInNotUsed);
        }

        return new Solution(
                instance.name,
                startVertexId,
                cycle
        );
    }

    private static int[] extractProfits(Instance instance) {
        int[] profit = new int[instance.size];

        for (int vertexId = 0; vertexId < instance.size; vertexId++) {
            profit[vertexId] = instance.vertices[vertexId].profit;
        }

        return profit;
    }
}
