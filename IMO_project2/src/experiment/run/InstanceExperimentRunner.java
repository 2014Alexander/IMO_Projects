package experiment.run;

import algorithm.OptimizationAlgorithm;
import algorithm.construction.singlephase.RandomSolution;
import algorithm.construction.twophase.BestHeuristicSolution;
import algorithm.localsearch.GreedyLocalSearch;
import algorithm.localsearch.NeighborhoodType;
import algorithm.localsearch.RandomWalk;
import algorithm.localsearch.SteepestLocalSearch;
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

    private static final int RUNS_COUNT = 100;

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
        List<RunConfig> runs = RunPreparator.prepareRuns(instance, baseSeed, RUNS_COUNT);

        List<AlgorithmExperimentSummary> algorithmSummaries = new ArrayList<>(11);
        List<AlgorithmExperimentSummary> localSearchSummaries = new ArrayList<>(8);

        /*
         * Punkt odniesienia z poprzedniej laby.
         */
        algorithmSummaries.add(runBestHeuristicSummary(instance, runs));

        /*
         * Osiem wersji local search:
         * - Greedy / Steepest
         * - RandomStart / BestStart
         * - SWAP_VERTICES / SWAP_EDGES
         */
        localSearchSummaries.add(runGreedySummary(instance, runs, false, NeighborhoodType.SWAP_VERTICES));
        localSearchSummaries.add(runGreedySummary(instance, runs, false, NeighborhoodType.SWAP_EDGES));
        localSearchSummaries.add(runGreedySummary(instance, runs, true, NeighborhoodType.SWAP_VERTICES));
        localSearchSummaries.add(runGreedySummary(instance, runs, true, NeighborhoodType.SWAP_EDGES));

        localSearchSummaries.add(runSteepestSummary(instance, runs, false, NeighborhoodType.SWAP_VERTICES));
        localSearchSummaries.add(runSteepestSummary(instance, runs, false, NeighborhoodType.SWAP_EDGES));
        localSearchSummaries.add(runSteepestSummary(instance, runs, true, NeighborhoodType.SWAP_VERTICES));
        localSearchSummaries.add(runSteepestSummary(instance, runs, true, NeighborhoodType.SWAP_EDGES));

        algorithmSummaries.addAll(localSearchSummaries);

        /*
         * RandomWalk ma działać tyle, ile średnio najwolniejsza
         * wersja local search.
         */
        long randomWalkTimeLimitNanos = findRandomWalkTimeLimit(localSearchSummaries);

        algorithmSummaries.add(
            runRandomWalkSummary(instance, runs, NeighborhoodType.SWAP_VERTICES, randomWalkTimeLimitNanos)
        );
        algorithmSummaries.add(
            runRandomWalkSummary(instance, runs, NeighborhoodType.SWAP_EDGES, randomWalkTimeLimitNanos)
        );

        return new InstanceExperimentResult(
            instance.name,
            algorithmSummaries
        );
    }

    private AlgorithmExperimentSummary runBestHeuristicSummary(
        Instance instance,
        List<RunConfig> runs
    ) {
        List<ExecutionResult> results = runAlgorithm(
            instance,
            runs,
            run -> new BestHeuristicSolution()
        );

        return buildSummaryAndPrintProgress(instance, results);
    }

    private AlgorithmExperimentSummary runGreedySummary(
        Instance instance,
        List<RunConfig> runs,
        boolean bestStart,
        NeighborhoodType neighborhoodType
    ) {
        List<ExecutionResult> results = runAlgorithm(
            instance,
            runs,
            run -> {
                OptimizationAlgorithm initialSolutionAlgorithm =
                    createInitialSolutionAlgorithm(bestStart, run);

                return new GreedyLocalSearch(
                    buildGreedyName(bestStart, neighborhoodType),
                    initialSolutionAlgorithm,
                    neighborhoodType,
                    run.runSeed()
                );
            }
        );

        return buildSummaryAndPrintProgress(instance, results);
    }

    private AlgorithmExperimentSummary runSteepestSummary(
        Instance instance,
        List<RunConfig> runs,
        boolean bestStart,
        NeighborhoodType neighborhoodType
    ) {
        List<ExecutionResult> results = runAlgorithm(
            instance,
            runs,
            run -> {
                OptimizationAlgorithm initialSolutionAlgorithm =
                    createInitialSolutionAlgorithm(bestStart, run);

                return new SteepestLocalSearch(
                    buildSteepestName(bestStart, neighborhoodType),
                    initialSolutionAlgorithm,
                    neighborhoodType
                );
            }
        );

        return buildSummaryAndPrintProgress(instance, results);
    }

    private AlgorithmExperimentSummary runRandomWalkSummary(
        Instance instance,
        List<RunConfig> runs,
        NeighborhoodType neighborhoodType,
        long timeLimitNanos
    ) {
        List<ExecutionResult> results = runAlgorithm(
            instance,
            runs,
            run -> {
                /*
                 * RandomWalk startuje z rozwiązania początkowego
                 * wygenerowanego przez RandomSolution dla tego samego runSeed.
                 */
                OptimizationAlgorithm initialSolutionAlgorithm =
                    new RandomSolution(run.runSeed());

                return new RandomWalk(
                    buildRandomWalkName(neighborhoodType),
                    initialSolutionAlgorithm,
                    neighborhoodType,
                    timeLimitNanos,
                    run.runSeed()
                );
            }
        );

        return buildSummaryAndPrintProgress(instance, results);
    }

    private List<ExecutionResult> runAlgorithm(
        Instance instance,
        List<RunConfig> runs,
        AlgorithmFactory algorithmFactory
    ) {
        List<ExecutionResult> results = new ArrayList<>(runs.size());

        for (RunConfig run : runs) {
            /*
             * Każde uruchomienie dostaje nową instancję algorytmu.
             * Dzięki temu cała losowość pozostaje w pełni deterministyczna
             * względem runSeed.
             */
            OptimizationAlgorithm algorithm = algorithmFactory.create(run);

            ExecutionResult result = algorithmExecutor.execute(
                instance,
                run.startVertexId(),
                algorithm.name(),
                algorithm
            );

            results.add(result);
        }

        return results;
    }

    private static OptimizationAlgorithm createInitialSolutionAlgorithm(
        boolean bestStart,
        RunConfig run
    ) {
        if (bestStart) {
            return new BestHeuristicSolution();
        }

        return new RandomSolution(run.runSeed());
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

    private static long findRandomWalkTimeLimit(List<AlgorithmExperimentSummary> localSearchSummaries) {
        double slowestAverageRuntimeNanos = 0.0;

        for (AlgorithmExperimentSummary summary : localSearchSummaries) {
            double avgRuntimeNanos = summary.runtimeStatistics().avgRuntimeNanos();

            if (avgRuntimeNanos > slowestAverageRuntimeNanos) {
                slowestAverageRuntimeNanos = avgRuntimeNanos;
            }
        }

        return Math.round(slowestAverageRuntimeNanos);
    }

    private static String buildGreedyName(
        boolean bestStart,
        NeighborhoodType neighborhoodType
    ) {
        String startName = bestStart ? "BestStart" : "RandomStart";
        return "GreedyLS_" + startName + "_" + neighborhoodType.name();
    }

    private static String buildSteepestName(
        boolean bestStart,
        NeighborhoodType neighborhoodType
    ) {
        String startName = bestStart ? "BestStart" : "RandomStart";
        return "SteepestLS_" + startName + "_" + neighborhoodType.name();
    }

    private static String buildRandomWalkName(NeighborhoodType neighborhoodType) {
        return "RandomWalk_RandomStart_" + neighborhoodType.name();
    }

    private interface AlgorithmFactory {
        OptimizationAlgorithm create(RunConfig run);
    }
}
