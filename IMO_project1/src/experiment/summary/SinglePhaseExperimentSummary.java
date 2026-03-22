package experiment.summary;

import execute.ExecutionResult;
import statistics.ObjectiveStatistics;

public record SinglePhaseExperimentSummary(
        String instanceName,
        String algorithmName,
        ObjectiveStatistics objectiveStatistics,
        ExecutionResult bestResult,
        ExecutionResult worstResult
) {
    public SinglePhaseExperimentSummary {
        validate(
                instanceName,
                algorithmName,
                objectiveStatistics,
                bestResult,
                worstResult
        );
    }

    private static void validate(
            String instanceName,
            String algorithmName,
            ObjectiveStatistics objectiveStatistics,
            ExecutionResult bestResult,
            ExecutionResult worstResult
    ) {
        if (instanceName == null) {
            throw new IllegalArgumentException("instanceName cannot be null");
        }
        if (algorithmName == null) {
            throw new IllegalArgumentException("algorithmName cannot be null");
        }
        if (objectiveStatistics == null) {
            throw new IllegalArgumentException("objectiveStatistics cannot be null");
        }
        if (bestResult == null) {
            throw new IllegalArgumentException("bestResult cannot be null");
        }
        if (worstResult == null) {
            throw new IllegalArgumentException("worstResult cannot be null");
        }

        validateStatisticsConsistency(instanceName, algorithmName, objectiveStatistics);
        validateResultConsistency(instanceName, algorithmName, bestResult, "bestResult");
        validateResultConsistency(instanceName, algorithmName, worstResult, "worstResult");
    }

    private static void validateStatisticsConsistency(
            String expectedInstanceName,
            String expectedAlgorithmName,
            ObjectiveStatistics objectiveStatistics
    ) {
        if (!expectedInstanceName.equals(objectiveStatistics.instanceName())) {
            throw new IllegalArgumentException(
                    "objectiveStatistics.instanceName must match summary instanceName"
            );
        }
        if (!expectedAlgorithmName.equals(objectiveStatistics.algorithmName())) {
            throw new IllegalArgumentException(
                    "objectiveStatistics.algorithmName must match summary algorithmName"
            );
        }
    }

    private static void validateResultConsistency(
            String expectedInstanceName,
            String expectedAlgorithmName,
            ExecutionResult result,
            String resultName
    ) {
        if (!expectedAlgorithmName.equals(result.algorithmName())) {
            throw new IllegalArgumentException(
                    resultName + ".algorithmName must match summary algorithmName"
            );
        }
        if (!expectedInstanceName.equals(result.solution().instanceName())) {
            throw new IllegalArgumentException(
                    resultName + ".solution.instanceName must match summary instanceName"
            );
        }
    }
}