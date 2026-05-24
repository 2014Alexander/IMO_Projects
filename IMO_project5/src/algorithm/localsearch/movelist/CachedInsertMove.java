package algorithm.localsearch.movelist;

import algorithm.localsearch.IndexedCycle;

/**
 * Zapamiętany ruch wstawienia wierzchołka do cyklu.
 */
public final class CachedInsertMove implements CachedMove {
    private final int insertedVertex;
    private final int edgeA;
    private final int edgeB;
    private final int delta;

    /**
     * Tworzy ruch wstawienia wierzchołka między końce zapisanej krawędzi.
     *
     * @param insertedVertex wierzchołek wstawiany do cyklu
     * @param edgeA pierwszy koniec krawędzi wstawienia
     * @param edgeB drugi koniec krawędzi wstawienia
     * @param delta zmiana wartości funkcji celu dla ruchu
     */
    public CachedInsertMove(int insertedVertex, int edgeA, int edgeB, int delta) {
        this.insertedVertex = insertedVertex;
        this.edgeA = edgeA;
        this.edgeB = edgeB;
        this.delta = delta;
    }

    /**
     * Zwraca zapamiętaną zmianę wartości funkcji celu dla ruchu.
     *
     * @return delta ruchu
     */
    @Override
    public int delta() {
        return delta;
    }

    /**
     * Sprawdza, czy wierzchołek można wstawić w zapisaną krawędź bieżącego cyklu.
     *
     * @param cycle bieżący cykl z szybkim dostępem do pozycji wierzchołków
     * @return APPLICABLE, gdy wierzchołek jest poza cyklem, a zapisana krawędź występuje w cyklu;
     *         INVALID w pozostałych przypadkach
     */
    @Override
    public MoveApplicability applicability(IndexedCycle cycle) {
        if (!cycle.containsVertex(insertedVertex) && cycle.hasUndirectedEdge(edgeA, edgeB)) {
            return MoveApplicability.APPLICABLE;
        }
        return MoveApplicability.INVALID;
    }

    /**
     * Wstawia wierzchołek między końce zapisanej krawędzi.
     *
     * @param cycle bieżący cykl modyfikowany przez ruch
     */
    @Override
    public void apply(IndexedCycle cycle) {
        cycle.insertBetween(edgeA, edgeB, insertedVertex);
    }
}
