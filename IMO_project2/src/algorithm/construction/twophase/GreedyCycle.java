package algorithm.construction.twophase;

import algorithm.OptimizationAlgorithm;
import model.Instance;
import model.Solution;
import model.Vertex;

import java.util.ArrayList;
import java.util.List;

public class GreedyCycle implements OptimizationAlgorithm {

    @Override
    public String name() {
        return "GC";
    }

    @Override
    public Solution solve(Instance instance, int startVertexId) {
        validateInputs(instance, startVertexId);

        int[][] D = instance.distanceMatrix.distances;
        Vertex[] vertices = instance.vertices;
        int n = instance.size;

        List<Integer> cycle = new ArrayList<>(n);
        List<Integer> notUsed = new ArrayList<>(n - 2);

        // Cykl zaczynamy od wierzchołka startowego.
        cycle.add(startVertexId);

        // Drugi wierzchołek wybieramy jako najbliższy sąsiad startu.
        int secondVertex = -1;
        int smallestDistanceFromStart = Integer.MAX_VALUE;

        for (int vertexId = 0; vertexId < n; vertexId++) {
            if (vertexId == startVertexId) {
                continue;
            }

            int distanceFromStart = D[startVertexId][vertexId];

            if (distanceFromStart < smallestDistanceFromStart) {
                smallestDistanceFromStart = distanceFromStart;
                secondVertex = vertexId;
            }
        }

        cycle.add(secondVertex);

        // notUsed zawiera wszystkie wierzchołki poza tymi,
        // które już należą do początkowego cyklu.
        for (int vertexId = 0; vertexId < n; vertexId++) {
            if (vertexId != startVertexId && vertexId != secondVertex) {
                notUsed.add(vertexId);
            }
        }

        // W każdej iteracji wybieramy taki wierzchołek i takie miejsce wstawienia,
        // które minimalizują koszt: increase - profit[v].
        while (!notUsed.isEmpty()) {
            int bestVertex = -1;
            int bestVertexIndexInNotUsed = -1;
            int bestInsertionPosition = -1;
            int bestCost = Integer.MAX_VALUE;

            // Dla każdego jeszcze niewstawionego wierzchołka
            // szukamy jego najlepszego miejsca wstawienia.
            for (int notUsedIndex = 0; notUsedIndex < notUsed.size(); notUsedIndex++) {
                int vertexId = notUsed.get(notUsedIndex);

                int vertexBestInsertionPosition = -1;
                int vertexBestCost = Integer.MAX_VALUE;

                // Rozważamy każdą krawędź (a, b) w aktualnym cyklu.
                // Wstawienie vertexId między a i b:
                // - usuwa krawędź (a, b),
                // - dodaje krawędzie (a, vertexId) i (vertexId, b).
                for (int cycleIndex = 0; cycleIndex < cycle.size(); cycleIndex++) {
                    int a = cycle.get(cycleIndex);
                    int b = cycle.get((cycleIndex + 1) % cycle.size());

                    int increase = D[a][vertexId] + D[vertexId][b] - D[a][b];
                    int cost = increase - vertices[vertexId].profit;

                    if (cost < vertexBestCost) {
                        vertexBestCost = cost;

                        // Pozycja cycleIndex + 1 oznacza wstawienie między a i b.
                        vertexBestInsertionPosition = cycleIndex + 1;
                    }
                }

                // Spośród wszystkich kandydatów wybieramy tego,
                // którego najlepsza wstawka daje najmniejszy koszt.
                if (vertexBestCost < bestCost) {
                    bestCost = vertexBestCost;
                    bestVertex = vertexId;
                    bestVertexIndexInNotUsed = notUsedIndex;
                    bestInsertionPosition = vertexBestInsertionPosition;
                }
            }

            cycle.add(bestInsertionPosition, bestVertex);
            notUsed.remove(bestVertexIndexInNotUsed);
        }

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
