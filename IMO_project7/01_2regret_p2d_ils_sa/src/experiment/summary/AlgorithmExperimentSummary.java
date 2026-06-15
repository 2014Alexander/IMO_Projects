package experiment.summary;

import experiment.execution.ExecutionResult;

public record AlgorithmExperimentSummary(
        String algorithmName,
        ObjectiveStatistics objectiveStatistics,
        RuntimeStatistics runtimeStatistics,
        IterationStatistics iterationStatistics,
        ExecutionResult bestExecutionResult
) {
}
