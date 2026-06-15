package algorithm.construction;

import algorithm.OptimizationAlgorithm;
import model.Instance;
import model.Solution;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Konstrukcyjny wariant 2-regret z prognozą na dwa kroki.
 *
 * <p>Parametry wariantu:</p>
 * <ul>
 *     <li>0-50% konstrukcji: top-5 kandydatów po 2-żalu,</li>
 *     <li>50-85% konstrukcji: top-15 kandydatów po 2-żalu,</li>
 *     <li>85-100% konstrukcji: top-25 kandydatów po 2-żalu,</li>
 *     <li>do 80% konstrukcji: drugi krok prognozy wybierany z top-3 po 2-żalu,</li>
 *     <li>po 80% konstrukcji: drugi krok prognozy wybierany z top-5 po 2-żalu,</li>
 *     <li>w drugim kroku prognozy używany jest bonus za krawędzie kandydackie N=10, lambda=20.</li>
 * </ul>
 */
public final class TailK25After85M5After80Lookahead implements OptimizationAlgorithm {
    private static final int NEIGHBOR_COUNT = 10;
    private static final double SECOND_STEP_BONUS_WEIGHT = 20.0;

    @Override
    public String name() {
        return "tailK25after85_m5after80_construction";
    }

    @Override
    public Solution solve(Instance instance, int startVertexId) {
        int[][] nearest = buildNearestNeighbors(instance, NEIGHBOR_COUNT);
        List<Integer> cycle = initialCycle(instance, startVertexId);
        List<Integer> unused = initialUnusedVertices(instance, cycle);

        while (!unused.isEmpty()) {
            List<VertexInsertion> insertions = buildInsertions(instance, cycle, unused);
            int k = Math.min(kForProgress(cycle.size(), instance.size), unused.size());
            List<VertexInsertion> firstCandidates = topByRegret(insertions, k);

            CandidateMove bestMove = null;

            for (VertexInsertion first : firstCandidates) {
                CandidateMove move = evaluateFirstMove(instance, cycle, unused, first, nearest);

                if (bestMove == null || move.isBetterThan(bestMove)) {
                    bestMove = move;
                }
            }

            cycle.add(bestMove.insertPosition, bestMove.vertexId);
            unused.remove(Integer.valueOf(bestMove.vertexId));
        }

        return new Solution(instance.name, startVertexId, cycle);
    }

    private static CandidateMove evaluateFirstMove(
            Instance instance,
            List<Integer> cycle,
            List<Integer> unused,
            VertexInsertion first,
            int[][] nearest
    ) {
        double totalScore = -first.bestCost;

        if (unused.size() > 1) {
            List<Integer> cycleAfterFirst = new ArrayList<>(cycle);
            cycleAfterFirst.add(first.bestPosition, first.vertexId);

            List<Integer> remaining = new ArrayList<>(unused);
            remaining.remove(Integer.valueOf(first.vertexId));

            List<VertexInsertion> secondInsertions = buildInsertions(instance, cycleAfterFirst, remaining);
            int m = Math.min(mForProgress(cycle.size(), instance.size), secondInsertions.size());
            List<VertexInsertion> secondCandidates = topByRegret(secondInsertions, m);

            double bestSecondScore = Double.NEGATIVE_INFINITY;
            for (VertexInsertion second : secondCandidates) {
                int bonus = candidateEdgeBonus(cycleAfterFirst, second.vertexId, second.bestPosition, nearest);
                double secondScore = -second.bestCost + SECOND_STEP_BONUS_WEIGHT * bonus;

                if (secondScore > bestSecondScore) {
                    bestSecondScore = secondScore;
                }
            }

            totalScore += bestSecondScore;
        }

        return new CandidateMove(
                first.vertexId,
                first.bestPosition,
                totalScore,
                -first.bestCost,
                first.regret
        );
    }

    private static List<Integer> initialCycle(Instance instance, int startVertexId) {
        int[][] distances = instance.distanceMatrix.distances;
        int secondVertex = -1;
        int bestDistance = Integer.MAX_VALUE;

        for (int vertexId = 0; vertexId < instance.size; vertexId++) {
            if (vertexId == startVertexId) {
                continue;
            }

            int distance = distances[startVertexId][vertexId];
            if (distance < bestDistance) {
                bestDistance = distance;
                secondVertex = vertexId;
            }
        }

        List<Integer> cycle = new ArrayList<>(instance.size);
        cycle.add(startVertexId);
        cycle.add(secondVertex);
        return cycle;
    }

    private static List<Integer> initialUnusedVertices(Instance instance, List<Integer> cycle) {
        List<Integer> unused = new ArrayList<>(instance.size - cycle.size());

        for (int vertexId = 0; vertexId < instance.size; vertexId++) {
            if (!cycle.contains(vertexId)) {
                unused.add(vertexId);
            }
        }

        return unused;
    }

