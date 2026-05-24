package experiment.summary;

import java.util.List;

public record InstanceExperimentResult(
        String instanceName,
        List<ExperimentParameter> parameters,
        List<AlgorithmExperimentSummary> algorithmSummaries,
        List<AlgorithmExperimentSummary> referenceSummaries
) {
    public InstanceExperimentResult(
        String instanceName,
        List<ExperimentParameter> parameters,
        List<AlgorithmExperimentSummary> algorithmSummaries
    ) {
        this(instanceName, parameters, algorithmSummaries, List.of());
    }

    public InstanceExperimentResult {
        parameters = List.copyOf(parameters);
        algorithmSummaries = List.copyOf(algorithmSummaries);
        referenceSummaries = List.copyOf(referenceSummaries);
    }

    public List<AlgorithmExperimentSummary> allSummaries() {
        if (referenceSummaries.isEmpty()) {
            return algorithmSummaries;
        }

        java.util.ArrayList<AlgorithmExperimentSummary> all =
            new java.util.ArrayList<>(algorithmSummaries.size() + referenceSummaries.size());
        all.addAll(algorithmSummaries);
        all.addAll(referenceSummaries);
        return List.copyOf(all);
    }
}
