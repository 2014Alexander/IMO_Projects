package experiment.build;

import algorithm.ConstructionAlgorithm;
import algorithm.improvement.PhaseTwoDelete;
import execute.ExecutionResult;
import experiment.run.ExperimentRunner;
import experiment.summary.TwoPhaseExperimentSummary;
import model.Instance;
import statistics.ObjectiveStatistics;
import statistics.ObjectiveStatisticsCalculator;
import statistics.TourLengthStatistics;
import statistics.TourLengthStatisticsCalculator;

import java.util.List;

public final class TwoPhaseExperimentSummaryBuilder {

    private final ExperimentRunner experimentRunner;

    public TwoPhaseExperimentSummaryBuilder() {
        this.experimentRunner = new ExperimentRunner();
    }

    public TwoPhaseExperimentSummary build(
            Instance instance,
            ConstructionAlgorithm phaseOneAlgorithm,
            PhaseTwoDelete phaseTwoDelete
    ) {
        if (phaseOneAlgorithm == null) {
            throw new IllegalArgumentException("phaseOneAlgorithm cannot be null");
        }

        return build(instance, phaseOneAlgorithm.name(), phaseOneAlgorithm, phaseTwoDelete);
    }

    public TwoPhaseExperimentSummary build(
            Instance instance,
            String algorithmName,
            ConstructionAlgorithm phaseOneAlgorithm,
            PhaseTwoDelete phaseTwoDelete
    ) {
        validateBuildArguments(instance, algorithmName, phaseOneAlgorithm, phaseTwoDelete);

        List<ExecutionResult> phaseOneResults = experimentRunner.runForAllVertices(
                instance,
                algorithmName,
                phaseOneAlgorithm
        );

        List<ExecutionResult> finalResults = experimentRunner.runForAllVertices(
                instance,
                algorithmName,
                phaseOneAlgorithm,
                phaseTwoDelete
        );

        TourLengthStatistics phaseOneTourLengthStatistics =
                TourLengthStatisticsCalculator.calculate(phaseOneResults);

        ObjectiveStatistics finalObjectiveStatistics =
                ObjectiveStatisticsCalculator.calculate(finalResults);

        ResultExtremes finalResultExtremes = findResultExtremes(finalResults);

        return new TwoPhaseExperimentSummary(
                instance.name,
                algorithmName,
                phaseOneTourLengthStatistics,
                finalObjectiveStatistics,
                finalResultExtremes.bestResult(),
                finalResultExtremes.worstResult()
        );
    }

    private static void validateBuildArguments(
            Instance instance,
            String algorithmName,
            ConstructionAlgorithm phaseOneAlgorithm,
            PhaseTwoDelete phaseTwoDelete
    ) {
        if (instance == null) {
            throw new IllegalArgumentException("instance cannot be null");
        }
        if (algorithmName == null) {
            throw new IllegalArgumentException("algorithmName cannot be null");
        }
        if (phaseOneAlgorithm == null) {
            throw new IllegalArgumentException("phaseOneAlgorithm cannot be null");
        }
        if (phaseTwoDelete == null) {
            throw new IllegalArgumentException("phaseTwoDelete cannot be null");
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