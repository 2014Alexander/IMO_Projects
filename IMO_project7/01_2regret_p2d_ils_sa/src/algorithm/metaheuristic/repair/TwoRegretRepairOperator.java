package algorithm.metaheuristic.repair;

import algorithm.CycleImprover;
import algorithm.improvement.PhaseTwoDelete;
import algorithm.localsearch.Cycle;
import model.Instance;
import model.Vertex;

import java.util.Random;

/**
 * Operator naprawy rozwiązania oparty na metodzie 2-regret.
 *
 * <p>Operator startuje z częściowego cyklu pozostawionego przez Destroy.
 * Następnie wstawia wszystkie nieobecne wierzchołki metodą 2-regret
 * i uruchamia końcową fazę usuwania nieopłacalnych wierzchołków.</p>
 */
public final class TwoRegretRepairOperator implements RepairOperator {
    private static final int SELECTED_NOT_USED_INDEX = 0;
    private static final int SELECTED_INSERT_AFTER_POSITION = 1;

    private final CycleImprover finalImprover;

    public TwoRegretRepairOperator() {
        this(new PhaseTwoDelete());
    }

    public TwoRegretRepairOperator(CycleImprover finalImprover) {
        this.finalImprover = finalImprover;
    }

    @Override
    public void repair(Instance instance, Cycle cycle, Random random) {
        int[] notUsed = buildNotUsedVertices(instance, cycle);
        int notUsedSize = notUsed.length;

        int[][] distances = instance.distanceMatrix.distances;
        int[] profits = buildProfits(instance.vertices);
        int[] selectedInsertion = new int[2];

        while (notUsedSize > 0) {
            findBestTwoRegretInsertion(
                    distances,
                    profits,
                    cycle,
                    notUsed,
                    notUsedSize,
                    selectedInsertion
            );

            int selectedNotUsedIndex = selectedInsertion[SELECTED_NOT_USED_INDEX];
            int insertAfterPosition = selectedInsertion[SELECTED_INSERT_AFTER_POSITION];
            int vertex = notUsed[selectedNotUsedIndex];

            cycle.insertAfter(insertAfterPosition, vertex);

            // Kolejność w tablicy notUsed nie ma znaczenia, więc usuwamy w O(1).
            notUsed[selectedNotUsedIndex] = notUsed[notUsedSize - 1];
            notUsedSize--;
        }

        finalImprover.improve(instance, cycle);
    }

    /**
     * Buduje listę wierzchołków, których aktualnie nie ma w cyklu.
     */
    private static int[] buildNotUsedVertices(Instance instance, Cycle cycle) {
        boolean[] used = new boolean[instance.size];

        for (int position = 0; position < cycle.size(); position++) {
            used[cycle.cycle[position]] = true;
        }

        int[] notUsed = new int[instance.size - cycle.size()];
        int notUsedSize = 0;

        for (int vertex = 0; vertex < instance.size; vertex++) {
            if (!used[vertex]) {
                notUsed[notUsedSize] = vertex;
                notUsedSize++;
            }
        }

        return notUsed;
    }

    /**
     * Kopiuje zyski do tablicy indeksowanej numerem wierzchołka.
     * W fazie 2-regret pozwala to unikać wielokrotnego przechodzenia przez obiekty Vertex.
     */
    private static int[] buildProfits(Vertex[] vertices) {
        int[] profits = new int[vertices.length];

        for (int vertex = 0; vertex < vertices.length; vertex++) {
            profits[vertex] = vertices[vertex].profit;
        }

        return profits;
    }

    /**
     * Wybiera wierzchołek o największym żalu i najlepsze miejsce jego wstawienia.
     */
    private static void findBestTwoRegretInsertion(
            int[][] distances,
            int[] profits,
            Cycle cycle,
            int[] notUsed,
            int notUsedSize,
            int[] selectedInsertion
    ) {
        int selectedNotUsedIndex = -1;
        int selectedInsertAfterPosition = -1;
        int selectedRegret = Integer.MIN_VALUE;
        int selectedBestCost = Integer.MAX_VALUE;

        int cycleSize = cycle.size();
        int[] cycleVertices = cycle.cycle;

        for (int notUsedIndex = 0; notUsedIndex < notUsedSize; notUsedIndex++) {
            int vertex = notUsed[notUsedIndex];
            int vertexProfit = profits[vertex];

            int bestCost = Integer.MAX_VALUE;
            int secondBestCost = Integer.MAX_VALUE;
            int bestInsertAfterPosition = -1;

            // Dla danego wierzchołka szukamy dwóch najlepszych miejsc wstawienia.
            for (int position = 0; position < cycleSize; position++) {
                int firstVertex = cycleVertices[position];
                int nextPosition = position + 1 == cycleSize ? 0 : position + 1;
                int secondVertex = cycleVertices[nextPosition];

                int increaseLength = distances[firstVertex][vertex]
                        + distances[vertex][secondVertex]
                        - distances[firstVertex][secondVertex];
                int cost = increaseLength - vertexProfit;

                if (cost < bestCost) {
                    secondBestCost = bestCost;
                    bestCost = cost;
                    bestInsertAfterPosition = position;
                } else if (cost < secondBestCost) {
                    secondBestCost = cost;
                }
            }

            int regret = secondBestCost - bestCost;

            if (regret > selectedRegret
                    || (regret == selectedRegret && bestCost < selectedBestCost)) {
                selectedNotUsedIndex = notUsedIndex;
                selectedInsertAfterPosition = bestInsertAfterPosition;
                selectedRegret = regret;
                selectedBestCost = bestCost;
            }
        }

        selectedInsertion[SELECTED_NOT_USED_INDEX] = selectedNotUsedIndex;
        selectedInsertion[SELECTED_INSERT_AFTER_POSITION] = selectedInsertAfterPosition;
    }
}
