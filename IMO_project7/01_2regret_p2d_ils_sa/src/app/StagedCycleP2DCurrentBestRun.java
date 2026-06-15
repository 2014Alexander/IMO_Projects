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

/** Compare current exact dynamic pool10 vs staged Cycle-P2D exact construction in the current best algorithm. */
public final class StagedCycleP2DCurrentBestRun {
    private static final long TIME_LIMIT_NANOS = 500_000_000L;
    private static final int DEFAULT_RUNS_COUNT = 20;
    private static final long DEFAULT_BASE_SEED = 20260614L;
    private static final CoolingSchedule COOLING = CoolingSchedule.GEOMETRIC;
    private static final int ARCHIVE_CAPACITY = 3;
    private static final int MIN_EDGE_FREQUENCY = 3;
    private static final int TARGET_POOL_SIZE = 10;
    private static final double T0 = 500.0;
    private static final double TMIN = 10.0;
    private static final int SWAPS = 30;
    private static final double BREAK_PROBABILITY = 0.20;

    private record Variant(String name, DynamicSeedReseedCheapConsensusParameterized.Mode mode) {}

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
                new Variant("CURRENT_EXACT_POOL10_T0_500", DynamicSeedReseedCheapConsensusParameterized.Mode.EXACT_DYNAMIC_RESEED),
                new Variant("STAGED_CYCLE_P2D_EXACT_POOL10_T0_500", DynamicSeedReseedCheapConsensusParameterized.Mode.EXACT_DYNAMIC_RESEED_STAGED_CYCLE_P2D)
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

            System.out.println(instance.name + "," + algorithm.name() + "," + runIndex + "," + run.startVertexId() + "," + run.runSeed() + "," + objective + "," + runtime + "," + iterationCount + "," + stats.acceptedBetterCount() + "," + stats.acceptedWorseCount() + "," + stats.rejectedWorseCount() + "," + stats.bestFoundIteration() + "," + frequency.eliteUpdateCount() + "," + frequency.lastProtectedEdgesCount() + "," + frequency.minProtectedEdgesCount() + "," + frequency.maxProtectedEdgesCount() + "," + frequency.avgProtectedEdgesOnUpdate() + "," + SWAPS + "," + COOLING + "," + T0 + "," + TMIN + "," + BREAK_PROBABILITY + "," + ARCHIVE_CAPACITY + "," + MIN_EDGE_FREQUENCY + "," + TARGET_POOL_SIZE);
            System.out.flush();
        }
    }

    private static OptimizationAlgorithm createAlgorithm(Variant variant, long seed) {
        return new IteratedLocalSearchWithFrequencyBackboneSaAcceptance(
                "ILS_EXACT_DYNAMIC_VERTEX_RESEED_POOL10_" + variant.name(),
                new DynamicSeedReseedCheapConsensusParameterized(
                        "ILS_EXACT_DYNAMIC_VERTEX_RESEED_POOL10_" + variant.name() + "_START",
                        variant.mode(),
                        TARGET_POOL_SIZE,
                        seed
                ),
                new SteepestLocalSearchWithCandidateMoves(),
                TIME_LIMIT_NANOS,
                seed,
                T0,
                TMIN,
                COOLING,
                SWAPS,
                ARCHIVE_CAPACITY,
                MIN_EDGE_FREQUENCY,
                BREAK_PROBABILITY,
                false
        );
    }

    private static String[] instancePaths(String[] args, int offset) {
        String[] paths = new String[args.length - offset];
        System.arraycopy(args, offset, paths, 0, paths.length);
        return paths;
    }
}
