package experiment.lab6;

import algorithm.OptimizationAlgorithm;
import experiment.core.RunConfig;
import experiment.core.RunPreparator;
import experiment.core.TestedAlgorithm;
import experiment.execution.AlgorithmExecutor;
import experiment.execution.ExecutionResult;
import experiment.summary.AlgorithmExperimentSummary;
import experiment.summary.ExecutionResultsSummarizer;
import experiment.summary.InstanceExperimentResult;
import model.Instance;

import java.util.ArrayList;
import java.util.List;

public final class Lab6InstanceExperimentRunner {
    private final long baseSeed;
    private final Lab6Scenario scenario;
    private final AlgorithmExecutor algorithmExecutor;

    public Lab6InstanceExperimentRunner(long baseSeed, Lab6Scenario scenario) {
        this.baseSeed = baseSeed;
        this.scenario = scenario;
        this.algorithmExecutor = new AlgorithmExecutor();
    }

    public InstanceExperimentResult run(Instance instance) {
        List<RunConfig> runs = RunPreparator.prepareRuns(instance, baseSeed, scenario.runsCount());

        TestedAlgorithm mslsAlgorithm = scenario.mslsAlgorithm();
        List<ExecutionResult> mslsResults = runAlgorithmOnSharedRuns(instance, runs, mslsAlgorithm);
        AlgorithmExperimentSummary mslsSummary = ExecutionResultsSummarizer.summarize(mslsResults);
        printProgress(instance, mslsSummary);

        double mslsAverageRuntimeNanos = mslsSummary.runtimeStatistics().avgRuntimeNanos();
        long timeLimitNanos = Math.round(mslsAverageRuntimeNanos);

        List<AlgorithmExperimentSummary> mainSummaries = new ArrayList<>();
        mainSummaries.add(mslsSummary);
        for (TestedAlgorithm testedAlgorithm : scenario.mainTimedAlgorithms(timeLimitNanos)) {
            List<ExecutionResult> results = runAlgorithmOnSharedRuns(instance, runs, testedAlgorithm);
            AlgorithmExperimentSummary summary = ExecutionResultsSummarizer.summarize(results);
            printProgress(instance, summary);
            mainSummaries.add(summary);
        }

        List<AlgorithmExperimentSummary> referenceSummaries = new ArrayList<>();
        for (TestedAlgorithm testedAlgorithm : scenario.referenceAlgorithms()) {
            List<ExecutionResult> results = runAlgorithmOnSharedRuns(instance, runs, testedAlgorithm);
            AlgorithmExperimentSummary summary = ExecutionResultsSummarizer.summarize(results);
            printProgress(instance, summary);
            referenceSummaries.add(summary);
        }

        return new InstanceExperimentResult(
            instance.name,
            scenario.parameters(mslsAverageRuntimeNanos, timeLimitNanos),
            mainSummaries,
            referenceSummaries
        );
    }

    private List<ExecutionResult> runAlgorithmOnSharedRuns(
        Instance instance,
        List<RunConfig> runs,
        TestedAlgorithm testedAlgorithm
    ) {
        List<ExecutionResult> results = new ArrayList<>(runs.size());

        for (RunConfig run : runs) {
            results.add(executeAlgorithm(instance, run, testedAlgorithm));
        }

        return results;
    }

    private ExecutionResult executeAlgorithm(
        Instance instance,
        RunConfig run,
        TestedAlgorithm testedAlgorithm
    ) {
        OptimizationAlgorithm algorithm = testedAlgorithm.create(run);

        return algorithmExecutor.execute(
            instance,
            run.startVertexId(),
            testedAlgorithm.name(),
            algorithm
        );
    }

    private static void printProgress(Instance instance, AlgorithmExperimentSummary summary) {
        System.out.println(
            "Zakonczono algorytm dla instancji "
                + instance.name
                + ": "
                + summary.algorithmName()
        );
    }
}
