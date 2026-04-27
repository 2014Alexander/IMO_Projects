package algorithm.localsearch.candidate;

import algorithm.localsearch.Cycle;
import algorithm.localsearch.move.DeleteMove;
import algorithm.localsearch.move.InsertMove;
import algorithm.localsearch.move.Move;
import algorithm.localsearch.move.SwapEdgesMove;
import model.Vertex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Generuje sąsiedztwo kandydackie dla bieżącego cyklu.
 *
 * <p>Klasa używa krawędzi kandydackich wyznaczonych dla instancji i zwraca
 * zwykłe ruchy używane przez lokalne przeszukiwanie.</p>
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
     * Generuje ruchy kandydackie dla sąsiedztwa z wymianą krawędzi.
     *
     * @return lista ruchów kandydackich
     */
    public List<Move> neighborhoodSwapEdges() {
        rebuildCycleIndex();

        List<Move> moves = new ArrayList<>();

        generateInsertMoves(moves);
        generateDeleteMoves(moves);
        generateSwapEdgesMoves(moves);

        return moves;
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
     * @param moves lista uzupełniana wygenerowanymi ruchami
     */
    private void generateInsertMoves(List<Move> moves) {
        for (int insertPosition = 0; insertPosition < cycleSize; insertPosition++) {
            int firstVertex = cycle.cycle[insertPosition];
            int secondVertex = cycle.cycle[cycle.nextIndex(insertPosition)];
            int currentMarker = nextMarker();

            addInsertMovesForIncidentVertices(
                moves,
                insertPosition,
                candidateEdges.incidentVertices(firstVertex),
                currentMarker
            );
            addInsertMovesForIncidentVertices(
                moves,
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
     * @param moves lista uzupełniana wygenerowanymi ruchami
     * @param insertPosition pozycja, za którą wstawiany jest wierzchołek
     * @param incidentVertices wierzchołki połączone krawędzią kandydacką
     * @param marker aktualny znacznik pozycji
     */
    private void addInsertMovesForIncidentVertices(
        List<Move> moves,
        int insertPosition,
        int[] incidentVertices,
        int marker
    ) {
        for (int vertexId : incidentVertices) {
            if (!selected[vertexId] && insertMarkerByVertex[vertexId] != marker) {
                moves.add(new InsertMove(vertexId, insertPosition));
                insertMarkerByVertex[vertexId] = marker;
            }
        }
    }

    /**
     * Generuje kandydackie ruchy usunięcia wierzchołka z cyklu.
     *
     * @param moves lista uzupełniana wygenerowanymi ruchami
     */
    private void generateDeleteMoves(List<Move> moves) {
        if (cycleSize <= 2) {
            return;
        }

        for (int position = 0; position < cycleSize; position++) {
            int leftVertex = cycle.cycle[cycle.prevIndex(position)];
            int rightVertex = cycle.cycle[cycle.nextIndex(position)];

            if (candidateEdges.isCandidateEdge(leftVertex, rightVertex)) {
                moves.add(new DeleteMove(position));
            }
        }
    }

    /**
     * Generuje kandydackie ruchy wymiany dwóch krawędzi.
     *
     * @param moves lista uzupełniana wygenerowanymi ruchami
     */
    private void generateSwapEdgesMoves(List<Move> moves) {
        int swapMarker = nextMarker();

        for (int firstPosition = 0; firstPosition < cycleSize; firstPosition++) {
            int firstVertex = cycle.cycle[firstPosition];

            for (int secondVertex : candidateEdges.nearestVertices(firstVertex)) {
                int secondPosition = positionByVertex[secondVertex];

                if (secondPosition == -1) {
                    continue;
                }

                addSwapEdgesMove(moves, swapMarker, firstPosition, secondPosition);
                addSwapEdgesMove(
                    moves,
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
     * @param moves lista uzupełniana wygenerowanymi ruchami
     * @param marker znacznik bieżącej generacji ruchów
     * @param firstPosition pierwsza pozycja krawędzi
     * @param secondPosition druga pozycja krawędzi
     */
    private void addSwapEdgesMove(
        List<Move> moves,
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
            moves.add(new SwapEdgesMove(leftPosition, rightPosition));
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
