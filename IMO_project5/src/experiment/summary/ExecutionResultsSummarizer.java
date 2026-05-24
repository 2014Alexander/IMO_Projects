package experiment.summary;

import experiment.execution.ExecutionResult;

import java.util.List;

/**
 * Buduje podsumowanie z wyników pojedynczego algorytmu.
 */
public final class ExecutionResultsSummarizer {
    private ExecutionResultsSummarizer() {
    }

    /**
     * Buduje pełne podsumowanie z listy wyników jednego algorytmu.
     *
     * @param results wyniki pojedynczego algorytmu
     * @return podsumowanie wyników
     */
    public static AlgorithmExperimentSummary summarize(List<ExecutionResult> results) {
        ObjectiveStatistics objectiveStatistics =
            ObjectiveStatisticsCalculator.calculate(results);

        RuntimeStatistics runtimeStatistics =
            RuntimeStatisticsCalculator.calculate(results);

        IterationStatistics iterationStatistics = null;
        if (allResultsHaveIterationCount(results)) {
            iterationStatistics = IterationStatisticsCalculator.calculate(results);
        }

        ExecutionResult bestExecutionResult = findBestExecutionResult(results);

        return new AlgorithmExperimentSummary(
            objectiveStatistics.algorithmName(),
            objectiveStatistics,
            runtimeStatistics,
            iterationStatistics,
            bestExecutionResult
        );
    }

    private static boolean allResultsHaveIterationCount(List<ExecutionResult> results) {
        for (ExecutionResult result : results) {
            if (result.iterationCount() == null) {
                return false;
            }
        }

        return true;
    }

    private static ExecutionResult findBestExecutionResult(List<ExecutionResult> results) {
        ExecutionResult bestResult = results.get(0);
        int bestObjective = bestResult.solutionMetrics().objective();

        for (int i = 1; i < results.size(); i++) {
            ExecutionResult currentResult = results.get(i);
            int currentObjective = currentResult.solutionMetrics().objective();

            if (currentObjective > bestObjective) {
                bestObjective = currentObjective;
                bestResult = currentResult;
            }
        }

        return bestResult;
    }
}
