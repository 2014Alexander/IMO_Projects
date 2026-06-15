package faircomparison;

import algorithm.OptimizationAlgorithm;
import algorithm.improvement.PhaseTwoDelete;
import algorithm.localsearch.Cycle;
import model.Instance;
import model.Solution;

/**
 * Deterministic fast 2-regret construction with staged PhaseTwoDelete checkpoints:
 * 15%, 35%, 55%, 75%, 100% of instance size.
 * Starts from (startVertex, nearest(startVertex)).
 */
public final class StagedCycleP2D15355575Start implements OptimizationAlgorithm {
    private final String name;
    private final PhaseTwoDelete phaseTwoDelete;

    public StagedCycleP2D15355575Start(String name) {
        this.name = name;
        this.phaseTwoDelete = new PhaseTwoDelete();
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Solution solve(Instance instance, int startVertexId) {
        int secondVertex = nearestVertex(instance, startVertexId);
        Cycle cycle = createInitialCycle(instance, startVertexId, secondVertex);
        boolean[] inCycle = new boolean[instance.size];
        rebuildInCycle(inCycle, cycle);

        int[] stages = new int[] {15, 35, 55, 75, 100};
        for (int percentage : stages) {
            int targetSize = (instance.size * percentage + 99) / 100;
            insertUntilFast(instance, cycle, inCycle, targetSize);
            phaseTwoDelete.improve(instance, cycle);
            rebuildInCycle(inCycle, cycle);
        }
        return new Solution(instance.name, startVertexId, cycle.toList());
    }

    private static Cycle createInitialCycle(Instance instance, int firstVertex, int secondVertex) {
        Cycle cycle = new Cycle(instance.size);
        cycle.append(firstVertex);
        if (secondVertex >= 0 && secondVertex != firstVertex) {
            cycle.append(secondVertex);
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
                recomputeTop2(cycle, vertex, distances, profits, bestCost, secondBestCost,
                        bestEdgeIndex, secondBestEdgeIndex, bestFirst, bestSecond,
                        secondBestFirst, secondBestSecond);
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
                        || (regret == bestRegret && bestCost[vertex] == selectedBestCost && (bestVertex < 0 || vertex < bestVertex))) {
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
                    recomputeTop2(cycle, vertex, distances, profits, bestCost, secondBestCost,
                            bestEdgeIndex, secondBestEdgeIndex, bestFirst, bestSecond,
                            secondBestFirst, secondBestSecond);
                } else {
                    int mappedBestEdgeIndex = bestEdgeIndex[vertex] < removedEdgeIndex
                            ? bestEdgeIndex[vertex] : bestEdgeIndex[vertex] + 1;
                    int mappedSecondBestEdgeIndex = secondBestEdgeIndex[vertex] < removedEdgeIndex
                            ? secondBestEdgeIndex[vertex] : secondBestEdgeIndex[vertex] + 1;

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
                                      int[] bestCost, int[] secondBestCost,
                                      int[] bestEdgeIndex, int[] secondBestEdgeIndex,
                                      int[] bestFirst, int[] bestSecond,
                                      int[] secondBestFirst, int[] secondBestSecond) {
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
                                                 int[] bestCost, int[] secondBestCost,
                                                 int[] bestEdgeIndex, int[] secondBestEdgeIndex,
                                                 int[] bestFirst, int[] bestSecond,
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

    private static void sortFourCandidatesByEdgeIndex(int[] candidateEdgeIndex, int[] candidateFirst,
                                                       int[] candidateSecond, int[] candidateCost) {
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

    private static int insertionCost(int[][] distances, int[] profits, int firstVertex, int insertedVertex, int secondVertex) {
        return distances[firstVertex][insertedVertex]
                + distances[insertedVertex][secondVertex]
                - distances[firstVertex][secondVertex]
                - profits[insertedVertex];
    }

    private static int nearestVertex(Instance instance, int vertexId) {
        int bestVertex = -1;
        int bestDistance = Integer.MAX_VALUE;
        for (int v = 0; v < instance.size; v++) {
            if (v == vertexId) {
                continue;
            }
            int distance = instance.distanceMatrix.distances[vertexId][v];
            if (distance < bestDistance || (distance == bestDistance && (bestVertex < 0 || v < bestVertex))) {
                bestDistance = distance;
                bestVertex = v;
            }
        }
        return bestVertex;
    }

    private static int[] profits(Instance instance) {
        int[] profits = new int[instance.size];
        for (int vertexId = 0; vertexId < instance.size; vertexId++) {
            profits[vertexId] = instance.vertices[vertexId].profit;
        }
        return profits;
    }
}
