package algorithm.localsearch;

import algorithm.OptimizationAlgorithm;
import algorithm.localsearch.move.Move;
import model.Instance;
import model.Solution;
import model.Vertex;

import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class GreedyLocalSearch implements OptimizationAlgorithm {

    private final String name;
    private final OptimizationAlgorithm initialSolutionAlgorithm;
    private final NeighborhoodType neighborhoodType;
    private final Random random;

    public GreedyLocalSearch(
            String name,
            OptimizationAlgorithm initialSolutionAlgorithm,
            NeighborhoodType neighborhoodType
    ) {
        this(name, initialSolutionAlgorithm, neighborhoodType, new Random());
    }

    public GreedyLocalSearch(
            String name,
            OptimizationAlgorithm initialSolutionAlgorithm,
            NeighborhoodType neighborhoodType,
            long seed
    ) {
        this(name, initialSolutionAlgorithm, neighborhoodType, new Random(seed));
    }

    private GreedyLocalSearch(
            String name,
            OptimizationAlgorithm initialSolutionAlgorithm,
            NeighborhoodType neighborhoodType,
            Random random
    ) {
        this.name = name;
        this.initialSolutionAlgorithm = initialSolutionAlgorithm;
        this.neighborhoodType = neighborhoodType;
        this.random = random;
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

            List<Move> moves = createMoves(neighborhood);
            Collections.shuffle(moves, random);

            // Przegladamy ruchy w losowej kolejnosci
            // i akceptujemy pierwszy ruch przynoszacy poprawe.
            for (Move move : moves) {
                int delta = move.delta(cycle, distanceMatrix, profit);

                if (delta > 0) {
                    move.apply(cycle);
                    improved = true;
                    break;
                }
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
