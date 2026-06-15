package algorithm.localsearch.movelist;

import algorithm.localsearch.IndexedCycle;

/**
 * Zapamiętany ruch usunięcia wierzchołka z cyklu.
 */
public final class CachedDeleteMove implements CachedMove {
    private final int deletedVertex;
    private final int leftNeighbor;
    private final int rightNeighbor;
    private final int delta;

    /**
     * Tworzy ruch usunięcia wierzchołka z zapisanego otoczenia.
     *
     * @param deletedVertex wierzchołek usuwany z cyklu
     * @param leftNeighbor pierwszy zapamiętany sąsiad usuwanego wierzchołka
     * @param rightNeighbor drugi zapamiętany sąsiad usuwanego wierzchołka
     * @param delta zmiana wartości funkcji celu dla ruchu
     */
    public CachedDeleteMove(int deletedVertex, int leftNeighbor, int rightNeighbor, int delta) {
        this.deletedVertex = deletedVertex;
        this.leftNeighbor = leftNeighbor;
        this.rightNeighbor = rightNeighbor;
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
     * Sprawdza, czy wierzchołek znajduje się między zapisanymi sąsiadami.
     *
     * @param cycle bieżący cykl z szybkim dostępem do pozycji wierzchołków
     * @return APPLICABLE, gdy wierzchołek znajduje się między zapisanymi sąsiadami;
     *         INVALID w pozostałych przypadkach
     */
    @Override
    public MoveApplicability applicability(IndexedCycle cycle) {
        if (cycle.isBetween(leftNeighbor, deletedVertex, rightNeighbor)) {
            return MoveApplicability.APPLICABLE;
        }
        return MoveApplicability.INVALID;
    }

    /**
     * Usuwa zapisany wierzchołek z bieżącego cyklu.
     *
     * @param cycle bieżący cykl modyfikowany przez ruch
     */
    @Override
    public void apply(IndexedCycle cycle) {
        cycle.removeVertex(deletedVertex);
    }
}
