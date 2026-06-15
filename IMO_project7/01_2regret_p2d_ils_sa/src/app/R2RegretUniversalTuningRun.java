package app;

import algorithm.IterationCountingAlgorithm;
import algorithm.OptimizationAlgorithm;
import algorithm.construction.Randomized2RegretParamBestNWithPhaseTwoDeleteStart;
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

import java.util.ArrayList;
import java.util.List;

public final class R2RegretUniversalTuningRun {
    private static final long TIME_LIMIT_NANOS = 500_000_000L;
    private static final int DEFAULT_RUNS_COUNT = 10;
    private static final long DEFAULT_BASE_SEED = 12345L;
    private static final int TOP_K = 3;
    private static final double RANDOM_PROBABILITY = 0.30;
    private static final int TRIES_COUNT = 12;
    private static final CoolingSchedule COOLING = CoolingSchedule.GEOMETRIC;

    private record Variant(String name, int swapEdges, double t0, double tmin, boolean baseline) {}

    public static void main(String[] args) {
        int runsCount = args.length >= 1 ? Integer.parseInt(args[0]) : DEFAULT_RUNS_COUNT;
        long baseSeed = args.length >= 2 ? Long.parseLong(args[1]) : DEFAULT_BASE_SEED;
        String[] instancePaths = args.length >= 3 ? instancePaths(args) : new String[] {"data/TSPA.csv", "data/TSPB.csv"};
        List<Variant> variants = variants();
        System.out.println("instance,algorithm,runIndex,startVertexId,seed,objective,runtimeNanos,iterationCount,acceptedBetterCount,acceptedWorseCount,rejectedWorseCount,bestFoundIteration,topK,probability,triesCount,swapEdges,cooling,T0,Tmin");
        System.out.flush();
        for (String instancePath : instancePaths) {
            Instance instance = new CsvInstanceReader().read(instancePath);
            List<RunConfig> runs = RunPreparator.prepareRuns(instance, baseSeed, runsCount);
            for (Variant variant : variants) {
                runVariant(instance, runs, variant);
            }
        }
    }

    private static List<Variant> variants() {
        List<Variant> result = new ArrayList<>();
        result.add(new Variant("ILS_2REGRET_P2D_START_SA_GEO_T0_300_TMIN_10_SWAP30", 30, 300.0, 10.0, true));

        // Tuning perturbacji dla aktualnego uniwersalnego startu TOP3P30_BEST12.
        result.add(new Variant("ILS_R2REGRET_TOP3P30_BEST12_P2D_LS_START_SA_GEO_T0_300_TMIN_10_SWAP25", 25, 300.0, 10.0, false));
        result.add(new Variant("ILS_R2REGRET_TOP3P30_BEST12_P2D_LS_START_SA_GEO_T0_300_TMIN_10_SWAP30", 30, 300.0, 10.0, false));
        result.add(new Variant("ILS_R2REGRET_TOP3P30_BEST12_P2D_LS_START_SA_GEO_T0_300_TMIN_10_SWAP35", 35, 300.0, 10.0, false));

        // Tuning temperatury SA dla perturbacji 30 SwapEdges.
        double[] t0Values = {200.0, 300.0, 500.0};
        double[] tminValues = {5.0, 10.0, 20.0};
        for (double t0 : t0Values) {
            for (double tmin : tminValues) {
                if (t0 == 300.0 && tmin == 10.0) {
                    continue;
                }
                result.add(new Variant(
                    String.format("ILS_R2REGRET_TOP3P30_BEST12_P2D_LS_START_SA_GEO_T0_%.0f_TMIN_%.0f_SWAP30", t0, tmin),
                    30,
                    t0,
                    tmin,
                    false
                ));
            }
        }
        return result;
    }

    private static void runVariant(Instance instance, List<RunConfig> runs, Variant variant) {
        for (int runIndex = 0; runIndex < runs.size(); runIndex++) {
            RunConfig run = runs.get(runIndex);
            OptimizationAlgorithm algorithm = createAlgorithm(variant, run.runSeed());
            long start = System.nanoTime();
            Solution solution = algorithm.solve(instance, run.startVertexId());
            long runtime = System.nanoTime() - start;
            int objective = new SolutionEvaluator().evaluate(instance, solution).objective();
            int iterationCount = ((IterationCountingAlgorithm) algorithm).lastIterationCount();
            SaAcceptanceStatistics stats = (SaAcceptanceStatistics) algorithm;
            System.out.println(instance.name + "," + algorithm.name() + "," + runIndex + "," + run.startVertexId() + "," + run.runSeed() + "," + objective + "," + runtime + "," + iterationCount + "," + stats.acceptedBetterCount() + "," + stats.acceptedWorseCount() + "," + stats.rejectedWorseCount() + "," + stats.bestFoundIteration() + "," + TOP_K + "," + RANDOM_PROBABILITY + "," + TRIES_COUNT + "," + variant.swapEdges() + "," + COOLING + "," + variant.t0() + "," + variant.tmin());
            System.out.flush();
        }
    }

    private static OptimizationAlgorithm createAlgorithm(Variant variant, long seed) {
        if (variant.baseline()) {
            return new IteratedLocalSearchWithTwoRegretStartAndSaAcceptance(
                variant.name(),
                new SteepestLocalSearchWithCandidateMoves(),
                new RandomSwapEdgesPerturbation(variant.swapEdges()),
                TIME_LIMIT_NANOS,
                seed,
                variant.t0(),
                variant.tmin(),
                COOLING
            );
        }
        return new IteratedLocalSearchWithConstructionStartAndSaAcceptance(
            variant.name(),
            new Randomized2RegretParamBestNWithPhaseTwoDeleteStart(
                variant.name() + "_START",
                TOP_K,
                RANDOM_PROBABILITY,
                TRIES_COUNT,
                seed,
                new SteepestLocalSearchWithCandidateMoves()
            ),
            new SteepestLocalSearchWithCandidateMoves(),
            new RandomSwapEdgesPerturbation(variant.swapEdges()),
            TIME_LIMIT_NANOS,
            seed,
            variant.t0(),
            variant.tmin(),
            COOLING
        );
    }

    private static String[] instancePaths(String[] args) {
        String[] paths = new String[args.length - 2];
        System.arraycopy(args, 2, paths, 0, paths.length);
        return paths;
    }
}