    private static List<VertexInsertion> buildInsertions(
            Instance instance,
            List<Integer> cycle,
            List<Integer> vertices
    ) {
        List<VertexInsertion> result = new ArrayList<>(vertices.size());

        for (int vertexId : vertices) {
            result.add(bestInsertionForVertex(instance, cycle, vertexId));
        }

        return result;
    }

    private static VertexInsertion bestInsertionForVertex(Instance instance, List<Integer> cycle, int vertexId) {
        int[][] distances = instance.distanceMatrix.distances;
        int profit = instance.vertices[vertexId].profit;

        int bestCost = Integer.MAX_VALUE;
        int secondBestCost = Integer.MAX_VALUE;
        int bestPosition = -1;

        for (int edgePosition = 0; edgePosition < cycle.size(); edgePosition++) {
            int previous = cycle.get(edgePosition);
            int next = cycle.get((edgePosition + 1) % cycle.size());
            int cost = distances[previous][vertexId]
                    + distances[vertexId][next]
                    - distances[previous][next]
                    - profit;

            if (cost < bestCost) {
                secondBestCost = bestCost;
                bestCost = cost;
                bestPosition = edgePosition + 1;
            } else if (cost < secondBestCost) {
                secondBestCost = cost;
            }
        }

        int regret = secondBestCost - bestCost;
        return new VertexInsertion(vertexId, bestPosition, bestCost, regret);
    }

    private static List<VertexInsertion> topByRegret(List<VertexInsertion> insertions, int limit) {
        return insertions.stream()
                .sorted(Comparator
                        .comparingInt(VertexInsertion::regret).reversed()
                        .thenComparingInt(VertexInsertion::bestCost)
                        .thenComparingInt(VertexInsertion::vertexId))
                .limit(limit)
                .toList();
    }

    private static int kForProgress(int cycleSize, int totalVertices) {
        double progress = progress(cycleSize, totalVertices);

        if (progress < 0.50) {
            return 5;
        }
        if (progress < 0.85) {
            return 15;
        }
        return 25;
    }

    private static int mForProgress(int cycleSize, int totalVertices) {
        return progress(cycleSize, totalVertices) >= 0.80 ? 5 : 3;
    }

    private static double progress(int cycleSize, int totalVertices) {
        return (double) (cycleSize - 2) / Math.max(1, totalVertices - 2);
    }

    private static int candidateEdgeBonus(
            List<Integer> cycle,
            int vertexId,
            int insertPosition,
            int[][] nearest
    ) {
        int previousPosition = insertPosition - 1;
        int nextPosition = insertPosition == cycle.size() ? 0 : insertPosition;
        int previous = cycle.get(previousPosition);
        int next = cycle.get(nextPosition);

        int bonus = 0;
        if (isNearest(nearest, vertexId, previous)) {
            bonus++;
        }
        if (isNearest(nearest, vertexId, next)) {
            bonus++;
        }
        if (isNearest(nearest, previous, vertexId)) {
            bonus++;
        }
        if (isNearest(nearest, next, vertexId)) {
            bonus++;
        }
        return bonus;
    }

    private static int[][] buildNearestNeighbors(Instance instance, int neighborCount) {
        int[][] distances = instance.distanceMatrix.distances;
        int[][] nearest = new int[instance.size][Math.min(neighborCount, instance.size - 1)];

        for (int vertexId = 0; vertexId < instance.size; vertexId++) {
            List<Integer> candidates = new ArrayList<>(instance.size - 1);
            for (int otherVertex = 0; otherVertex < instance.size; otherVertex++) {
                if (otherVertex != vertexId) {
                    candidates.add(otherVertex);
                }
            }

            final int currentVertex = vertexId;
            candidates.sort(Comparator
                    .comparingInt((Integer otherVertex) -> distances[currentVertex][otherVertex])
                    .thenComparingInt(Integer::intValue));

            for (int index = 0; index < nearest[vertexId].length; index++) {
                nearest[vertexId][index] = candidates.get(index);
            }
        }

        return nearest;
    }

    private static boolean isNearest(int[][] nearest, int vertexId, int candidate) {
        for (int neighbor : nearest[vertexId]) {
            if (neighbor == candidate) {
                return true;
            }
        }
        return false;
    }

    private record VertexInsertion(int vertexId, int bestPosition, int bestCost, int regret) {
    }

    private record CandidateMove(
            int vertexId,
            int insertPosition,
            double score,
            int immediateDelta,
            int regret
    ) {
        private boolean isBetterThan(CandidateMove other) {
            if (Double.compare(score, other.score) != 0) {
                return score > other.score;
            }
            if (immediateDelta != other.immediateDelta) {
                return immediateDelta > other.immediateDelta;
            }
            if (regret != other.regret) {
                return regret > other.regret;
            }
            return vertexId < other.vertexId;
        }
    }
}
