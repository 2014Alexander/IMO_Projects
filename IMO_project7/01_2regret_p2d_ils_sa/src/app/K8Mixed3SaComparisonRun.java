package app;

import algorithm.IterationCountingAlgorithm;
import algorithm.OptimizationAlgorithm;
import algorithm.construction.K8M3StdWithPhaseTwoDelete;
import algorithm.construction.K8Mixed3WithPhaseTwoDeleteStart;
import algorithm.construction.K8Top3P20BestOf3WithPhaseTwoDeleteStart;
import algorithm.localsearch.SteepestLocalSearchWithCandidateMoves;
import algorithm.metaheuristic.CoolingSchedule;
import algorithm.metaheuristic.IteratedLocalSearchWithConstructionStartAndSaAcceptance;
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

public final class K8Mixed3SaComparisonRun {
    private static final long TIME_LIMIT_NANOS = 500_000_000L;
    private static final int DEFAULT_RUNS_COUNT = 20;
    private static final long DEFAULT_BASE_SEED = 12345L;
    private static final double T0 = 300.0;
    private static final double TMIN = 10.0;
    private static final CoolingSchedule COOLING = CoolingSchedule.GEOMETRIC;

    public static void main(String[] args) {
        int runsCount = args.length >= 1 ? Integer.parseInt(args[0]) : DEFAULT_RUNS_COUNT;
        long baseSeed = args.length >= 2 ? Long.parseLong(args[1]) : DEFAULT_BASE_SEED;
        String[] instancePaths = args.length >= 3
            ? instancePaths(args)
            : new String[] {"data/TSPA.csv", "data/TSPB.csv"};

        System.out.println("instance,algorithm,runIndex,startVertexId,seed,objective,runtimeNanos,iterationCount,acceptedBetterCount,acceptedWorseCount,rejectedWorseCount,bestFoundIteration,cooling,T0,Tmin");
        System.out.flush();

        for (String instancePath : instancePaths) {
            Instance instance = new CsvInstanceReader().read(instancePath);
            List<RunConfig> runs = RunPreparator.prepareRuns(instance, baseSeed, runsCount);

            runAlgorithm(instance, runs, "2regret");
            runAlgorithm(instance, runs, "k8");
            runAlgorithm(instance, runs, "top3_best3");
            runAlgorithm(instance, runs, "mixed3");
        }
    }

    private static void runAlgorithm(Instance instance, List<RunConfig> runs, String algorithmKind) {
        for (int runIndex = 0; runIndex < runs.size(); runIndex++) {
            RunConfig run = runs.get(runIndex);
            OptimizationAlgorithm algorithm = createAlgorithm(algorithmKind, run.runSeed());
            long start = System.nanoTime();
            Solution solution = algorithm.solve(instance, run.startVertexId());
            long runtime = System.nanoTime() - start;
            int objective = new SolutionEvaluator().evaluate(instance, solution).objective();
            int iterationCount = ((IterationCountingAlgorithm) algorithm).lastIterationCount();
            SaAcceptanceStatistics stats = (SaAcceptanceStatistics) algorithm;

            System.out.println(
                instance.name + "," + algorithm.name() + "," + runIndex + ","
                    + run.startVertexId() + "," + run.runSeed() + ","
                    + objective + "," + runtime + "," + iterationCount + ","
                    + stats.acceptedBetterCount() + "," + stats.acceptedWorseCount() + ","
                    + stats.rejectedWorseCount() + "," + stats.bestFoundIteration() + ","
                    + COOLING + "," + T0 + "," + TMIN
            );
            System.out.flush();
        }
    }

    private static OptimizationAlgorithm createAlgorithm(String algorithmKind, long seed) {
        if (algorithmKind.equals("k8")) {
            return new IteratedLocalSearchWithConstructionStartAndSaAcceptance(
                "ILS_K8_M3_STD_P2D_START_SA_GEO_T0_300_TMIN_10",
                new K8M3StdWithPhaseTwoDelete(),
                new SteepestLocalSearchWithCandidateMoves(),
                new RandomSwapEdgesPerturbation(30),
                TIME_LIMIT_NANOS,
                seed,
                T0,
                TMIN,
                COOLING
            );
        }
        if (algorithmKind.equals("top3_best3")) {
            return new IteratedLocalSearchWithConstructionStartAndSaAcceptance(
                "ILS_K8_TOP3P20_BEST3_SAME_START_P2D_SA_GEO_T0_300_TMIN_10",
                new K8Top3P20BestOf3WithPhaseTwoDeleteStart(seed, new SteepestLocalSearchWithCandidateMoves()),
                new SteepestLocalSearchWithCandidateMoves(),
                new RandomSwapEdgesPerturbation(30),
                TIME_LIMIT_NANOS,
                seed,
                T0,
                TMIN,
                COOLING
            );
        }
        if (algorithmKind.equals("mixed3")) {
            return new IteratedLocalSearchWithConstructionStartAndSaAcceptance(
                "ILS_K8_MIXED3_SAME_START_P2D_SA_GEO_T0_300_TMIN_10",
                new K8Mixed3WithPhaseTwoDeleteStart(seed, new SteepestLocalSearchWithCandidateMoves()),
                new SteepestLocalSearchWithCandidateMoves(),
                new RandomSwapEdgesPerturbation(30),
                TIME_LIMIT_NANOS,
                seed,
                T0,
                TMIN,
                COOLING
            );
        }
        return new IteratedLocalSearchWithTwoRegretStartAndSaAcceptance(
            "ILS_2REGRET_P2D_START_SA_GEO_T0_300_TMIN_10",
            new SteepestLocalSearchWithCandidateMoves(),
            new RandomSwapEdgesPerturbation(30),
            TIME_LIMIT_NANOS,
            seed,
            T0,
            TMIN,
            COOLING
        );
    }

    private static String[] instancePaths(String[] args) {
        String[] paths = new String[args.length - 2];
        System.arraycopy(args, 2, paths, 0, paths.length);
        return paths;
    }
}
