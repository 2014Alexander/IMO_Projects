package algorithm.construction.twophase;

import algorithm.ConstructionAlgorithm;
import model.Instance;
import model.Solution;

import java.util.ArrayList;
import java.util.List;

public class NearestNeighborWithoutProfit implements ConstructionAlgorithm {

    @Override
    public String name() {
        return "NNa";
    }

    @Override
    public Solution solve(Instance instance, Integer startVertexId) {
        validateInputs(instance, startVertexId);

        int[][] D = instance.distanceMatrix.distances;
        int n = instance.size;

        List<Integer> cycle = new ArrayList<>(n);

        // used[v] = true oznacza, że wierzchołek v został już dodany do cyklu.
        boolean[] used = new boolean[n];

        // Zaczynamy od wierzchołka startowego.
        cycle.add(startVertexId);
        used[startVertexId] = true;

        // Dopóki istnieją jeszcze niewykorzystane wierzchołki,
        // dokładamy do cyklu najbliższego sąsiada ostatniego elementu.
        while (cycle.size() < n) {
            int current = cycle.get(cycle.size() - 1);

            int bestVertex = -1;
            int bestDistance = Integer.MAX_VALUE;

            // Szukamy nieużytego wierzchołka o najmniejszej odległości
            // od aktualnego końca cyklu.
            for (int v = 0; v < n; v++) {
                if (used[v]) {
                    continue;
                }

                int distance = D[current][v];

                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestVertex = v;
                }
            }

            // Dodajemy znaleziony najbliższy wierzchołek na koniec cyklu
            // i oznaczamy go jako użyty.
            cycle.add(bestVertex);
            used[bestVertex] = true;
        }

        return new Solution(
                instance.name,
                startVertexId,
                cycle
        );
    }

    private static void validateInputs(Instance instance, Integer startVertexId) {
        if (instance == null) {
            throw new IllegalArgumentException("Instance nie może być null.");
        }
        if (startVertexId == null) {
            throw new IllegalArgumentException("startVertexId nie może być null.");
        }
        if (instance.size < 1) {
            throw new IllegalArgumentException("Instancja musi zawierać co najmniej 1 wierzchołek.");
        }
        if (startVertexId < 0 || startVertexId >= instance.size) {
            throw new IllegalArgumentException("startVertexId jest poza zakresem instancji.");
        }
    }
}