package experiment.run;

import algorithm.OptimizationAlgorithm;
import algorithm.construction.singlephase.RandomSolution;
import algorithm.construction.twophase.BestHeuristicSolution;
import algorithm.localsearch.NeighborhoodType;
import algorithm.localsearch.SteepestLocalSearch;
import algorithm.localsearch.SteepestLocalSearchWithCandidateMoves;
import algorithm.localsearch.SteepestLocalSearchWithMoveList;
import execute.AlgorithmExecutor;
import execute.ExecutionResult;
import experiment.summary.AlgorithmExperimentSummary;
import experiment.summary.InstanceExperimentResult;
import model.Instance;
import statistics.ObjectiveStatistics;
import statistics.ObjectiveStatisticsCalculator;
import statistics.RuntimeStatistics;
import statistics.RuntimeStatisticsCalculator;

import java.util.ArrayList;
import java.util.List;

public final class InstanceExperimentRunner {

    private static final int LAB3_RUNS_COUNT = 100;

    private final long baseSeed;
    private final AlgorithmExecutor algorithmExecutor;

    public InstanceExperimentRunner(long baseSeed) {
        this.baseSeed = baseSeed;
        this.algorithmExecutor = new AlgorithmExecutor();
    }

    public InstanceExperimentResult run(Instance instance) {
        /*
         * Dla danej instancji przygotowujemy jeden wspólny zestaw uruchomień.
         * Ten sam układ startVertexId i runSeed jest potem używany
         * przez wszystkie algorytmy na tej instancji.
         */
        List<RunConfig> runs = RunPreparator.prepareRuns(instance, baseSeed, LAB3_RUNS_COUNT);

        List<AlgorithmFactory> algorithmFactories = List.of(
            run -> new BestHeuristicSolution(),
            run -> new SteepestLocalSearch(
                "SteepestLS_RandomStart_SWAP_EDGES",
                new RandomSolution(run.runSeed()),
                NeighborhoodType.SWAP_EDGES
            ),
            run -> new SteepestLocalSearchWithMoveList(
                "SteepestLS_LM_RandomStart_SWAP_EDGES",
                new RandomSolution(run.runSeed())
            ),
            run -> new SteepestLocalSearchWithCandidateMoves(
                "SteepestLS_CM_RandomStart_SWAP_EDGES",
                new RandomSolution(run.runSeed())
            )
        );

        List<List<ExecutionResult>> resultsByAlgorithm =
            runAlgorithmsInterleaved(instance, runs, algorithmFactories);

        List<AlgorithmExperimentSummary> algorithmSummaries = new ArrayList<>(algorithmFactories.size());
        for (List<ExecutionResult> results : resultsByAlgorithm) {
            algorithmSummaries.add(buildSummaryAndPrintProgress(instance, results));
        }

        return new InstanceExperimentResult(
            instance.name,
            algorithmSummaries
        );
    }

    private List<List<ExecutionResult>> runAlgorithmsInterleaved(
        Instance instance,
        List<RunConfig> runs,
        List<AlgorithmFactory> algorithmFactories
    ) {
        List<List<ExecutionResult>> resultsByAlgorithm =
            createResultLists(algorithmFactories.size(), runs.size());

        for (RunConfig run : runs) {
            for (int algorithmIndex = 0; algorithmIndex < algorithmFactories.size(); algorithmIndex++) {
                AlgorithmFactory algorithmFactory = algorithmFactories.get(algorithmIndex);
                ExecutionResult result = executeAlgorithm(instance, run, algorithmFactory);

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
        AlgorithmFactory algorithmFactory
    ) {
        /*
         * Każde uruchomienie dostaje nową instancję algorytmu.
         * Dzięki temu cała losowość pozostaje w pełni deterministyczna
         * względem runSeed.
         */
        OptimizationAlgorithm algorithm = algorithmFactory.create(run);

        return algorithmExecutor.execute(
            instance,
            run.startVertexId(),
            algorithm.name(),
            algorithm
        );
    }

    private static AlgorithmExperimentSummary buildSummary(List<ExecutionResult> results) {
        ObjectiveStatistics objectiveStatistics =
            ObjectiveStatisticsCalculator.calculate(results);

        RuntimeStatistics runtimeStatistics =
            RuntimeStatisticsCalculator.calculate(results);

        ExecutionResult bestExecutionResult = findBestExecutionResult(results);

        return new AlgorithmExperimentSummary(
            objectiveStatistics.algorithmName(),
            objectiveStatistics,
            runtimeStatistics,
            bestExecutionResult
        );
    }

    private static AlgorithmExperimentSummary buildSummaryAndPrintProgress(
        Instance instance,
        List<ExecutionResult> results
    ) {
        AlgorithmExperimentSummary summary = buildSummary(results);

        System.out.println(
            "Zakonczono algorytm dla instancji "
                + instance.name
                + ": "
                + summary.algorithmName()
        );

        return summary;
    }

    private static ExecutionResult findBestExecutionResult(List<ExecutionResult> results) {
        ExecutionResult bestResult = results.get(0);
        int bestObjective = bestResult.solutionMetrics().objective();

        for (int i = 1; i < results.size(); i++) {
            ExecutionResult currentResult = results.get(i);
            int currentObjective = currentResult.solutionMetrics().objective();

            if (currentObjective > bestObjective) {
                bestObjective = currentObjective;
                bestResult = currentResult;
            }
        }

        return bestResult;
    }

    private interface AlgorithmFactory {
        OptimizationAlgorithm create(RunConfig run);
    }
}
