package algorithm.construction.twophase;

import algorithm.ConstructionAlgorithm;
import model.Instance;
import model.Solution;
import model.Vertex;

import java.util.ArrayList;
import java.util.List;

public class NearestNeighbor implements ConstructionAlgorithm {

    @Override
    public String name() {
        return "NN";
    }

    @Override
    public Solution solve(Instance instance, Integer startVertexId) {
        validateInputs(instance, startVertexId);

        int[][] D = instance.distanceMatrix.distances;
        Vertex[] vertices = instance.vertices;
        int n = instance.size;

        List<Integer> cycle = new ArrayList<>(n);

        // used[v] = true oznacza, że wierzchołek v został już dodany do cyklu.
        boolean[] used = new boolean[n];

        // Zaczynamy od wierzchołka startowego.
        cycle.add(startVertexId);
        used[startVertexId] = true;

        // Dopóki istnieją jeszcze niewykorzystane wierzchołki,
        // dokładamy do cyklu wierzchołek minimalizujący koszt:
        // D[current][v] - profit[v].
        while (cycle.size() < n) {
            int current = cycle.get(cycle.size() - 1);

            int bestVertex = -1;
            int bestCost = Integer.MAX_VALUE;

            // Szukamy nieużytego wierzchołka o najmniejszym koszcie
            // względem aktualnego końca cyklu.
            for (int v = 0; v < n; v++) {
                if (used[v]) {
                    continue;
                }

                int cost = D[current][v] - vertices[v].profit;

                if (cost < bestCost) {
                    bestCost = cost;
                    bestVertex = v;
                }
            }

            // Dodajemy najlepszy znaleziony wierzchołek na koniec cyklu
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