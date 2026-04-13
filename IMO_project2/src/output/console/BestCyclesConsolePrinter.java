package output.console;

import evaluation.SolutionMetrics;
import execute.ExecutionResult;
import experiment.summary.AlgorithmExperimentSummary;
import experiment.summary.InstanceExperimentResult;
import model.Solution;

import java.util.List;
import java.util.StringJoiner;

public final class BestCyclesConsolePrinter {

    public void print(List<InstanceExperimentResult> instanceResults) {
        for (InstanceExperimentResult instanceResult : instanceResults) {
            print(instanceResult);
        }
    }

    public void print(InstanceExperimentResult instanceResult) {
        for (AlgorithmExperimentSummary summary : instanceResult.algorithmSummaries()) {
            printBestResult(instanceResult.instanceName(), summary.algorithmName(), summary.bestExecutionResult());
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
                        + " | Profit=" + metrics.profitSum()
                        + " | Tour length=" + metrics.tourLength()
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
}
