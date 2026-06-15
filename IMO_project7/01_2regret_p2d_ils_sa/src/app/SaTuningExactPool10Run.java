package app;

import algorithm.IterationCountingAlgorithm;
import algorithm.OptimizationAlgorithm;
import algorithm.construction.DynamicSeedReseedCheapConsensusParameterized;
import algorithm.localsearch.SteepestLocalSearchWithCandidateMoves;
import algorithm.metaheuristic.CoolingSchedule;
import algorithm.metaheuristic.IteratedLocalSearchWithFrequencyBackboneSaAcceptance;
import algorithm.metaheuristic.SaAcceptanceStatistics;
import evaluation.SolutionEvaluator;
import experiment.core.RunConfig;
import experiment.core.RunPreparator;
import io.CsvInstanceReader;
import model.Instance;
import model.Solution;

import java.util.List;

/** SA parameter grid for EXACT_DYNAMIC_VERTEX_RESEED_POOL10 baseline. */
public final class SaTuningExactPool10Run {
    private static final long TIME_LIMIT_NANOS = 500_000_000L;
    private static final int DEFAULT_RUNS_COUNT = 20;
    private static final long DEFAULT_BASE_SEED = 20260614L;
    private static final CoolingSchedule COOLING = CoolingSchedule.GEOMETRIC;
    private static final int ARCHIVE_CAPACITY = 3;
    private static final int MIN_EDGE_FREQUENCY = 3;
    private static final int TARGET_POOL_SIZE = 10;

    private record Variant(String name, double t0, double tmin, int swaps, double breakProbability) {}

    public static void main(String[] args) {
        int runsCount = args.length >= 1 ? Integer.parseInt(args[0]) : DEFAULT_RUNS_COUNT;
        long baseSeed = args.length >= 2 ? Long.parseLong(args[1]) : DEFAULT_BASE_SEED;
        String variantFilter = args.length >= 3 && !args[2].endsWith(".csv") ? args[2] : null;
        int pathOffset = variantFilter == null ? 2 : 3;
        String[] instancePaths = args.length > pathOffset ? instancePaths(args, pathOffset) : new String[] {"data/TSPA.csv", "data/TSPB.csv"};

        System.out.println("instance,algorithm,runIndex,startVertexId,seed,objective,runtimeNanos,iterationCount,acceptedBetterCount,acceptedWorseCount,rejectedWorseCount,bestFoundIteration,eliteUpdateCount,lastProtectedEdgesCount,minProtectedEdgesCount,maxProtectedEdgesCount,avgProtectedEdgesOnUpdate,swapEdges,cooling,T0,Tmin,breakProbability,archiveCapacity,minEdgeFrequency,targetPoolSize");
        System.out.flush();

        for (String instancePath : instancePaths) {
            Instance instance = new CsvInstanceReader().read(instancePath);
            List<RunConfig> runs = RunPreparator.prepareRuns(instance, baseSeed, runsCount);
            for (Variant variant : variants()) {
                if (variantFilter != null && !variant.name().equals(variantFilter)) {
                    continue;
                }
                runVariant(instance, runs, variant);
            }
        }
    }

    private static List<Variant> variants() {
        return List.of(
                new Variant("SA_A_T0_200_TMIN_10_SWAPS_30_BREAK_020", 200.0, 10.0, 30, 0.20),
                new Variant("SA_B_T0_300_TMIN_10_SWAPS_30_BREAK_020_CURRENT", 300.0, 10.0, 30, 0.20),
                new Variant("SA_C_T0_500_TMIN_10_SWAPS_30_BREAK_020", 500.0, 10.0, 30, 0.20),
                new Variant("SA_D_T0_300_TMIN_5_SWAPS_30_BREAK_020", 300.0, 5.0, 30, 0.20),
                new Variant("SA_E_T0_300_TMIN_20_SWAPS_30_BREAK_020", 300.0, 20.0, 30, 0.20),
                new Variant("SA_F_T0_300_TMIN_10_SWAPS_20_BREAK_020", 300.0, 10.0, 20, 0.20),
                new Variant("SA_G_T0_300_TMIN_10_SWAPS_40_BREAK_020", 300.0, 10.0, 40, 0.20),
                new Variant("SA_H_T0_300_TMIN_10_SWAPS_30_BREAK_010", 300.0, 10.0, 30, 0.10),
                new Variant("SA_I_T0_300_TMIN_10_SWAPS_30_BREAK_030", 300.0, 10.0, 30, 0.30)
        );
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
            IteratedLocalSearchWithFrequencyBackboneSaAcceptance frequency = (IteratedLocalSearchWithFrequencyBackboneSaAcceptance) algorithm;

            System.out.println(instance.name + "," + algorithm.name() + "," + runIndex + "," + run.startVertexId() + "," + run.runSeed() + "," + objective + "," + runtime + "," + iterationCount + "," + stats.acceptedBetterCount() + "," + stats.acceptedWorseCount() + "," + stats.rejectedWorseCount() + "," + stats.bestFoundIteration() + "," + frequency.eliteUpdateCount() + "," + frequency.lastProtectedEdgesCount() + "," + frequency.minProtectedEdgesCount() + "," + frequency.maxProtectedEdgesCount() + "," + frequency.avgProtectedEdgesOnUpdate() + "," + variant.swaps() + "," + COOLING + "," + variant.t0() + "," + variant.tmin() + "," + variant.breakProbability() + "," + ARCHIVE_CAPACITY + "," + MIN_EDGE_FREQUENCY + "," + TARGET_POOL_SIZE);
            System.out.flush();
        }
    }

    private static OptimizationAlgorithm createAlgorithm(Variant variant, long seed) {
        return new IteratedLocalSearchWithFrequencyBackboneSaAcceptance(
                "ILS_EXACT_DYNAMIC_VERTEX_RESEED_POOL10_" + variant.name(),
                new DynamicSeedReseedCheapConsensusParameterized(
                        "ILS_EXACT_DYNAMIC_VERTEX_RESEED_POOL10_" + variant.name() + "_START",
                        DynamicSeedReseedCheapConsensusParameterized.Mode.EXACT_DYNAMIC_RESEED,
                        TARGET_POOL_SIZE,
                        seed
                ),
                new SteepestLocalSearchWithCandidateMoves(),
                TIME_LIMIT_NANOS,
                seed,
                variant.t0(),
                variant.tmin(),
                COOLING,
                variant.swaps(),
                ARCHIVE_CAPACITY,
                MIN_EDGE_FREQUENCY,
                variant.breakProbability(),
                false
        );
    }

    private static String[] instancePaths(String[] args, int offset) {
        String[] paths = new String[args.length - offset];
        System.arraycopy(args, offset, paths, 0, paths.length);
        return paths;
    }
}
