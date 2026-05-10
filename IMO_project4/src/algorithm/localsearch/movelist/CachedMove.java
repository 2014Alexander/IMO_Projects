package algorithm.localsearch.movelist;

import algorithm.localsearch.IndexedCycle;

/**
 * Wspólny interfejs dla zapamiętanych ruchów przechowywanych na liście LM.
 */
public interface CachedMove {
    /**
     * Zwraca zapamiętaną zmianę wartości funkcji celu dla ruchu.
     *
     * @return delta ruchu używana do sortowania LM
     */
    int delta();

    /**
     * Sprawdza, czy zapamiętany ruch pasuje do bieżącego cyklu.
     *
     * @param cycle bieżący cykl z szybkim dostępem do pozycji wierzchołków
     * @return wynik sprawdzenia aplikowalności ruchu
     */
    MoveApplicability applicability(IndexedCycle cycle);

    /**
     * Wykonuje zapamiętany ruch na bieżącym cyklu.
     *
     * @param cycle bieżący cykl modyfikowany przez ruch
     */
    void apply(IndexedCycle cycle);
}
