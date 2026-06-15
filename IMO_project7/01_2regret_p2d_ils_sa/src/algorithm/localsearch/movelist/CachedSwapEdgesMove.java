package algorithm.localsearch.movelist;

import algorithm.localsearch.IndexedCycle;

/**
 * Zapamiętany ruch wymiany dwóch krawędzi w cyklu.
 */
public final class CachedSwapEdgesMove implements CachedMove {
    private final int firstStart;
    private final int firstEnd;
    private final int secondStart;
    private final int secondEnd;
    private final int delta;

    /**
     * Tworzy ruch wymiany dwóch zapisanych krawędzi.
     *
     * @param firstStart  początek pierwszej usuwanej krawędzi
     * @param firstEnd    koniec pierwszej usuwanej krawędzi
     * @param secondStart początek drugiej usuwanej krawędzi
     * @param secondEnd   koniec drugiej usuwanej krawędzi
     * @param delta       zmiana wartości funkcji celu dla ruchu
     */
    public CachedSwapEdgesMove(int firstStart, int firstEnd, int secondStart, int secondEnd, int delta) {
        this.firstStart = firstStart;
        this.firstEnd = firstEnd;
        this.secondStart = secondStart;
        this.secondEnd = secondEnd;
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
     * Sprawdza, czy zapisane krawędzie występują w tym samym względnym kierunku.
     *
     * @param cycle bieżący cykl z szybkim dostępem do pozycji wierzchołków
     * @return APPLICABLE, gdy zapisane krawędzie pasują do bieżącego cyklu;
     * INVALID w pozostałych przypadkach
     */
    @Override
    public MoveApplicability applicability(IndexedCycle cycle) {
        int direction = cycle.relativeDirectionOfEdges(firstStart, firstEnd, secondStart, secondEnd);
        if (direction == IndexedCycle.SAME_RELATIVE_DIRECTION) {
            return MoveApplicability.APPLICABLE;
        }
        return MoveApplicability.INVALID;
    }

    /**
     * Wykonuje wymianę dwóch krawędzi zgodnie z ich bieżącym kierunkiem w cyklu.
     *
     * @param cycle bieżący cykl modyfikowany przez ruch
     */
    @Override
    public void apply(IndexedCycle cycle) {
        if (cycle.hasDirectedEdge(firstStart, firstEnd)) {
            cycle.swapEdgesByVertices(firstStart, firstEnd, secondStart, secondEnd);
        } else {
            cycle.swapEdgesByVertices(firstEnd, firstStart, secondEnd, secondStart);
        }
    }
}
