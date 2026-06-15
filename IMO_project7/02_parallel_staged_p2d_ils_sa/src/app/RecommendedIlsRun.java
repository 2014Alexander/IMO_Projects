package app;

import algorithm.IterationCountingAlgorithm;
import algorithm.OptimizationAlgorithm;
import algorithm.localsearch.SteepestLocalSearchWithCandidateMoves;
import algorithm.metaheuristic.CoolingSchedule;
import algorithm.metaheuristic.IteratedLocalSearchWithTwoRegretStartAndSaAcceptance;
import algorithm.metaheuristic.SaAcceptanceStatistics;
import algorithm.metaheuristic.perturbation.RandomSwapEdgesPerturbation;
import evaluation.SolutionEvaluator;
import experiment.core.RunConfig;
import experiment.core.RunPreparator;
import io.CsvInstanceReader;
import model.Instance;
import model.Solution;

import java.util.List;

/**
 * Docelowy wariant ILS dla instancji A i B.
 * Parametry: start 2-regret + druga faza, akceptacja SA, T0=300, Tmin=10,
 * chlodzenie geometryczne po czasie, limit 0.5 s, perturbacja 30 SwapEdges.
 */
public final class RecommendedIlsRun {
    private static final long TIME_LIMIT_NANOS = 1_000_000_000L;
    private static final int DEFAULT_RUNS_COUNT = 20;
    private static final long DEFAULT_BASE_SEED = 12345L;

    private static final String ALGORITHM_NAME = "ILS_2REGRET_P2D_START_SA_GEO_T0_300_TMIN_10";
    private static final double INITIAL_TEMPERATURE = 300.0;
    private static final double FINAL_TEMPERATURE = 10.0;

    public static void main(String[] args) {
        int runsCount = args.length >= 1 ? Integer.parseInt(args[0]) : DEFAULT_RUNS_COUNT;
        long baseSeed = args.length >= 2 ? Long.parseLong(args[1]) : DEFAULT_BASE_SEED;

        String[] instancePaths = args.length >= 3
            ? instancePaths(args)
            : new String[] {"data/TSPA.csv", "data/TSPB.csv"};

        System.out.println("instance,algorithm,runIndex,startVertexId,seed,objective,runtimeNanos,iterationCount,acceptedBetterCount,acceptedWorseCount,rejectedWorseCount,bestFoundIteration,cooling,T0,Tmin");

        for (String instancePath : instancePaths) {
            Instance instance = new CsvInstanceReader().read(instancePath);
            List<RunConfig> runs = RunPreparator.prepareRuns(instance, baseSeed, runsCount);

            for (int runIndex = 0; runIndex < runs.size(); runIndex++) {
                RunConfig run = runs.get(runIndex);
                OptimizationAlgorithm algorithm = new IteratedLocalSearchWithTwoRegretStartAndSaAcceptance(
                    ALGORITHM_NAME,
                    new SteepestLocalSearchWithCandidateMoves(),
                    new RandomSwapEdgesPerturbation(30),
                    TIME_LIMIT_NANOS,
                    run.runSeed(),
                    INITIAL_TEMPERATURE,
                    FINAL_TEMPERATURE,
                    CoolingSchedule.GEOMETRIC
                );

                long startTime = System.nanoTime();
                Solution solution = algorithm.solve(instance, run.startVertexId());
                long runtimeNanos = System.nanoTime() - startTime;

                int objective = new SolutionEvaluator().evaluate(instance, solution).objective();
                int iterationCount = ((IterationCountingAlgorithm) algorithm).lastIterationCount();
                SaAcceptanceStatistics stats = (SaAcceptanceStatistics) algorithm;

                System.out.println(
                    instance.name + "," + ALGORITHM_NAME + "," + runIndex + ","
                        + run.startVertexId() + "," + run.runSeed() + ","
                        + objective + "," + runtimeNanos + "," + iterationCount + ","
                        + stats.acceptedBetterCount() + "," + stats.acceptedWorseCount() + ","
                        + stats.rejectedWorseCount() + "," + stats.bestFoundIteration() + ","
                        + CoolingSchedule.GEOMETRIC + "," + INITIAL_TEMPERATURE + "," + FINAL_TEMPERATURE
                );
            }
        }
    }

    private static String[] instancePaths(String[] args) {
        String[] paths = new String[args.length - 2];
        System.arraycopy(args, 2, paths, 0, paths.length);
        return paths;
    }
}
