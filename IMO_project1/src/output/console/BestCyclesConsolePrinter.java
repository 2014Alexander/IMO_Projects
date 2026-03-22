package output.console;

import execute.ExecutionResult;
import experiment.summary.InstanceExperimentResult;
import experiment.summary.SinglePhaseExperimentSummary;
import experiment.summary.TwoPhaseExperimentSummary;
import model.Solution;
import evaluation.SolutionMetrics;

import java.util.List;
import java.util.StringJoiner;

public final class BestCyclesConsolePrinter {

    public void print(List<InstanceExperimentResult> instanceResults) {
        validateInstanceResults(instanceResults);

        for (InstanceExperimentResult instanceResult : instanceResults) {
            print(instanceResult);
        }
    }

    public void print(InstanceExperimentResult instanceResult) {
        if (instanceResult == null) {
            throw new IllegalArgumentException("instanceResult cannot be null");
        }

        for (SinglePhaseExperimentSummary summary : instanceResult.singlePhaseSummaries()) {
            printBestResult(summary.instanceName(), summary.algorithmName(), summary.bestResult());
        }

        for (TwoPhaseExperimentSummary summary : instanceResult.twoPhaseSummaries()) {
            printBestResult(summary.instanceName(), summary.algorithmName(), summary.bestFinalResult());
        }
    }

    private void printBestResult(
            String instanceName,
            String algorithmName,
            ExecutionResult bestResult
    ) {
        Solution solution = bestResult.solution();
        SolutionMetrics metrics = bestResult.solutionMetrics();

        System.out.println(
                instanceName
                        + " | "
                        + algorithmName
                        + " | "
                        + cycleToCsv(solution.cycle())
                        + " | Cost=" + metrics.profitSum()
                        + " | Edge length=" + metrics.tourLength()
                        + " | Objective=" + metrics.objective()
        );
    }

    private String cycleToCsv(List<Integer> cycle) {
        StringJoiner joiner = new StringJoiner(",");

        for (Integer vertexId : cycle) {
            joiner.add(String.valueOf(vertexId));
        }

        return joiner.toString();
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