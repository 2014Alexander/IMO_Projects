package algorithm;

/**
 * Udostepnia liczbe iteracji wykonanych w ostatnim uruchomieniu algorytmu.
 */
public interface IterationCountingAlgorithm {

    /**
     * Zwraca liczbe iteracji wykonanych przez ostatnie wywolanie solve.
     *
     * @return liczba iteracji ostatniego uruchomienia
     */
    int lastIterationCount();
}
