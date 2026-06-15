package experiment.lab3;

import algorithm.OptimizationAlgorithm;
import experiment.core.RunConfig;
import experiment.core.RunPreparator;
import experiment.core.TestedAlgorithm;
import experiment.execution.AlgorithmExecutor;
import experiment.execution.ExecutionResult;
import experiment.summary.AlgorithmExperimentSummary;
import experiment.summary.ExecutionResultsSummarizer;
import experiment.summary.ExperimentParameter;
import experiment.summary.InstanceExperimentResult;
import model.Instance;

import java.util.ArrayList;
import java.util.List;

public final class InstanceExperimentRunner {
    private final long baseSeed;
    private final Lab3Scenario scenario;
    private final AlgorithmExecutor algorithmExecutor;

    public InstanceExperimentRunner(long baseSeed, Lab3Scenario scenario) {
        this.baseSeed = baseSeed;
        this.scenario = scenario;
        this.algorithmExecutor = new AlgorithmExecutor();
    }

    public InstanceExperimentResult run(Instance instance) {
        /*
         * Dla danej instancji przygotowujemy jeden wspólny zestaw uruchomień.
         * Ten sam układ startVertexId i runSeed jest potem używany
         * przez wszystkie algorytmy na tej instancji.
         */
        List<RunConfig> runs = RunPreparator.prepareRuns(instance, baseSeed, scenario.runsCount());
        List<TestedAlgorithm> testedAlgorithms = scenario.testedAlgorithms();

        List<List<ExecutionResult>> resultsByAlgorithm =
            runTestedAlgorithmsOnSharedRuns(instance, runs, testedAlgorithms);

        List<AlgorithmExperimentSummary> algorithmSummaries = new ArrayList<>(testedAlgorithms.size());
        for (List<ExecutionResult> results : resultsByAlgorithm) {
            AlgorithmExperimentSummary summary = ExecutionResultsSummarizer.summarize(results);
            printProgress(instance, summary);
            algorithmSummaries.add(summary);
        }

        return new InstanceExperimentResult(
            instance.name,
            List.of(new ExperimentParameter("runsCount", Integer.toString(scenario.runsCount()))),
            algorithmSummaries
        );
    }

    private List<List<ExecutionResult>> runTestedAlgorithmsOnSharedRuns(
        Instance instance,
        List<RunConfig> runs,
        List<TestedAlgorithm> testedAlgorithms
    ) {
        List<List<ExecutionResult>> resultsByAlgorithm =
            createResultLists(testedAlgorithms.size(), runs.size());

        for (RunConfig run : runs) {
            for (int algorithmIndex = 0; algorithmIndex < testedAlgorithms.size(); algorithmIndex++) {
                TestedAlgorithm testedAlgorithm = testedAlgorithms.get(algorithmIndex);
                ExecutionResult result = executeAlgorithm(instance, run, testedAlgorithm);

                resultsByAlgorithm.get(algorithmIndex).add(result);
            }
        }

        return resultsByAlgorithm;
    }

    private static List<List<ExecutionResult>> createResultLists(int algorithmsCount, int runsCount) {
        List<List<ExecutionResult>> resultsByAlgorithm = new ArrayList<>(algorithmsCount);
        for (int algorithmIndex = 0; algorithmIndex < algorithmsCount; algorithmIndex++) {
            resultsByAlgorithm.add(new ArrayList<>(runsCount));
        }

        return resultsByAlgorithm;
    }

    private ExecutionResult executeAlgorithm(
        Instance instance,
        RunConfig run,
        TestedAlgorithm testedAlgorithm
    ) {
        /*
         * Każde uruchomienie dostaje nową instancję algorytmu.
         * Dzięki temu cała losowość pozostaje w pełni deterministyczna
         * względem runSeed.
         */
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
