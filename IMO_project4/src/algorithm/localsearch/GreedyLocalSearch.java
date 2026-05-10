package algorithm.localsearch;

import algorithm.SolutionImprover;
import algorithm.localsearch.move.Move;
import model.Instance;
import model.Solution;
import model.Vertex;

import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class GreedyLocalSearch implements SolutionImprover {

    private final NeighborhoodType neighborhoodType;
    private final Random random;

    public GreedyLocalSearch(NeighborhoodType neighborhoodType) {
        this(neighborhoodType, new Random());
    }

    public GreedyLocalSearch(NeighborhoodType neighborhoodType, long seed) {
        this(neighborhoodType, new Random(seed));
    }

    private GreedyLocalSearch(
            NeighborhoodType neighborhoodType,
            Random random
    ) {
        this.neighborhoodType = neighborhoodType;
        this.random = random;
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
