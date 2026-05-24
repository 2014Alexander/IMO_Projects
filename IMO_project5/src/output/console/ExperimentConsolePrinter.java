package output.console;

import experiment.summary.AlgorithmExperimentSummary;
import experiment.summary.ExperimentParameter;
import experiment.summary.InstanceExperimentResult;
import experiment.summary.IterationStatistics;
import experiment.summary.ObjectiveStatistics;
import experiment.summary.RuntimeStatistics;

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
        printParameters(instanceResult.parameters());
        printAlgorithmSummaries("Algorithms", instanceResult.algorithmSummaries());
        if (!instanceResult.referenceSummaries().isEmpty()) {
            System.out.println();
            printAlgorithmSummaries("Reference results", instanceResult.referenceSummaries());
        }
    }

    private void printHeader(String instanceName) {
        System.out.println("==================================================");
        System.out.println("Instance: " + instanceName);
        System.out.println("==================================================");
        System.out.println();
    }

    private void printParameters(List<ExperimentParameter> parameters) {
        if (parameters.isEmpty()) {
            return;
        }

        System.out.println("Parameters:");

        for (ExperimentParameter parameter : parameters) {
            System.out.println("  " + parameter.name() + " = " + parameter.value());
        }

        System.out.println();
    }

    private void printAlgorithmSummaries(String sectionName, List<AlgorithmExperimentSummary> summaries) {
        System.out.println(sectionName + ":");

        for (AlgorithmExperimentSummary summary : summaries) {
            ObjectiveStatistics objectiveStatistics = summary.objectiveStatistics();
            RuntimeStatistics runtimeStatistics = summary.runtimeStatistics();
            IterationStatistics iterationStatistics = summary.iterationStatistics();

            System.out.printf(
                    "  %-32s objective avg=%.2f min=%d max=%d | runtime avg=%.0f ns min=%d ns max=%d ns",
                    summary.algorithmName(),
                    objectiveStatistics.avgObjective(),
                    objectiveStatistics.minObjective(),
                    objectiveStatistics.maxObjective(),
                    runtimeStatistics.avgRuntimeNanos(),
                    runtimeStatistics.minRuntimeNanos(),
                    runtimeStatistics.maxRuntimeNanos()
            );

            if (iterationStatistics != null) {
                System.out.printf(
                        " | iterations avg=%.2f min=%d max=%d",
                        iterationStatistics.avgIterationCount(),
                        iterationStatistics.minIterationCount(),
                        iterationStatistics.maxIterationCount()
                );
            }

            System.out.println();
        }
    }
}
