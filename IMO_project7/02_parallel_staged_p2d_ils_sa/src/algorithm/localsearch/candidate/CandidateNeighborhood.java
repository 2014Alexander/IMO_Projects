package algorithm.localsearch.candidate;

import algorithm.localsearch.Cycle;
import algorithm.localsearch.move.Move;
import model.Vertex;

import java.util.Arrays;

/**
 * Generuje sąsiedztwo kandydackie dla bieżącego cyklu.
 *
 * <p>Klasa używa krawędzi kandydackich wyznaczonych dla instancji i znajduje
 * najlepszy ruch poprawiający dla bieżącego cyklu.</p>
 */
public final class CandidateNeighborhood {
    private final Cycle cycle;
    private final Vertex[] vertices;
    private final CandidateEdges candidateEdges;

    private int cycleSize;
    private final int[] positionByVertex;
    private final boolean[] selected;
    private final int[] insertMarkerByVertex;
    private final int[] addedSwapMoveMarker;
    private int marker;

    /**
     * Tworzy generator sąsiedztwa kandydackiego.
     *
     * @param cycle bieżący cykl
     * @param vertices wszystkie wierzchołki instancji
     * @param candidateEdges krawędzie kandydackie dla instancji
     */
    public CandidateNeighborhood(
        Cycle cycle,
        Vertex[] vertices,
        CandidateEdges candidateEdges
    ) {
        this.cycle = cycle;
        this.vertices = vertices;
        this.candidateEdges = candidateEdges;
        this.positionByVertex = new int[vertices.length];
        this.selected = new boolean[vertices.length];
        this.insertMarkerByVertex = new int[vertices.length];
        this.addedSwapMoveMarker = new int[vertices.length * vertices.length];
        this.marker = 1;
    }

    /**
     * Znajduje najlepszy ruch poprawiający dla sąsiedztwa z wymianą krawędzi.
     *
     * @param distanceMatrix macierz odległości
     * @param profit zyski wierzchołków
     * @return najlepszy ruch poprawiający albo null, jeżeli taki ruch nie istnieje
     */
    public Move bestMoveInSwapEdgesNeighborhood(int[][] distanceMatrix, int[] profit) {
        rebuildCycleIndex();

        BestCandidateMoveSelector selector =
            new BestCandidateMoveSelector(cycle, distanceMatrix, profit);

        generateInsertMoves(selector);
        generateDeleteMoves(selector);
        generateSwapEdgesMoves(selector);

        return selector.bestMove();
    }

    /**
     * Odtwarza indeks pozycji wierzchołków dla bieżącego cyklu.
     */
    private void rebuildCycleIndex() {
        cycleSize = cycle.size();
        Arrays.fill(positionByVertex, -1);
        Arrays.fill(selected, false);

        for (int position = 0; position < cycleSize; position++) {
            int vertexId = cycle.cycle[position];
            positionByVertex[vertexId] = position;
            selected[vertexId] = true;
        }
    }

    /**
     * Generuje kandydackie ruchy wstawienia wierzchołka do cyklu.
     *
     */
    private void generateInsertMoves(BestCandidateMoveSelector selector) {
        for (int insertPosition = 0; insertPosition < cycleSize; insertPosition++) {
            int firstVertex = cycle.cycle[insertPosition];
            int secondVertex = cycle.cycle[cycle.nextIndex(insertPosition)];
            int currentMarker = nextMarker();

            addInsertMovesForIncidentVertices(
                selector,
                insertPosition,
                candidateEdges.incidentVertices(firstVertex),
                currentMarker
            );
            addInsertMovesForIncidentVertices(
                selector,
                insertPosition,
                candidateEdges.incidentVertices(secondVertex),
                currentMarker
            );
        }
    }

    /**
     * Dodaje ruchy wstawienia wynikające z krawędzi kandydackich incydentnych
     * z jednym końcem aktualnej krawędzi cyklu.
     *
     * @param insertPosition pozycja, za którą wstawiany jest wierzchołek
     * @param incidentVertices wierzchołki połączone krawędzią kandydacką
     * @param marker aktualny znacznik pozycji
     */
    private void addInsertMovesForIncidentVertices(
        BestCandidateMoveSelector selector,
        int insertPosition,
        int[] incidentVertices,
        int marker
    ) {
        for (int vertexId : incidentVertices) {
            if (!selected[vertexId] && insertMarkerByVertex[vertexId] != marker) {
                selector.considerInsert(vertexId, insertPosition);
                insertMarkerByVertex[vertexId] = marker;
            }
        }
    }

    /**
     * Generuje kandydackie ruchy usunięcia wierzchołka z cyklu.
     *
     */
    private void generateDeleteMoves(BestCandidateMoveSelector selector) {
        if (cycleSize <= 2) {
            return;
        }

        for (int position = 0; position < cycleSize; position++) {
            int leftVertex = cycle.cycle[cycle.prevIndex(position)];
            int rightVertex = cycle.cycle[cycle.nextIndex(position)];

            if (candidateEdges.isCandidateEdge(leftVertex, rightVertex)) {
                selector.considerDelete(position);
            }
        }
    }

    /**
     * Generuje kandydackie ruchy wymiany dwóch krawędzi.
     *
     */
    private void generateSwapEdgesMoves(BestCandidateMoveSelector selector) {
        int swapMarker = nextMarker();

        for (int firstPosition = 0; firstPosition < cycleSize; firstPosition++) {
            int firstVertex = cycle.cycle[firstPosition];

            for (int secondVertex : candidateEdges.nearestVertices(firstVertex)) {
                int secondPosition = positionByVertex[secondVertex];

                if (secondPosition == -1) {
                    continue;
                }

                addSwapEdgesMove(selector, swapMarker, firstPosition, secondPosition);
                addSwapEdgesMove(
                    selector,
                    swapMarker,
                    cycle.prevIndex(firstPosition),
                    cycle.prevIndex(secondPosition)
                );
            }
        }
    }

    /**
     * Dodaje pojedynczy ruch wymiany dwóch krawędzi, jeżeli nie został jeszcze
     * dodany dla tej pary pozycji.
     *
     * @param marker znacznik bieżącej generacji ruchów
     * @param firstPosition pierwsza pozycja krawędzi
     * @param secondPosition druga pozycja krawędzi
     */
    private void addSwapEdgesMove(
        BestCandidateMoveSelector selector,
        int marker,
        int firstPosition,
        int secondPosition
    ) {
        int leftPosition = Math.min(firstPosition, secondPosition);
        int rightPosition = Math.max(firstPosition, secondPosition);

        if (cycle.areAdjacentPositions(leftPosition, rightPosition)) {
            return;
        }

        int markerIndex = leftPosition * vertices.length + rightPosition;
        if (addedSwapMoveMarker[markerIndex] != marker) {
            selector.considerSwapEdges(leftPosition, rightPosition);
            addedSwapMoveMarker[markerIndex] = marker;
        }
    }

    /**
     * Zwraca kolejny znacznik bez czyszczenia tablic pomocniczych.
     *
     * @return nowy znacznik generacji
     */
    private int nextMarker() {
        if (marker == Integer.MAX_VALUE) {
            Arrays.fill(insertMarkerByVertex, 0);
            Arrays.fill(addedSwapMoveMarker, 0);
            marker = 1;
        }

        int result = marker;
        marker++;
        return result;
    }
}
