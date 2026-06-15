package algorithm.improvement;

import algorithm.CycleImprover;
import algorithm.SolutionImprover;
import algorithm.localsearch.Cycle;
import model.Instance;
import model.Solution;
import model.Vertex;

/**
 * Faza usuwania wierzchołków, które pogarszają wartość funkcji celu.
 *
 * <p>W każdej iteracji wybierane jest najlepsze usunięcie, czyli takie,
 * które daje największą dodatnią poprawę funkcji celu. Faza kończy się,
 * gdy żadne pojedyncze usunięcie nie poprawia rozwiązania.</p>
 */
public class PhaseTwoDelete implements SolutionImprover, CycleImprover {

    /**
     * Adapter dla starszego kodu pracującego na obiekcie Solution.
     * Właściwa logika działa na Cycle, żeby uniknąć duplikacji implementacji.
     */
    @Override
    public Solution improve(Instance instance, Solution solution) {
        Cycle cycle = new Cycle(solution.cycle(), instance.size);
        improve(instance, cycle);

        return new Solution(
                solution.instanceName(),
                solution.startVertexId(),
                cycle.toList()
        );
    }

    /**
     * Usuwa z cyklu wierzchołki, których usunięcie poprawia funkcję celu.
     * Cykl jest modyfikowany w miejscu.
     */
    @Override
    public void improve(Instance instance, Cycle cycle) {
        int[][] distances = instance.distanceMatrix.distances;
        int[] profits = buildProfits(instance.vertices);

        while (cycle.size() > 2) {
            int bestImprovement = 0;
            int bestPosition = -1;

            int cycleSize = cycle.size();
            int[] cycleVertices = cycle.cycle;

            // Szukamy najlepszego pojedynczego usunięcia w aktualnym cyklu.
            for (int position = 0; position < cycleSize; position++) {
                int improvement = removalDelta(
                        distances,
                        profits,
                        cycleVertices,
                        cycleSize,
                        position
                );

                if (improvement > bestImprovement) {
                    bestImprovement = improvement;
                    bestPosition = position;
                }
            }

            if (bestPosition == -1) {
                return;
            }

            cycle.removeAt(bestPosition);
        }
    }

    /**
     * Kopiuje zyski do tablicy indeksowanej numerem wierzchołka.
     */
    private static int[] buildProfits(Vertex[] vertices) {
        int[] profits = new int[vertices.length];

        for (int vertex = 0; vertex < vertices.length; vertex++) {
            profits[vertex] = vertices[vertex].profit;
        }

        return profits;
    }

    /**
     * Zwraca zmianę funkcji celu po usunięciu wierzchołka z podanej pozycji.
     * Wartość dodatnia oznacza poprawę rozwiązania.
     */
    private static int removalDelta(
            int[][] distances,
            int[] profits,
            int[] cycleVertices,
            int cycleSize,
            int position
    ) {
        int vertex = cycleVertices[position];
        int previousPosition = position == 0 ? cycleSize - 1 : position - 1;
        int nextPosition = position + 1 == cycleSize ? 0 : position + 1;
        int previous = cycleVertices[previousPosition];
        int next = cycleVertices[nextPosition];

        int removedEdgeCost = distances[previous][vertex] + distances[vertex][next];
        int addedEdgeCost = distances[previous][next];
        int lostProfit = profits[vertex];

        return removedEdgeCost - addedEdgeCost - lostProfit;
    }
}
