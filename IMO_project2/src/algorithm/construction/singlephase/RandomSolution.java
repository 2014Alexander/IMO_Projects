package algorithm.construction.singlephase;

import algorithm.OptimizationAlgorithm;
import model.Instance;
import model.Solution;
import model.Vertex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class RandomSolution implements OptimizationAlgorithm {

    private final Random random;

    public RandomSolution() {
        this.random = new Random();
    }

    public RandomSolution(long seed) {
        this.random = new Random(seed);
    }

    @Override
    public String name() {
        return "RS";
    }

    @Override
    public Solution solve(Instance instance, int startVertexId) {
        validateInputs(instance, startVertexId);

        int n = instance.size;

        // Losujemy liczbę wierzchołków, które znajdą się w rozwiązaniu.
        // random.nextInt(n - 1) zwraca liczbę z przedziału [0, n - 2],
        // więc po dodaniu 2 otrzymujemy k z przedziału [2, n].
        int k = 2 + random.nextInt(n - 1);

        // Cykl zawsze zaczynamy od wierzchołka startowego.
        List<Integer> cycle = new ArrayList<>(k);
        cycle.add(startVertexId);

        // Zbieramy identyfikatory wszystkich pozostałych wierzchołków.
        List<Integer> remainingVertexIds = new ArrayList<>(n - 1);
        for (Vertex vertex : instance.vertices) {
            if (vertex.id != startVertexId) {
                remainingVertexIds.add(vertex.id);
            }
        }

        // Tasujemy identyfikatory pozostałych wierzchołków.
        Collections.shuffle(remainingVertexIds, random);


        // Dopełniamy cykl losowo wybranymi wierzchołkami.
        cycle.addAll(remainingVertexIds.subList(0, k - 1));

        return new Solution(
                instance.name,
                startVertexId,
                cycle
        );
    }

    private static void validateInputs(Instance instance, int startVertexId) {
        if (instance == null) {
            throw new IllegalArgumentException("Instance nie może być null.");
        }
        if (instance.size < 2) {
            throw new IllegalArgumentException("Instancja musi zawierać co najmniej 2 wierzchołki.");
        }
        if (startVertexId < 0 || startVertexId >= instance.size) {
            throw new IllegalArgumentException("startVertexId jest poza zakresem instancji.");
        }
    }
}
