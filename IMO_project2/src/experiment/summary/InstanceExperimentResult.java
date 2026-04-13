package experiment.summary;

import java.util.List;

public record InstanceExperimentResult(
        String instanceName,
        List<AlgorithmExperimentSummary> algorithmSummaries
) {
}
