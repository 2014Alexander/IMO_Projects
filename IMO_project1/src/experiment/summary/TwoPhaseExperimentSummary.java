package experiment.summary;

import execute.ExecutionResult;
import statistics.ObjectiveStatistics;
import statistics.TourLengthStatistics;

public record TwoPhaseExperimentSummary(
        String instanceName,
        String algorithmName,
        TourLengthStatistics phaseOneTourLengthStatistics,
        ObjectiveStatistics finalObjectiveStatistics,
        ExecutionResult bestFinalResult,
        ExecutionResult worstFinalResult
) {
    public TwoPhaseExperimentSummary {
        validate(
                instanceName,
                algorithmName,
                phaseOneTourLengthStatistics,
                finalObjectiveStatistics,
                bestFinalResult,
                worstFinalResult
        );
    }

    private static void validate(
            String instanceName,
            String algorithmName,
            TourLengthStatistics phaseOneTourLengthStatistics,
            ObjectiveStatistics finalObjectiveStatistics,
            ExecutionResult bestFinalResult,
            ExecutionResult worstFinalResult
    ) {
        if (instanceName == null) {
            throw new IllegalArgumentException("instanceName cannot be null");
        }
        if (algorithmName == null) {
            throw new IllegalArgumentException("algorithmName cannot be null");
        }
        if (phaseOneTourLengthStatistics == null) {
            throw new IllegalArgumentException("phaseOneTourLengthStatistics cannot be null");
        }
        if (finalObjectiveStatistics == null) {
            throw new IllegalArgumentException("finalObjectiveStatistics cannot be null");
        }
        if (bestFinalResult == null) {
            throw new IllegalArgumentException("bestFinalResult cannot be null");
        }
        if (worstFinalResult == null) {
            throw new IllegalArgumentException("worstFinalResult cannot be null");
        }

        validatePhaseOneStatisticsConsistency(
                instanceName,
                algorithmName,
                phaseOneTourLengthStatistics
        );
        validateFinalStatisticsConsistency(
                instanceName,
                algorithmName,
                finalObjectiveStatistics
        );
        validateFinalResultConsistency(
                instanceName,
                algorithmName,
                bestFinalResult,
                "bestFinalResult"
        );
        validateFinalResultConsistency(
                instanceName,
                algorithmName,
                worstFinalResult,
                "worstFinalResult"
        );
    }

    private static void validatePhaseOneStatisticsConsistency(
            String expectedInstanceName,
            String expectedAlgorithmName,
            TourLengthStatistics phaseOneTourLengthStatistics
    ) {
        if (!expectedInstanceName.equals(phaseOneTourLengthStatistics.instanceName())) {
            throw new IllegalArgumentException(
                    "phaseOneTourLengthStatistics.instanceName must match summary instanceName"
            );
        }
        if (!expectedAlgorithmName.equals(phaseOneTourLengthStatistics.algorithmName())) {
            throw new IllegalArgumentException(
                    "phaseOneTourLengthStatistics.algorithmName must match summary algorithmName"
            );
        }
    }

    private static void validateFinalStatisticsConsistency(
            String expectedInstanceName,
            String expectedAlgorithmName,
            ObjectiveStatistics finalObjectiveStatistics
    ) {
        if (!expectedInstanceName.equals(finalObjectiveStatistics.instanceName())) {
            throw new IllegalArgumentException(
                    "finalObjectiveStatistics.instanceName must match summary instanceName"
            );
        }
        if (!expectedAlgorithmName.equals(finalObjectiveStatistics.algorithmName())) {
            throw new IllegalArgumentException(
                    "finalObjectiveStatistics.algorithmName must match summary algorithmName"
            );
        }
    }

    private static void validateFinalResultConsistency(
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