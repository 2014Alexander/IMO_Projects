package statistics;

import execute.ExecutionResult;

import java.util.List;

public final class ObjectiveStatisticsCalculator {

    private ObjectiveStatisticsCalculator() {
    }

    public static ObjectiveStatistics calculate(List<ExecutionResult> results) {
        validateResultsList(results);

        ExecutionResult firstResult = results.get(0);
        validateSingleResult(firstResult);

        String instanceName = firstResult.solution().instanceName();
        String algorithmName = firstResult.algorithmName();

        int runsCount = results.size();

        int firstObjective = firstResult.solutionMetrics().objective();

        int minObjective = firstObjective;
        int maxObjective = firstObjective;
        long objectiveSum = firstObjective;

        for (int i = 1; i < results.size(); i++) {
            ExecutionResult result = results.get(i);
            validateSingleResult(result);
            validateConsistency(result, instanceName, algorithmName);

            int objective = result.solutionMetrics().objective();

            if (objective < minObjective) {
                minObjective = objective;
            }
            if (objective > maxObjective) {
                maxObjective = objective;
            }

            objectiveSum += objective;
        }

        double avgObjective = (double) objectiveSum / runsCount;

        return new ObjectiveStatistics(
                instanceName,
                algorithmName,
                runsCount,
                minObjective,
                maxObjective,
                avgObjective
        );
    }

    private static void validateResultsList(List<ExecutionResult> results) {
        if (results == null) {
            throw new IllegalArgumentException("results cannot be null");
        }
        if (results.isEmpty()) {
            throw new IllegalArgumentException("results cannot be empty");
        }
    }

    private static void validateSingleResult(ExecutionResult result) {
        if (result == null) {
            throw new IllegalArgumentException("results cannot contain null");
        }
        if (result.solution() == null) {
            throw new IllegalArgumentException("result.solution cannot be null");
        }
        if (result.solutionMetrics() == null) {
            throw new IllegalArgumentException("result.solutionMetrics cannot be null");
        }
        if (result.algorithmName() == null) {
            throw new IllegalArgumentException("result.algorithmName cannot be null");
        }
        if (result.solution().instanceName() == null) {
            throw new IllegalArgumentException("result.solution.instanceName cannot be null");
        }
    }

    private static void validateConsistency(
            ExecutionResult result,
            String expectedInstanceName,
            String expectedAlgorithmName
    ) {
        String actualInstanceName = result.solution().instanceName();
        String actualAlgorithmName = result.algorithmName();

        if (!expectedInstanceName.equals(actualInstanceName)) {
            throw new IllegalArgumentException("all results must have the same instanceName");
        }
        if (!expectedAlgorithmName.equals(actualAlgorithmName)) {
            throw new IllegalArgumentException("all results must have the same algorithmName");
        }
    }
}