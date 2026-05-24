package experiment.summary;

/**
 * Statystyki liczby iteracji dla jednego algorytmu i jednej instancji.
 */
public record IterationStatistics(
        String instanceName,
        String algorithmName,
        int runsCount,
        int minIterationCount,
        int maxIterationCount,
        double avgIterationCount
) {
}
