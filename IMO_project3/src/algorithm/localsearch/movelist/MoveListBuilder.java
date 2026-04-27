package algorithm.localsearch.movelist;

import algorithm.localsearch.IndexedCycle;
import model.Instance;
import model.Vertex;

import java.util.List;

/**
 * Buduje listę LM dla bieżącego cyklu w alternatywnej wersji lokalnego przeszukiwania.
 *
 * <p>Lista LM zawiera zapamiętane ruchy poprawiające wartość funkcji celu i jest
 * uporządkowana od najlepszego ruchu do najsłabszego ruchu poprawiającego.</p>
 */
public final class MoveListBuilder {
    private final int[][] distanceMatrix;
    private final int[] profits;
    private final int vertexCount;

    /**
     * Tworzy budowniczego listy LM dla podanej instancji problemu.
     *
     * @param instance instancja zawierająca macierz odległości i zyski wierzchołków
     */
    public MoveListBuilder(Instance instance) {
        this.distanceMatrix = instance.distanceMatrix.distances;
        this.vertexCount = instance.size;
        this.profits = new int[vertexCount];

        for (Vertex vertex : instance.vertices) {
            profits[vertex.id] = vertex.profit;
        }
    }

    /**
     * Wypełnia podaną listę wszystkimi aktualnymi ruchami poprawiającymi dla bieżącego cyklu.
     *
     * @param cycle    bieżący cykl, dla którego budowana jest lista LM
     * @param moveList lista LM wypełniana ruchami poprawiającymi
     */
    public void fillMoveList(IndexedCycle cycle, List<CachedMove> moveList) {
        moveList.clear();

        generateInsertMoves(cycle, moveList);
        generateDeleteMoves(cycle, moveList);
        generateSwapEdgesMoves(cycle, moveList);

        moveList.sort((first, second) -> Integer.compare(second.delta(), first.delta()));
    }

    /**
     * Dodaje do LM ruchy wstawienia niewybranych wierzchołków do krawędzi bieżącego cyklu.
     *
     * @param cycle    bieżący cykl
     * @param moveList lista LM
     */
    private void generateInsertMoves(IndexedCycle cycle, List<CachedMove> moveList) {
        int cycleSize = cycle.size();

        for (int insertedVertex = 0; insertedVertex < vertexCount; insertedVertex++) {
            if (!cycle.containsVertex(insertedVertex)) {
                for (int position = 0; position < cycleSize; position++) {
                    int edgeA = cycle.vertexAt(position);
                    int edgeB = cycle.vertexAt(cycle.nextIndex(position));

                    int delta = profits[insertedVertex]
                        - distanceMatrix[edgeA][insertedVertex]
                        - distanceMatrix[insertedVertex][edgeB]
                        + distanceMatrix[edgeA][edgeB];

                    if (delta > 0) {
                        moveList.add(new CachedInsertMove(insertedVertex, edgeA, edgeB, delta));
                    }
                }
            }
        }
    }

    /**
     * Dodaje do LM ruchy usunięcia wierzchołków z bieżącego cyklu.
     *
     * @param cycle    bieżący cykl
     * @param moveList lista LM
     */
    private void generateDeleteMoves(IndexedCycle cycle, List<CachedMove> moveList) {
        int cycleSize = cycle.size();

        if (cycleSize > 2) {
            for (int position = 0; position < cycleSize; position++) {
                int deletedVertex = cycle.vertexAt(position);
                int leftNeighbor = cycle.vertexAt(cycle.prevIndex(position));
                int rightNeighbor = cycle.vertexAt(cycle.nextIndex(position));

                int delta = -profits[deletedVertex]
                    - distanceMatrix[leftNeighbor][rightNeighbor]
                    + distanceMatrix[leftNeighbor][deletedVertex]
                    + distanceMatrix[deletedVertex][rightNeighbor];

                if (delta > 0) {
                    moveList.add(new CachedDeleteMove(
                        deletedVertex,
                        leftNeighbor,
                        rightNeighbor,
                        delta
                    ));
                }
            }
        }
    }

    /**
     * Dodaje do LM ruchy wymiany dwóch krawędzi w bieżącym cyklu.
     *
     * @param cycle    bieżący cykl
     * @param moveList lista LM
     */
    private void generateSwapEdgesMoves(IndexedCycle cycle, List<CachedMove> moveList) {
        int cycleSize = cycle.size();

        for (int firstPosition = 0; firstPosition < cycleSize - 1; firstPosition++) {
            for (int secondPosition = firstPosition + 1; secondPosition < cycleSize; secondPosition++) {
                if (!cycle.areAdjacentPositions(firstPosition, secondPosition)) {
                    int firstStart = cycle.vertexAt(firstPosition);
                    int firstEnd = cycle.vertexAt(cycle.nextIndex(firstPosition));
                    int secondStart = cycle.vertexAt(secondPosition);
                    int secondEnd = cycle.vertexAt(cycle.nextIndex(secondPosition));

                    int delta = distanceMatrix[firstStart][firstEnd]
                        + distanceMatrix[secondStart][secondEnd]
                        - distanceMatrix[firstStart][secondStart]
                        - distanceMatrix[firstEnd][secondEnd];

                    if (delta > 0) {
                        moveList.add(new CachedSwapEdgesMove(
                            firstStart,
                            firstEnd,
                            secondStart,
                            secondEnd,
                            delta
                        ));
                    }
                }
            }
        }
    }
}
