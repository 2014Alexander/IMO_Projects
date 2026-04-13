package output.console;

import experiment.summary.AlgorithmExperimentSummary;
import experiment.summary.InstanceExperimentResult;
import statistics.ObjectiveStatistics;
import statistics.RuntimeStatistics;

import java.util.List;

public final class ExperimentConsolePrinter {

    public void print(List<InstanceExperimentResult> instanceResults) {
        for (int i = 0; i < instanceResults.size(); i++) {
            print(instanceResults.get(i));

            if (i < instanceResults.size() - 1) {
                System.out.println();
            }
        }
    }

    public void print(InstanceExperimentResult instanceResult) {
        printHeader(instanceResult.instanceName());
        printAlgorithmSummaries(instanceResult.algorithmSummaries());
    }

    private void printHeader(String instanceName) {
        System.out.println("==================================================");
        System.out.println("Instance: " + instanceName);
        System.out.println("==================================================");
        System.out.println();
    }

    private void printAlgorithmSummaries(List<AlgorithmExperimentSummary> summaries) {
        System.out.println("Algorithms:");

        for (AlgorithmExperimentSummary summary : summaries) {
            ObjectiveStatistics objectiveStatistics = summary.objectiveStatistics();
            RuntimeStatistics runtimeStatistics = summary.runtimeStatistics();

            System.out.printf(
                    "  %-32s objective avg=%.2f min=%d max=%d | runtime avg=%.0f ns min=%d ns max=%d ns%n",
                    summary.algorithmName(),
                    objectiveStatistics.avgObjective(),
                    objectiveStatistics.minObjective(),
                    objectiveStatistics.maxObjective(),
                    runtimeStatistics.avgRuntimeNanos(),
                    runtimeStatistics.minRuntimeNanos(),
                    runtimeStatistics.maxRuntimeNanos()
            );
        }
    }
}
