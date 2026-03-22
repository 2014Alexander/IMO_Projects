package statistics;

import execute.ExecutionResult;

import java.util.List;

public final class TourLengthStatisticsCalculator {

    private TourLengthStatisticsCalculator() {
    }

    public static TourLengthStatistics calculate(List<ExecutionResult> results) {
        validateResultsList(results);

        ExecutionResult firstResult = results.get(0);
        validateSingleResult(firstResult);

        String instanceName = firstResult.solution().instanceName();
        String algorithmName = firstResult.algorithmName();

        int runsCount = results.size();

        int firstTourLength = firstResult.solutionMetrics().tourLength();

        int minTourLength = firstTourLength;
        int maxTourLength = firstTourLength;
        long tourLengthSum = firstTourLength;

        for (int i = 1; i < results.size(); i++) {
            ExecutionResult result = results.get(i);
            validateSingleResult(result);
            validateConsistency(result, instanceName, algorithmName);

            int tourLength = result.solutionMetrics().tourLength();

            if (tourLength < minTourLength) {
                minTourLength = tourLength;
            }
            if (tourLength > maxTourLength) {
                maxTourLength = tourLength;
            }

            tourLengthSum += tourLength;
        }

        double avgTourLength = (double) tourLengthSum / runsCount;

        return new TourLengthStatistics(
                instanceName,
                algorithmName,
                runsCount,
                minTourLength,
                maxTourLength,
                avgTourLength
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