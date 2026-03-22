package experiment.build;

import algorithm.ConstructionAlgorithm;
import execute.ExecutionResult;
import experiment.run.ExperimentRunner;
import experiment.summary.SinglePhaseExperimentSummary;
import model.Instance;
import statistics.ObjectiveStatistics;
import statistics.ObjectiveStatisticsCalculator;

import java.util.List;

public final class SinglePhaseExperimentSummaryBuilder {

    private final ExperimentRunner experimentRunner;

    public SinglePhaseExperimentSummaryBuilder() {
        this.experimentRunner = new ExperimentRunner();
    }

    public SinglePhaseExperimentSummary build(
            Instance instance,
            ConstructionAlgorithm algorithm
    ) {
        if (algorithm == null) {
            throw new IllegalArgumentException("algorithm cannot be null");
        }

        return build(instance, algorithm.name(), algorithm);
    }

    public SinglePhaseExperimentSummary build(
            Instance instance,
            String algorithmName,
            ConstructionAlgorithm algorithm
    ) {
        validateBuildArguments(instance, algorithmName, algorithm);

        List<ExecutionResult> results = experimentRunner.runForAllVertices(
                instance,
                algorithmName,
                algorithm
        );

        ObjectiveStatistics objectiveStatistics =
                ObjectiveStatisticsCalculator.calculate(results);

        ResultExtremes resultExtremes = findResultExtremes(results);

        return new SinglePhaseExperimentSummary(
                instance.name,
                algorithmName,
                objectiveStatistics,
                resultExtremes.bestResult(),
                resultExtremes.worstResult()
        );
    }

    private static void validateBuildArguments(
            Instance instance,
            String algorithmName,
            ConstructionAlgorithm algorithm
    ) {
        if (instance == null) {
            throw new IllegalArgumentException("instance cannot be null");
        }
        if (algorithmName == null) {
            throw new IllegalArgumentException("algorithmName cannot be null");
        }
        if (algorithm == null) {
            throw new IllegalArgumentException("algorithm cannot be null");
        }
    }

    private static ResultExtremes findResultExtremes(List<ExecutionResult> results) {
        if (results == null || results.isEmpty()) {
            throw new IllegalArgumentException("results cannot be null or empty");
        }

        ExecutionResult firstResult = results.get(0);
        ExecutionResult bestResult = firstResult;
        ExecutionResult worstResult = firstResult;

        int bestObjective = firstResult.solutionMetrics().objective();
        int worstObjective = firstResult.solutionMetrics().objective();

        for (int i = 1; i < results.size(); i++) {
            ExecutionResult currentResult = results.get(i);
            int currentObjective = currentResult.solutionMetrics().objective();

            if (currentObjective > bestObjective) {
                bestResult = currentResult;
                bestObjective = currentObjective;
            }

            if (currentObjective < worstObjective) {
                worstResult = currentResult;
                worstObjective = currentObjective;
            }
        }

        return new ResultExtremes(bestResult, worstResult);
    }

    private record ResultExtremes(
            ExecutionResult bestResult,
            ExecutionResult worstResult
    ) {
    }
}