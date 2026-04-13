package experiment.summary;

import execute.ExecutionResult;
import statistics.ObjectiveStatistics;
import statistics.RuntimeStatistics;

public record AlgorithmExperimentSummary(
        String algorithmName,
        ObjectiveStatistics objectiveStatistics,
        RuntimeStatistics runtimeStatistics,
        ExecutionResult bestExecutionResult
) {
}
