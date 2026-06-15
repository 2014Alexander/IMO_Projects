package algorithm.metaheuristic.destroy;

import algorithm.localsearch.Cycle;
import model.Instance;

import java.util.Random;

/**
 * Operator Destroy oparty na logice drugiej fazy usuwania wierzchołków.
 *
 * <p>Operator usuwa z cyklu ustalony procent wierzchołków. W każdym kroku ocenia wszystkie
 * aktualnie obecne wierzchołki, tworzy RCL (Restricted Candidate List, ograniczoną listę
 * kandydatów) z najlepszych możliwych usunięć, a następnie losuje jeden element z tej listy.
 * Dzięki temu wybór jest jednocześnie heurystyczny i losowy.</p>
 *
 * <p>Wartość usunięcia wierzchołka jest liczona jako zmiana funkcji celu po jego usunięciu:
 * zysk z usunięcia dwóch dotychczasowych krawędzi minus koszt dodania nowej krawędzi
 * oraz minus utracony zysk usuwanego wierzchołka.</p>
 */
public final class PhaseTwoDeleteDestroyOperator implements DestroyOperator {
    private static final int DEFAULT_CANDIDATE_LIST_SIZE = 5;

    private final double destroyRatio;
    private final int candidateListSize;

    /**
     * Tworzy operator z domyślnym rozmiarem RCL (Restricted Candidate List,
     * ograniczonej listy kandydatów).
     *
     * @param destroyRatio część aktualnego cyklu przeznaczona do usunięcia
     */
    public PhaseTwoDeleteDestroyOperator(double destroyRatio) {
        this(destroyRatio, DEFAULT_CANDIDATE_LIST_SIZE);
    }

    /**
     * Tworzy operator z jawnie podanym rozmiarem RCL (Restricted Candidate List,
     * ograniczonej listy kandydatów).
     *
     * @param destroyRatio część aktualnego cyklu przeznaczona do usunięcia
     * @param candidateListSize liczba najlepszych kandydatów, spośród których wybieramy losowo
     */
    public PhaseTwoDeleteDestroyOperator(double destroyRatio, int candidateListSize) {
        this.destroyRatio = destroyRatio;
        this.candidateListSize = candidateListSize;
    }

    /**
     * Niszczy przekazany cykl przez usunięcie ustalonej liczby wierzchołków.
     *
     * <p>Liczba usuwanych wierzchołków jest wyznaczana raz na początku, na podstawie początkowego
     * rozmiaru cyklu. Po każdym usunięciu wartości kolejnych możliwych usunięć są liczone od nowa,
     * ponieważ zmieniają się sąsiedzi pozostałych wierzchołków.</p>
     */
    @Override
    public void destroy(Instance instance, Cycle cycle, Random random) {
        int verticesToRemove = (int) Math.round(cycle.size() * destroyRatio);
        verticesToRemove = Math.min(verticesToRemove, cycle.size() - 2);

        int[] bestPositions = new int[candidateListSize];
        int[] bestDeltas = new int[candidateListSize];

        for (int removed = 0; removed < verticesToRemove; removed++) {
            int position = randomPositionFromBestRemovals(
                instance,
                cycle,
                random,
                bestPositions,
                bestDeltas
            );
            cycle.removeAt(position);
        }
    }

    /**
     * Zwraca losową pozycję z RCL (Restricted Candidate List, ograniczonej listy kandydatów).
     *
     * <p>RCL zawiera pozycje o największej wartości {@code removalDelta}. Tablice przekazane jako
     * argumenty są używane ponownie w kolejnych krokach usuwania, żeby nie tworzyć nowych obiektów
     * w głównej pętli operatora.</p>
     */
    private int randomPositionFromBestRemovals(
        Instance instance,
        Cycle cycle,
        Random random,
        int[] bestPositions,
        int[] bestDeltas
    ) {
        int selectedCount = Math.min(candidateListSize, cycle.size());
        int filledCount = 0;

        for (int position = 0; position < cycle.size(); position++) {
            int currentDelta = removalDelta(instance, cycle, position);

            // Wypełniamy początkową część RCL przed porównywaniem z najgorszym kandydatem.
            if (filledCount < selectedCount) {
                bestPositions[filledCount] = position;
                bestDeltas[filledCount] = currentDelta;
                moveCandidateUp(bestPositions, bestDeltas, filledCount);
                filledCount++;
            } else if (currentDelta > bestDeltas[selectedCount - 1]) {
                // Po zapełnieniu RCL zastępujemy jej najgorszy element tylko lepszym kandydatem.
                bestPositions[selectedCount - 1] = position;
                bestDeltas[selectedCount - 1] = currentDelta;
                moveCandidateUp(bestPositions, bestDeltas, selectedCount - 1);
            }
        }

        return bestPositions[random.nextInt(selectedCount)];
    }

    /**
     * Przesuwa kandydata w górę RCL, aby tablica była uporządkowana malejąco po wartości delty.
     */
    private static void moveCandidateUp(int[] positions, int[] deltas, int index) {
        for (int current = index; current > 0 && deltas[current] > deltas[current - 1]; current--) {
            swap(positions, current, current - 1);
            swap(deltas, current, current - 1);
        }
    }

    private static void swap(int[] values, int firstIndex, int secondIndex) {
        int temporary = values[firstIndex];
        values[firstIndex] = values[secondIndex];
        values[secondIndex] = temporary;
    }

    /**
     * Liczy zmianę funkcji celu po usunięciu wierzchołka z danej pozycji cyklu.
     *
     * <p>Dodatnia wartość oznacza, że samo usunięcie wierzchołka poprawiłoby funkcję celu.
     * Ujemna wartość oznacza, że usunięcie pogarsza funkcję celu, ale w fazie Destroy takie
     * usunięcia nadal są dopuszczalne, ponieważ późniejszy Repair ma odbudować cykl inaczej.</p>
     */
    private static int removalDelta(Instance instance, Cycle cycle, int position) {
        int previous = cycle.cycle[cycle.prevIndex(position)];
        int vertex = cycle.cycle[position];
        int next = cycle.cycle[cycle.nextIndex(position)];

        int removedEdgeCost = instance.distanceMatrix.distances[previous][vertex]
            + instance.distanceMatrix.distances[vertex][next];
        int addedEdgeCost = instance.distanceMatrix.distances[previous][next];
        int lostProfit = instance.vertices[vertex].profit;

        return removedEdgeCost - addedEdgeCost - lostProfit;
    }
}
