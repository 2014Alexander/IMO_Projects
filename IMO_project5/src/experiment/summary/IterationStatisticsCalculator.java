package experiment.summary;

import experiment.execution.ExecutionResult;

import java.util.List;

public final class IterationStatisticsCalculator {

    private IterationStatisticsCalculator() {
    }

    /**
     * Liczy statystyki iteracji dla wynikow jednego algorytmu.
     *
     * @param results wyniki jednego algorytmu z wypelnionym iterationCount
     * @return statystyki liczby iteracji
     */
    public static IterationStatistics calculate(List<ExecutionResult> results) {
        ExecutionResult firstResult = results.get(0);

        int minIterationCount = firstResult.iterationCount();
        int maxIterationCount = firstResult.iterationCount();
        long sumIterationCount = 0L;

        for (ExecutionResult result : results) {
            int iterationCount = result.iterationCount();

            if (iterationCount < minIterationCount) {
                minIterationCount = iterationCount;
            }

            if (iterationCount > maxIterationCount) {
                maxIterationCount = iterationCount;
            }

            sumIterationCount += iterationCount;
        }

        double avgIterationCount = (double) sumIterationCount / results.size();

        return new IterationStatistics(
                firstResult.solution().instanceName(),
                firstResult.algorithmName(),
                results.size(),
                minIterationCount,
                maxIterationCount,
                avgIterationCount
        );
    }
}
