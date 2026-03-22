package algorithm.construction.twophase;

import algorithm.ConstructionAlgorithm;
import model.Instance;
import model.Solution;

import java.util.ArrayList;
import java.util.List;

public class GreedyCycleWithoutProfit implements ConstructionAlgorithm {

    @Override
    public String name() {
        return "GCa";
    }

    @Override
    public Solution solve(Instance instance, Integer startVertexId) {
        validateInputs(instance, startVertexId);

        int[][] D = instance.distanceMatrix.distances;
        int n = instance.size;

        List<Integer> cycle = new ArrayList<>(n);
        List<Integer> notUsed = new ArrayList<>(n - 1);

        // Cykl zaczynamy od wierzchołka startowego.
        cycle.add(startVertexId);

        // notUsed zawiera wszystkie wierzchołki poza startowym.
        for (int vertexId = 0; vertexId < n; vertexId++) {
            if (vertexId != startVertexId) {
                notUsed.add(vertexId);
            }
        }

        // Drugi wierzchołek wybieramy jako najbliższy sąsiad startu.
        int secondVertex = -1;
        int secondVertexIndexInNotUsed = -1;
        int smallestDistanceFromStart = Integer.MAX_VALUE;

        for (int notUsedIndex = 0; notUsedIndex < notUsed.size(); notUsedIndex++) {
            int vertexId = notUsed.get(notUsedIndex);
            int distanceFromStart = D[startVertexId][vertexId];

            if (distanceFromStart < smallestDistanceFromStart) {
                smallestDistanceFromStart = distanceFromStart;
                secondVertex = vertexId;
                secondVertexIndexInNotUsed = notUsedIndex;
            }
        }

        // Po tym kroku mamy początkowy cykl złożony z dwóch wierzchołków.
        cycle.add(secondVertex);
        notUsed.remove(secondVertexIndexInNotUsed);

        // W każdej iteracji wybieramy taki wierzchołek i takie miejsce wstawienia,
        // które powodują najmniejszy wzrost długości cyklu.
        while (!notUsed.isEmpty()) {
            int bestVertex = -1;
            int bestVertexIndexInNotUsed = -1;
            int bestInsertionPosition = -1;
            int bestIncrease = Integer.MAX_VALUE;

            // Dla każdego jeszcze niewstawionego wierzchołka
            // szukamy jego najlepszego miejsca wstawienia.
            for (int notUsedIndex = 0; notUsedIndex < notUsed.size(); notUsedIndex++) {
                int vertexId = notUsed.get(notUsedIndex);

                int vertexBestInsertionPosition = -1;
                int vertexBestIncrease = Integer.MAX_VALUE;

                // Rozważamy każdą krawędź (a, b) w aktualnym cyklu.
                // Wstawienie vertexId między a i b usuwa krawędź (a, b)
                // i dodaje dwie nowe: (a, vertexId) oraz (vertexId, b).
                for (int cycleIndex = 0; cycleIndex < cycle.size(); cycleIndex++) {
                    int a = cycle.get(cycleIndex);
                    int b = cycle.get((cycleIndex + 1) % cycle.size());

                    int increase = D[a][vertexId] + D[vertexId][b] - D[a][b];

                    if (increase < vertexBestIncrease) {
                        vertexBestIncrease = increase;

                        // Pozycja cycleIndex + 1 oznacza wstawienie między a i b.
                        vertexBestInsertionPosition = cycleIndex + 1;
                    }
                }

                // Spośród wszystkich kandydatów zapamiętujemy tego,
                // którego najlepsza wstawka daje najmniejszy wzrost długości cyklu.
                if (vertexBestIncrease < bestIncrease) {
                    bestIncrease = vertexBestIncrease;
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

    private static void validateInputs(Instance instance, Integer startVertexId) {
        if (instance == null) {
            throw new IllegalArgumentException("Instance nie może być null.");
        }
        if (startVertexId == null) {
            throw new IllegalArgumentException("startVertexId nie może być null.");
        }
        if (instance.size < 2) {
            throw new IllegalArgumentException("Instancja musi zawierać co najmniej 2 wierzchołki.");
        }
        if (startVertexId < 0 || startVertexId >= instance.size) {
            throw new IllegalArgumentException("startVertexId jest poza zakresem instancji.");
        }
    }
}