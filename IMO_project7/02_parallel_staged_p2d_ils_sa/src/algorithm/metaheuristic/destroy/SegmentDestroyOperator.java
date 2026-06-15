package algorithm.metaheuristic.destroy;

import algorithm.localsearch.Cycle;
import model.Instance;

import java.util.Random;

/**
 * Operator Destroy usuwający kilka spójnych fragmentów cyklu.
 *
 * <p>Operator usuwa z cyklu ustalony procent wierzchołków, ale nie robi tego pojedynczymi
 * wierzchołkami. Liczba usuwanych wierzchołków jest dzielona na kilka segmentów, czyli
 * podścieżek złożonych z kolejnych wierzchołków w aktualnym cyklu.</p>
 *
 * <p>Początek każdego segmentu jest wybierany heurystyczno-losowo. Najpierw oceniane są wszystkie
 * możliwe pozycje startowe segmentu, następnie tworzona jest RCL (Restricted Candidate List,
 * ograniczona lista kandydatów) z najlepszych segmentów, a ostateczny segment do usunięcia jest
 * losowany z tej listy.</p>
 */
public final class SegmentDestroyOperator implements DestroyOperator {
    public static final double DEFAULT_DESTROY_RATIO = 0.40;
    public static final int DEFAULT_SEGMENTS_COUNT = 5;
    public static final int DEFAULT_CANDIDATE_LIST_SIZE = 15;

    private final double destroyRatio;
    private final int segmentsCount;
    private final int candidateListSize;


    /**
     * Tworzy operator z domyślnymi parametrami dobranymi eksperymentalnie.
     */
    public SegmentDestroyOperator() {
        this(DEFAULT_DESTROY_RATIO, DEFAULT_SEGMENTS_COUNT, DEFAULT_CANDIDATE_LIST_SIZE);
    }

    /**
     * Tworzy operator z domyślnym rozmiarem RCL (Restricted Candidate List,
     * ograniczonej listy kandydatów).
     *
     * @param destroyRatio część aktualnego cyklu przeznaczona do usunięcia
     * @param segmentsCount liczba segmentów, na które dzielimy usuwane wierzchołki
     */
    public SegmentDestroyOperator(double destroyRatio, int segmentsCount) {
        this(destroyRatio, segmentsCount, DEFAULT_CANDIDATE_LIST_SIZE);
    }

    /**
     * Tworzy operator z jawnie podanym rozmiarem RCL (Restricted Candidate List,
     * ograniczonej listy kandydatów).
     *
     * @param destroyRatio część aktualnego cyklu przeznaczona do usunięcia
     * @param segmentsCount liczba segmentów, na które dzielimy usuwane wierzchołki
     * @param candidateListSize liczba najlepszych segmentów, spośród których wybieramy losowo
     */
    public SegmentDestroyOperator(double destroyRatio, int segmentsCount, int candidateListSize) {
        this.destroyRatio = destroyRatio;
        this.segmentsCount = segmentsCount;
        this.candidateListSize = candidateListSize;
    }

    /**
     * Niszczy cykl przez usunięcie kilku podścieżek.
     *
     * <p>Łączna liczba usuwanych wierzchołków jest liczona raz na początku na podstawie
     * początkowego rozmiaru cyklu. Następnie ta liczba jest rozdzielana między kolejne segmenty.
     * Po każdym usunięciu następny segment jest oceniany już w zmienionym cyklu.</p>
     */
    @Override
    public void destroy(Instance instance, Cycle cycle, Random random) {
        int verticesToRemove = (int) Math.round(cycle.size() * destroyRatio);
        verticesToRemove = Math.min(verticesToRemove, cycle.size() - 2);

        int remainingToRemove = verticesToRemove;
        int remainingSegments = Math.min(segmentsCount, verticesToRemove);

        int[] bestStarts = new int[candidateListSize];
        int[] bestDeltas = new int[candidateListSize];

        while (remainingSegments > 0) {
            int segmentLength = (remainingToRemove + remainingSegments - 1) / remainingSegments;
            int startPosition = randomStartFromBestSegments(
                instance,
                cycle,
                random,
                segmentLength,
                bestStarts,
                bestDeltas
            );

            removeSegment(cycle, startPosition, segmentLength);

            remainingToRemove -= segmentLength;
            remainingSegments--;
        }
    }

    /**
     * Wybiera losowy początek segmentu z RCL (Restricted Candidate List,
     * ograniczonej listy kandydatów) najlepszych segmentów.
     */
    private int randomStartFromBestSegments(
        Instance instance,
        Cycle cycle,
        Random random,
        int segmentLength,
        int[] bestStarts,
        int[] bestDeltas
    ) {
        int selectedCount = Math.min(candidateListSize, cycle.size());
        int filledCount = 0;

        for (int startPosition = 0; startPosition < cycle.size(); startPosition++) {
            int currentDelta = segmentRemovalDelta(instance, cycle, startPosition, segmentLength);

            // Najpierw wypełniamy RCL, a potem utrzymujemy w niej najlepsze segmenty.
            if (filledCount < selectedCount) {
                bestStarts[filledCount] = startPosition;
                bestDeltas[filledCount] = currentDelta;
                moveCandidateUp(bestStarts, bestDeltas, filledCount);
                filledCount++;
            } else if (currentDelta > bestDeltas[selectedCount - 1]) {
                bestStarts[selectedCount - 1] = startPosition;
                bestDeltas[selectedCount - 1] = currentDelta;
                moveCandidateUp(bestStarts, bestDeltas, selectedCount - 1);
            }
        }

        return bestStarts[random.nextInt(selectedCount)];
    }

    /**
     * Liczy zmianę funkcji celu po usunięciu segmentu zaczynającego się w podanej pozycji.
     */
    private static int segmentRemovalDelta(
        Instance instance,
        Cycle cycle,
        int startPosition,
        int segmentLength
    ) {
        int cycleSize = cycle.size();
        int[] cycleVertices = cycle.cycle;
        int[][] distances = instance.distanceMatrix.distances;

        int previousPosition = startPosition == 0 ? cycleSize - 1 : startPosition - 1;
        int previous = cycleVertices[previousPosition];

        int removedEdgeCost = 0;
        int lostProfit = 0;
        int from = previous;
        int currentPosition = startPosition;

        for (int removed = 0; removed < segmentLength; removed++) {
            int vertex = cycleVertices[currentPosition];
            removedEdgeCost += distances[from][vertex];
            lostProfit += instance.vertices[vertex].profit;

            from = vertex;
            currentPosition++;
            if (currentPosition == cycleSize) {
                currentPosition = 0;
            }
        }

        int next = cycleVertices[currentPosition];
        removedEdgeCost += distances[from][next];

        int addedEdgeCost = distances[previous][next];

        return removedEdgeCost - addedEdgeCost - lostProfit;
    }

    /**
     * Usuwa kolejne wierzchołki segmentu, zachowując cykliczne przejście przez koniec tablicy.
     */
    private static void removeSegment(Cycle cycle, int startPosition, int segmentLength) {
        int currentPosition = startPosition;

        for (int removed = 0; removed < segmentLength; removed++) {
            if (currentPosition == cycle.size()) {
                currentPosition = 0;
            }

            cycle.removeAt(currentPosition);
        }
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
}
