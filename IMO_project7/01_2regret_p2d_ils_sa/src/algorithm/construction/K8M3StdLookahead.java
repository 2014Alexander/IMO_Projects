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
 *     <li>50-100% konstrukcji: top-8 kandydatów po 2-żalu,</li>
 *     <li>drugi krok prognozy: top-3 kandydatów po 2-żalu,</li>
 *     <li>w drugim kroku prognozy używany jest bonus za krawędzie kandydackie N=10, lambda=20.</li>
 * </ul>
 */
public final class K8M3StdLookahead implements OptimizationAlgorithm {
    private static final int NEIGHBOR_COUNT = 10;
    private static final double SECOND_STEP_BONUS_WEIGHT = 20.0;
    private static final int TOP_INSERTIONS = 3;
    private static final int INF = 1_000_000_000;

    @Override
    public String name() {
        return "k8_m3_std_construction";
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
                CandidateMove move = evaluateFirstMove(instance, cycle, unused, insertions, first, nearest);

                if (bestMove == null || move.isBetterThan(bestMove)) {
                    bestMove = move;
                }
            }

            cycle.add(bestMove.edgeIndex + 1, bestMove.vertexId);
            unused.remove(bestMove.unusedIndex);
        }

        return new Solution(instance.name, startVertexId, cycle);
    }

    private static CandidateMove evaluateFirstMove(
            Instance instance,
            List<Integer> cycle,
            List<Integer> unused,
            List<VertexInsertion> insertions,
            VertexInsertion first,
            int[][] nearest
    ) {
        double totalScore = -first.bestCost();

        if (unused.size() > 1) {
            List<Integer> cycleAfterFirst = new ArrayList<>(cycle);
            cycleAfterFirst.add(first.bestEdge() + 1, first.vertexId());

            List<SecondInsertion> secondInsertions = updatedSecondInsertions(
                    instance,
                    cycle,
                    unused,
                    insertions,
                    first
            );
            List<SecondInsertion> secondCandidates = topSecondByRegret(secondInsertions, Math.min(3, secondInsertions.size()));

            double bestSecondScore = Double.NEGATIVE_INFINITY;
            for (SecondInsertion second : secondCandidates) {
                int bonus = candidateEdgeBonusByEdge(cycleAfterFirst, second.vertexId(), second.bestEdge(), nearest);
                double secondScore = -second.bestCost() + SECOND_STEP_BONUS_WEIGHT * bonus;

                if (secondScore > bestSecondScore) {
                    bestSecondScore = secondScore;
                }
            }

            totalScore += bestSecondScore;
        }

        return new CandidateMove(
                first.unusedIndex(),
                first.vertexId(),
                first.bestEdge(),
                totalScore,
                -first.bestCost(),
                first.regret()
        );
    }

    private static List<SecondInsertion> updatedSecondInsertions(
            Instance instance,
            List<Integer> cycle,
            List<Integer> unused,
            List<VertexInsertion> insertions,
            VertexInsertion first
    ) {
        int[][] distances = instance.distanceMatrix.distances;
        int firstVertex = first.vertexId();
        int firstEdge = first.bestEdge();
        int previous = cycle.get(firstEdge);
        int next = cycle.get((firstEdge + 1) % cycle.size());
        List<SecondInsertion> result = new ArrayList<>(unused.size() - 1);

        for (VertexInsertion insertion : insertions) {
            if (insertion.unusedIndex() == first.unusedIndex()) {
                continue;
            }

            int vertex = insertion.vertexId();
            int profit = instance.vertices[vertex].profit;

            CostPosition old1;
            CostPosition old2;
            if (insertion.edge(0) != firstEdge) {
                old1 = new CostPosition(insertion.cost(0), adjustedEdge(insertion.edge(0), firstEdge));
                if (insertion.edge(1) != firstEdge) {
                    old2 = new CostPosition(insertion.cost(1), adjustedEdge(insertion.edge(1), firstEdge));
                } else {
                    old2 = new CostPosition(insertion.cost(2), adjustedEdge(insertion.edge(2), firstEdge));
                }
            } else {
                old1 = new CostPosition(insertion.cost(1), adjustedEdge(insertion.edge(1), firstEdge));
                old2 = new CostPosition(insertion.cost(2), adjustedEdge(insertion.edge(2), firstEdge));
            }

            int new1Cost = distances[previous][vertex] + distances[vertex][firstVertex]
                    - distances[previous][firstVertex] - profit;
            int new2Cost = distances[firstVertex][vertex] + distances[vertex][next]
                    - distances[firstVertex][next] - profit;

            CostPosition best = old1;
            CostPosition second = old2;
            CostPosition new1 = new CostPosition(new1Cost, firstEdge);
            CostPosition new2 = new CostPosition(new2Cost, firstEdge + 1);

            CostPosition[] candidates = {old1, old2, new1, new2};
            for (CostPosition candidate : candidates) {
                if (candidate.cost() < best.cost()) {
                    second = best;
                    best = candidate;
                } else if (candidate != best && candidate.cost() < second.cost()) {
                    second = candidate;
                }
            }

            result.add(new SecondInsertion(vertex, best.edge(), best.cost(), second.cost() - best.cost()));
        }

        return result;
    }

    private static int adjustedEdge(int edge, int firstEdge) {
        if (edge < 0) {
            return edge;
        }
        return edge < firstEdge ? edge : edge + 1;
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

        for (int index = 0; index < vertices.size(); index++) {
            result.add(topInsertionForVertex(instance, cycle, vertices.get(index), index));
        }

        return result;
    }

    private static VertexInsertion topInsertionForVertex(
            Instance instance,
            List<Integer> cycle,
            int vertexId,
            int unusedIndex
    ) {
        int[][] distances = instance.distanceMatrix.distances;
        int profit = instance.vertices[vertexId].profit;
        int[] costs = new int[TOP_INSERTIONS];
        int[] edges = new int[TOP_INSERTIONS];

        for (int i = 0; i < TOP_INSERTIONS; i++) {
            costs[i] = INF;
            edges[i] = -1;
        }

        for (int edgePosition = 0; edgePosition < cycle.size(); edgePosition++) {
            int previous = cycle.get(edgePosition);
            int next = cycle.get((edgePosition + 1) % cycle.size());
            int cost = distances[previous][vertexId]
                    + distances[vertexId][next]
                    - distances[previous][next]
                    - profit;

            insertCostPosition(costs, edges, cost, edgePosition);
        }

        return new VertexInsertion(unusedIndex, vertexId, costs, edges);
    }

    private static void insertCostPosition(int[] costs, int[] edges, int cost, int edge) {
        for (int index = 0; index < TOP_INSERTIONS; index++) {
            if (cost < costs[index]) {
                for (int move = TOP_INSERTIONS - 1; move > index; move--) {
                    costs[move] = costs[move - 1];
                    edges[move] = edges[move - 1];
                }
                costs[index] = cost;
                edges[index] = edge;
                return;
            }
        }
    }

    private static List<VertexInsertion> topByRegret(List<VertexInsertion> insertions, int limit) {
        return insertions.stream()
                .sorted(Comparator
                        .comparingInt(VertexInsertion::regret).reversed()
                        .thenComparingInt(VertexInsertion::bestCost)
                        .thenComparingInt(VertexInsertion::unusedIndex))
                .limit(limit)
                .toList();
    }

    private static List<SecondInsertion> topSecondByRegret(List<SecondInsertion> insertions, int limit) {
        return insertions.stream()
                .sorted(Comparator
                        .comparingInt(SecondInsertion::regret).reversed()
                        .thenComparingInt(SecondInsertion::bestCost)
                        .thenComparingInt(SecondInsertion::vertexId))
                .limit(limit)
                .toList();
    }

    private static int kForProgress(int cycleSize, int totalVertices) {
        double progress = progress(cycleSize, totalVertices);
        return progress < 0.50 ? 5 : 8;
    }

    private static double progress(int cycleSize, int totalVertices) {
        return (double) (cycleSize - 2) / Math.max(1, totalVertices - 2);
    }

    private static int candidateEdgeBonusByEdge(
            List<Integer> cycle,
            int vertexId,
            int edgePosition,
            int[][] nearest
    ) {
        int previous = cycle.get(edgePosition);
        int next = cycle.get((edgePosition + 1) % cycle.size());

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

    private record VertexInsertion(int unusedIndex, int vertexId, int[] costs, int[] edges) {
        private int cost(int index) {
            return costs[index];
        }

        private int edge(int index) {
            return edges[index];
        }

        private int bestCost() {
            return cost(0);
        }

        private int bestEdge() {
            return edge(0);
        }

        private int regret() {
            return cost(1) - cost(0);
        }
    }

    private record SecondInsertion(int vertexId, int bestEdge, int bestCost, int regret) {
    }

    private record CostPosition(int cost, int edge) {
    }

    private record CandidateMove(
            int unusedIndex,
            int vertexId,
            int edgeIndex,
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
            return vertexId > other.vertexId;
        }
    }
}
