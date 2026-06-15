package algorithm.localsearch;

import algorithm.SolutionImprover;
import algorithm.localsearch.move.Move;
import model.Instance;
import model.Solution;
import model.Vertex;

import java.util.List;

public final class SteepestLocalSearch implements SolutionImprover {

    private final NeighborhoodType neighborhoodType;

    public SteepestLocalSearch(NeighborhoodType neighborhoodType) {
        this.neighborhoodType = neighborhoodType;
    }

    @Override
    public Solution improve(Instance instance, Solution solution) {
        Cycle cycle = new Cycle(solution.cycle(), instance.size);
        Neighborhood neighborhood = new Neighborhood(cycle, instance.vertices);

        int[][] distanceMatrix = instance.distanceMatrix.distances;
        int[] profit = buildProfitArray(instance.vertices);

        boolean improved;

        do {
            improved = false;

            Move bestMove = null;
            int bestDelta = 0;

            List<Move> moves = createMoves(neighborhood);

            // Przegladamy cale biezace sasiedztwo
            // i wybieramy najlepszy ruch przynoszacy poprawe.
            for (Move move : moves) {
                int delta = move.delta(cycle, distanceMatrix, profit);

                if (delta > bestDelta) {
                    bestDelta = delta;
                    bestMove = move;
                }
            }

            if (bestMove != null) {
                bestMove.apply(cycle);
                improved = true;
            }
        } while (improved);

        return new Solution(solution.instanceName(), solution.startVertexId(), cycle.toList());
    }

    private List<Move> createMoves(Neighborhood neighborhood) {
        return neighborhoodType == NeighborhoodType.SWAP_VERTICES
                ? neighborhood.neighborhoodSwapVertices()
                : neighborhood.neighborhoodSwapEdges();
    }

    private static int[] buildProfitArray(Vertex[] vertices) {
        int[] profit = new int[vertices.length];

        for (Vertex vertex : vertices) {
            profit[vertex.id] = vertex.profit;
        }

        return profit;
    }
}
