package algorithm.localsearch;

import algorithm.OptimizationAlgorithm;
import algorithm.localsearch.move.Move;
import model.Instance;
import model.Solution;
import model.Vertex;

import java.util.List;

public final class SteepestLocalSearch implements OptimizationAlgorithm {

    private final String name;
    private final OptimizationAlgorithm initialSolutionAlgorithm;
    private final NeighborhoodType neighborhoodType;

    public SteepestLocalSearch(
            String name,
            OptimizationAlgorithm initialSolutionAlgorithm,
            NeighborhoodType neighborhoodType
    ) {
        this.name = name;
        this.initialSolutionAlgorithm = initialSolutionAlgorithm;
        this.neighborhoodType = neighborhoodType;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Solution solve(Instance instance, int startVertexId) {
        Solution initialSolution = initialSolutionAlgorithm.solve(instance, startVertexId);

        Cycle cycle = new Cycle(initialSolution.cycle(), instance.size);
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

        return new Solution(instance.name, startVertexId, cycle);
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
