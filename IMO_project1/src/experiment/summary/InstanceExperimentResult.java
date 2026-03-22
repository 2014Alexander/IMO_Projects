package experiment.summary;

import java.util.List;

public record InstanceExperimentResult(
        String instanceName,
        List<SinglePhaseExperimentSummary> singlePhaseSummaries,
        List<TwoPhaseExperimentSummary> twoPhaseSummaries
) {
    public InstanceExperimentResult {
        validate(instanceName, singlePhaseSummaries, twoPhaseSummaries);

        singlePhaseSummaries = List.copyOf(singlePhaseSummaries);
        twoPhaseSummaries = List.copyOf(twoPhaseSummaries);
    }

    private static void validate(
            String instanceName,
            List<SinglePhaseExperimentSummary> singlePhaseSummaries,
            List<TwoPhaseExperimentSummary> twoPhaseSummaries
    ) {
        if (instanceName == null) {
            throw new IllegalArgumentException("instanceName cannot be null");
        }
        if (singlePhaseSummaries == null) {
            throw new IllegalArgumentException("singlePhaseSummaries cannot be null");
        }
        if (twoPhaseSummaries == null) {
            throw new IllegalArgumentException("twoPhaseSummaries cannot be null");
        }

        validateSinglePhaseSummaries(instanceName, singlePhaseSummaries);
        validateTwoPhaseSummaries(instanceName, twoPhaseSummaries);
    }

    private static void validateSinglePhaseSummaries(
            String expectedInstanceName,
            List<SinglePhaseExperimentSummary> summaries
    ) {
        for (SinglePhaseExperimentSummary summary : summaries) {
            if (summary == null) {
                throw new IllegalArgumentException("singlePhaseSummaries cannot contain null");
            }
            if (!expectedInstanceName.equals(summary.instanceName())) {
                throw new IllegalArgumentException(
                        "all singlePhaseSummaries must have the same instanceName as InstanceExperimentResult"
                );
            }
        }
    }

    private static void validateTwoPhaseSummaries(
            String expectedInstanceName,
            List<TwoPhaseExperimentSummary> summaries
    ) {
        for (TwoPhaseExperimentSummary summary : summaries) {
            if (summary == null) {
                throw new IllegalArgumentException("twoPhaseSummaries cannot contain null");
            }
            if (!expectedInstanceName.equals(summary.instanceName())) {
                throw new IllegalArgumentException(
                        "all twoPhaseSummaries must have the same instanceName as InstanceExperimentResult"
                );
            }
        }
    }
}