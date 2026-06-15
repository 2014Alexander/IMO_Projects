package algorithm.localsearch.movelist;

/**
 * Wynik sprawdzenia, czy zapamiętany ruch z listy LM nadal pasuje do bieżącego cyklu.
 */
public enum MoveApplicability {
    /**
     * Ruch można zastosować do bieżącego cyklu.
     */
    APPLICABLE,

    /**
     * Ruch nie pasuje już do bieżącego cyklu i powinien zostać usunięty z LM.
     */
    INVALID
}
