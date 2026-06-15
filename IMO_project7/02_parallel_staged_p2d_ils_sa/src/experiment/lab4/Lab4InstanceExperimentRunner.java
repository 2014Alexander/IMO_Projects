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
     * Uruchamia test ILS dla jednej instancji.
     *
     * @param instance instancja problemu
     * @return wynik testu dla instancji
     */
    public InstanceExperimentResult run(Instance instance) {
        List<RunConfig> runs = RunPreparator.prepareRuns(instance, baseSeed, scenario.runsCount());
        List<AlgorithmExperimentSummary> algorithmSummaries = new ArrayList<>(scenario.algorithms().size());

        for (TestedAlgorithm testedAlgorithm : scenario.algorithms()) {
            List<ExecutionResult> results = runAlgorithmOnSharedRuns(instance, runs, testedAlgorithm);
            AlgorithmExperimentSummary summary = ExecutionResultsSummarizer.summarize(results);
            printProgress(instance, summary);
            algorithmSummaries.add(summary);
        }

        return new InstanceExperimentResult(
            instance.name,
            scenario.parameters(),
            algorithmSummaries
        );
    }

    private List<ExecutionResult> runAlgorithmOnSharedRuns(
        Instance instance,
        List<RunConfig> runs,
        TestedAlgorithm testedAlgorithm
    ) {
        List<ExecutionResult> results = new ArrayList<>(runs.size());

        for (int runIndex = 0; runIndex < runs.size(); runIndex++) {
            RunConfig run = runs.get(runIndex);
            System.out.println("Start uruchomienia " + instance.name + " " + testedAlgorithm.name() + " #" + runIndex);
            results.add(executeAlgorithm(instance, run, testedAlgorithm));
            System.out.println("Koniec uruchomienia " + instance.name + " " + testedAlgorithm.name() + " #" + runIndex);
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
