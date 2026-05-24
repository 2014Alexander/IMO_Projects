package experiment.lab4;

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

public final class Lab4InstanceExperimentRunner {
    private final long baseSeed;
    private final Lab4Scenario scenario;
    private final AlgorithmExecutor algorithmExecutor;

    public Lab4InstanceExperimentRunner(long baseSeed, Lab4Scenario scenario) {
        this.baseSeed = baseSeed;
        this.scenario = scenario;
        this.algorithmExecutor = new AlgorithmExecutor();
    }

    /**
     * Uruchamia pelny eksperyment lab4 dla jednej instancji.
     *
     * @param instance instancja problemu
     * @return wynik eksperymentu dla instancji
     */
    public InstanceExperimentResult run(Instance instance) {
        List<RunConfig> runs = RunPreparator.prepareRuns(instance, baseSeed, scenario.runsCount());

        TestedAlgorithm mslsAlgorithm = scenario.mslsAlgorithm();
        List<ExecutionResult> mslsResults = runAlgorithmOnSharedRuns(instance, runs, mslsAlgorithm);
        AlgorithmExperimentSummary mslsSummary = ExecutionResultsSummarizer.summarize(mslsResults);
        printProgress(instance, mslsSummary);

        double mslsAverageRuntimeNanos = mslsSummary.runtimeStatistics().avgRuntimeNanos();
        long timeLimitNanos = Math.round(mslsAverageRuntimeNanos);

        List<TestedAlgorithm> timedAlgorithms = scenario.timedAlgorithms(timeLimitNanos);
        List<AlgorithmExperimentSummary> algorithmSummaries =
            new ArrayList<>(1 + timedAlgorithms.size());
        algorithmSummaries.add(mslsSummary);

        for (TestedAlgorithm timedAlgorithm : timedAlgorithms) {
            List<ExecutionResult> results = runAlgorithmOnSharedRuns(instance, runs, timedAlgorithm);
            AlgorithmExperimentSummary summary = ExecutionResultsSummarizer.summarize(results);
            printProgress(instance, summary);
            algorithmSummaries.add(summary);
        }

        return new InstanceExperimentResult(
            instance.name,
            scenario.parameters(mslsAverageRuntimeNanos, timeLimitNanos),
            algorithmSummaries
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

    private static void printProgress(
        Instance instance,
        AlgorithmExperimentSummary summary
    ) {
        System.out.println(
            "Zakonczono algorytm dla instancji "
                + instance.name
                + ": "
                + summary.algorithmName()
        );
    }
}
