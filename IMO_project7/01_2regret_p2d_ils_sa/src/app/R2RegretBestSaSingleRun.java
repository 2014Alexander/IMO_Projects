package app;

import algorithm.IterationCountingAlgorithm;
import algorithm.OptimizationAlgorithm;
import algorithm.construction.K8M3StdWithPhaseTwoDelete;
import algorithm.construction.K8Mixed3WithPhaseTwoDeleteStart;
import algorithm.construction.Randomized2RegretBestNWithPhaseTwoDeleteStart;
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

public final class R2RegretBestSaSingleRun {
    private static final long TIME_LIMIT_NANOS = 500_000_000L;
    private static final double T0 = 300.0;
    private static final double TMIN = 10.0;
    private static final CoolingSchedule COOLING = CoolingSchedule.GEOMETRIC;

    public static void main(String[] args) {
        String kind = args[0];
        String instancePath = args[1];
        int runsCount = Integer.parseInt(args[2]);
        long baseSeed = Long.parseLong(args[3]);
        int runIndex = Integer.parseInt(args[4]);

        Instance instance = new CsvInstanceReader().read(instancePath);
        List<RunConfig> runs = RunPreparator.prepareRuns(instance, baseSeed, runsCount);
        RunConfig run = runs.get(runIndex);
        OptimizationAlgorithm algorithm = createAlgorithm(kind, run.runSeed());

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
    }

    private static OptimizationAlgorithm createAlgorithm(String algorithmKind, long seed) {
        if (algorithmKind.equals("k8")) {
            return constructionStartSa("ILS_K8_M3_STD_P2D_START_SA_GEO_T0_300_TMIN_10", new K8M3StdWithPhaseTwoDelete(), seed);
        }
        if (algorithmKind.equals("k8mixed3")) {
            return constructionStartSa(
                "ILS_K8_MIXED3_SAME_START_P2D_SA_GEO_T0_300_TMIN_10",
                new K8Mixed3WithPhaseTwoDeleteStart(seed, new SteepestLocalSearchWithCandidateMoves()),
                seed
            );
        }
        if (algorithmKind.equals("r2top3best5")) {
            return randomized2RegretBestStartSa("ILS_R2REGRET_TOP3P20_BEST5_P2D_LS_START_SA_GEO_T0_300_TMIN_10", false, 5, seed);
        }
        if (algorithmKind.equals("r2top3best10")) {
            return randomized2RegretBestStartSa("ILS_R2REGRET_TOP3P20_BEST10_P2D_LS_START_SA_GEO_T0_300_TMIN_10", false, 10, seed);
        }
        if (algorithmKind.equals("r2top5best5")) {
            return randomized2RegretBestStartSa("ILS_R2REGRET_TOP5P20_BEST5_P2D_LS_START_SA_GEO_T0_300_TMIN_10", true, 5, seed);
        }
        if (algorithmKind.equals("r2top5best10")) {
            return randomized2RegretBestStartSa("ILS_R2REGRET_TOP5P20_BEST10_P2D_LS_START_SA_GEO_T0_300_TMIN_10", true, 10, seed);
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

    private static OptimizationAlgorithm randomized2RegretBestStartSa(String name, boolean top5, int triesCount, long seed) {
        return constructionStartSa(
            name,
            new Randomized2RegretBestNWithPhaseTwoDeleteStart(
                name + "_START",
                top5,
                triesCount,
                seed,
                new SteepestLocalSearchWithCandidateMoves()
            ),
            seed
        );
    }

    private static OptimizationAlgorithm constructionStartSa(String name, OptimizationAlgorithm startAlgorithm, long seed) {
        return new IteratedLocalSearchWithConstructionStartAndSaAcceptance(
            name,
            startAlgorithm,
            new SteepestLocalSearchWithCandidateMoves(),
            new RandomSwapEdgesPerturbation(30),
            TIME_LIMIT_NANOS,
            seed,
            T0,
            TMIN,
            COOLING
        );
    }
}
