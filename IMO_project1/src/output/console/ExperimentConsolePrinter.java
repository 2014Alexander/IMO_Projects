package output.console;

import experiment.summary.InstanceExperimentResult;
import experiment.summary.SinglePhaseExperimentSummary;
import experiment.summary.TwoPhaseExperimentSummary;
import statistics.ObjectiveStatistics;
import statistics.TourLengthStatistics;

import java.util.List;

public final class ExperimentConsolePrinter {

    public void print(List<InstanceExperimentResult> instanceResults) {
        validateInstanceResults(instanceResults);

        for (int i = 0; i < instanceResults.size(); i++) {
            print(instanceResults.get(i));

            if (i < instanceResults.size() - 1) {
                System.out.println();
            }
        }
    }

    public void print(InstanceExperimentResult instanceResult) {
        if (instanceResult == null) {
            throw new IllegalArgumentException("instanceResult cannot be null");
        }

        printHeader(instanceResult.instanceName());
        printSinglePhaseSummaries(instanceResult.singlePhaseSummaries());
        printTwoPhaseSummaries(instanceResult.twoPhaseSummaries());
    }

    private void printHeader(String instanceName) {
        System.out.println("==================================================");
        System.out.println("Instance: " + instanceName);
        System.out.println("==================================================");
        System.out.println();
    }

    private void printSinglePhaseSummaries(List<SinglePhaseExperimentSummary> summaries) {
        System.out.println("Single-phase algorithms:");

        for (SinglePhaseExperimentSummary summary : summaries) {
            ObjectiveStatistics statistics = summary.objectiveStatistics();

            System.out.printf(
                    "  %-24s objective avg=%.2f min=%d max=%d | best=%d worst=%d%n",
                    summary.algorithmName(),
                    statistics.avgObjective(),
                    statistics.minObjective(),
                    statistics.maxObjective(),
                    summary.bestResult().solutionMetrics().objective(),
                    summary.worstResult().solutionMetrics().objective()
            );
        }

        System.out.println();
    }

    private void printTwoPhaseSummaries(List<TwoPhaseExperimentSummary> summaries) {
        System.out.println("Two-phase algorithms:");

        for (TwoPhaseExperimentSummary summary : summaries) {
            TourLengthStatistics phaseOneStatistics = summary.phaseOneTourLengthStatistics();
            ObjectiveStatistics finalStatistics = summary.finalObjectiveStatistics();

            System.out.printf(
                    "  %-24s phase1 tour avg=%.2f min=%d max=%d | final objective avg=%.2f min=%d max=%d | best=%d worst=%d%n",
                    summary.algorithmName(),
                    phaseOneStatistics.avgTourLength(),
                    phaseOneStatistics.minTourLength(),
                    phaseOneStatistics.maxTourLength(),
                    finalStatistics.avgObjective(),
                    finalStatistics.minObjective(),
                    finalStatistics.maxObjective(),
                    summary.bestFinalResult().solutionMetrics().objective(),
                    summary.worstFinalResult().solutionMetrics().objective()
            );
        }
    }

    private static void validateInstanceResults(List<InstanceExperimentResult> instanceResults) {
        if (instanceResults == null) {
            throw new IllegalArgumentException("instanceResults cannot be null");
        }
        if (instanceResults.isEmpty()) {
            throw new IllegalArgumentException("instanceResults cannot be empty");
        }

        for (InstanceExperimentResult instanceResult : instanceResults) {
            if (instanceResult == null) {
                throw new IllegalArgumentException("instanceResults cannot contain null");
            }
        }
    }
}