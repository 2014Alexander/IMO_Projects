package algorithm.construction.singlephase;

import algorithm.ConstructionAlgorithm;
import model.Instance;
import model.Solution;

import java.util.ArrayList;
import java.util.List;

public class TwoRegretCost implements ConstructionAlgorithm {

    @Override
    public String name() {
        return "2RegretCost";
    }

    @Override
    public Solution solve(Instance instance, Integer startVertexId) {
        validateInputs(instance, startVertexId);

        int[][] D = instance.distanceMatrix.distances;
        int[] profit = extractProfits(instance);
        int n = instance.size;

        List<Integer> cycle = new ArrayList<>(n);
        List<Integer> notUsed = new ArrayList<>(n - 1);

        // Cykl zaczynamy od zadanego wierzchołka startowego.
        cycle.add(startVertexId);

        // notUsed zawiera wszystkie wierzchołki poza startowym.
        for (int vertexId = 0; vertexId < n; vertexId++) {
            if (vertexId != startVertexId) {
                notUsed.add(vertexId);
            }
        }

        // Drugi wierzchołek wybieramy jako najbliższy sąsiad startu.
        // Dzięki temu tworzymy początkowy cykl złożony z dwóch wierzchołków.
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

        cycle.add(secondVertex);
        notUsed.remove(secondVertexIndexInNotUsed);

        // W każdej iteracji wybieramy wierzchołek o największym żalu (regret),
        // a następnie wstawiamy go w jego najlepsze miejsce.
        while (!notUsed.isEmpty()) {
            int bestVertex = -1;
            int bestVertexIndexInNotUsed = -1;
            int bestRegret = Integer.MIN_VALUE;
            int bestPositionForBestVertex = -1;

            // Dla każdego jeszcze niewstawionego wierzchołka
            // obliczamy jego najlepszy i drugi najlepszy koszt wstawienia.
            for (int notUsedIndex = 0; notUsedIndex < notUsed.size(); notUsedIndex++) {
                int vertexId = notUsed.get(notUsedIndex);

                int bestCost = Integer.MAX_VALUE;
                int secondBestCost = Integer.MAX_VALUE;
                int bestPosition = -1;

                // Sprawdzamy wszystkie możliwe miejsca wstawienia vertexId do cyklu.
                // Każde miejsce odpowiada przecięciu jednej krawędzi (a, b)
                // i wstawieniu vertexId między a i b.
                for (int cycleIndex = 0; cycleIndex < cycle.size(); cycleIndex++) {
                    int a = cycle.get(cycleIndex);
                    int b = cycle.get((cycleIndex + 1) % cycle.size());

                    // O ile wzrośnie długość cyklu po wstawieniu vertexId między a i b.
                    int increaseLength = D[a][vertexId] + D[vertexId][b] - D[a][b];

                    // Koszt wstawienia łączy dwa efekty:
                    // - increaseLength: o ile wydłuży się cykl,
                    // - profit[vertexId]: jaki zysk daje nowy wierzchołek.
                    int cost = increaseLength - profit[vertexId];

                    // Aktualizujemy najlepszy i drugi najlepszy koszt dla tego vertexId.
                    if (cost < bestCost) {
                        secondBestCost = bestCost;
                        bestCost = cost;

                        // Pozycja cycleIndex + 1 oznacza wstawienie między a i b.
                        bestPosition = cycleIndex + 1;
                    } else if (cost < secondBestCost) {
                        secondBestCost = cost;
                    }
                }

                // Żal mówi, jak bardzo "boli" odłożenie wstawienia tego wierzchołka.
                // Im większa różnica między drugą a pierwszą najlepszą opcją,
                // tym bardziej opłaca się wstawić ten wierzchołek już teraz.
                int regret = secondBestCost - bestCost;

                // Spośród wszystkich kandydatów wybieramy tego,
                // który ma największy regret.
                if (regret > bestRegret) {
                    bestRegret = regret;
                    bestVertex = vertexId;
                    bestVertexIndexInNotUsed = notUsedIndex;
                    bestPositionForBestVertex = bestPosition;
                }
            }

            // Wstawiamy wybrany wierzchołek w jego najlepsze miejsce
            // i usuwamy go z listy niewykorzystanych.
            cycle.add(bestPositionForBestVertex, bestVertex);
            notUsed.remove(bestVertexIndexInNotUsed);
        }

        return new Solution(
                instance.name,
                startVertexId,
                cycle
        );
    }

    private static int[] extractProfits(Instance instance) {
        int[] profit = new int[instance.size];

        // Przepisujemy profity do osobnej tablicy,
        // żeby później odwoływać się do nich prosto przez profit[v].
        for (int i = 0; i < instance.size; i++) {
            profit[i] = instance.vertices[i].profit;
        }

        return profit;
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