package app;

import algorithm.IterationCountingAlgorithm;
import algorithm.OptimizationAlgorithm;
import algorithm.construction.DynamicSeedReseedCheapConsensusParameterized;
import algorithm.construction.RepairReseedCheapConsensusFastR2Top3P30Parameterized;
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

/** Tests deterministic exact seed-reseed and hybrid anchor seed scheduler variants. */
public final class ExactSeedReseedTestsRun {
    private static final long TIME_LIMIT_NANOS = 500_000_000L;
    private static final int DEFAULT_RUNS_COUNT = 20;
    private static final long DEFAULT_BASE_SEED = 20260613L;
    private static final double T0 = 300.0;
    private static final double TMIN = 10.0;
    private static final int SWAP_EDGES = 30;
    private static final double BREAK_PROBABILITY = 0.20;
    private static final CoolingSchedule COOLING = CoolingSchedule.GEOMETRIC;
    private static final int ARCHIVE_CAPACITY = 3;
    private static final int MIN_EDGE_FREQUENCY = 3;

    private enum Kind {
        BASELINE_FAST_R2_VERTEX_RESEED_TOP2,
        HYBRID_ANCHOR_FAST_RESEED_POOL9,
        HYBRID_ANCHOR_FAST_RESEED_POOL11,
        EXACT_DYNAMIC_RESEED_POOL10,
        EXACT_DYNAMIC_RESEED_POOL12
    }

    private record Variant(String name, Kind kind, int targetPoolSize) {}

    public static void main(String[] args) {
        int runsCount = args.length >= 1 ? Integer.parseInt(args[0]) : DEFAULT_RUNS_COUNT;
        long baseSeed = args.length >= 2 ? Long.parseLong(args[1]) : DEFAULT_BASE_SEED;
        String variantFilter = args.length >= 3 && !args[2].endsWith(".csv") ? args[2] : null;
        int pathOffset = variantFilter == null ? 2 : 3;
        String[] instancePaths = args.length > pathOffset ? instancePaths(args, pathOffset) : new String[] {"data/TSPA.csv", "data/TSPB.csv"};

        System.out.println("instance,algorithm,runIndex,startVertexId,seed,objective,runtimeNanos,iterationCount,acceptedBetterCount,acceptedWorseCount,rejectedWorseCount,bestFoundIteration,eliteUpdateCount,lastProtectedEdgesCount,minProtectedEdgesCount,maxProtectedEdgesCount,avgProtectedEdgesOnUpdate,swapEdges,cooling,T0,Tmin,breakProbability,archiveCapacity,minEdgeFrequency,targetPoolSize,variantKind");
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
                new Variant("ILS_CHEAP_CONSENSUS_FAST_R2_TOP3P30_VERTEX_RESEED_TOP2_LSTOP5_OR_BEST_PARENT_DYNAMIC_FIXED", Kind.BASELINE_FAST_R2_VERTEX_RESEED_TOP2, 12),
                new Variant("ILS_HYBRID_EXACT_ANCHOR_FAST_R2_DYNAMIC_RESEED_POOL9_LSTOP5_OR_BEST_PARENT_DYNAMIC_FIXED", Kind.HYBRID_ANCHOR_FAST_RESEED_POOL9, 9),
                new Variant("ILS_HYBRID_EXACT_ANCHOR_FAST_R2_DYNAMIC_RESEED_POOL11_LSTOP5_OR_BEST_PARENT_DYNAMIC_FIXED", Kind.HYBRID_ANCHOR_FAST_RESEED_POOL11, 11),
                new Variant("ILS_EXACT_DYNAMIC_VERTEX_RESEED_POOL10_LSTOP5_OR_BEST_PARENT_DYNAMIC_FIXED", Kind.EXACT_DYNAMIC_RESEED_POOL10, 10),
                new Variant("ILS_EXACT_DYNAMIC_VERTEX_RESEED_POOL12_LSTOP5_OR_BEST_PARENT_DYNAMIC_FIXED", Kind.EXACT_DYNAMIC_RESEED_POOL12, 12)
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

            System.out.println(instance.name + "," + algorithm.name() + "," + runIndex + "," + run.startVertexId() + "," + run.runSeed() + "," + objective + "," + runtime + "," + iterationCount + "," + stats.acceptedBetterCount() + "," + stats.acceptedWorseCount() + "," + stats.rejectedWorseCount() + "," + stats.bestFoundIteration() + "," + frequency.eliteUpdateCount() + "," + frequency.lastProtectedEdgesCount() + "," + frequency.minProtectedEdgesCount() + "," + frequency.maxProtectedEdgesCount() + "," + frequency.avgProtectedEdgesOnUpdate() + "," + SWAP_EDGES + "," + COOLING + "," + T0 + "," + TMIN + "," + BREAK_PROBABILITY + "," + ARCHIVE_CAPACITY + "," + MIN_EDGE_FREQUENCY + "," + variant.targetPoolSize() + "," + variant.kind());
            System.out.flush();
        }
    }

    private static OptimizationAlgorithm createAlgorithm(Variant variant, long seed) {
        return new IteratedLocalSearchWithFrequencyBackboneSaAcceptance(
                variant.name(),
                startAlgorithm(variant, seed),
                new SteepestLocalSearchWithCandidateMoves(),
                TIME_LIMIT_NANOS,
                seed,
                T0,
                TMIN,
                COOLING,
                SWAP_EDGES,
                ARCHIVE_CAPACITY,
                MIN_EDGE_FREQUENCY,
                BREAK_PROBABILITY,
                false
        );
    }

    private static OptimizationAlgorithm startAlgorithm(Variant variant, long seed) {
        return switch (variant.kind()) {
            case BASELINE_FAST_R2_VERTEX_RESEED_TOP2 -> new RepairReseedCheapConsensusFastR2Top3P30Parameterized(
                    variant.name() + "_START",
                    RepairReseedCheapConsensusFastR2Top3P30Parameterized.Mode.VERTEX_RESEED_TOP3,
                    seed,
                    2
            );
            case HYBRID_ANCHOR_FAST_RESEED_POOL9, HYBRID_ANCHOR_FAST_RESEED_POOL11 -> new DynamicSeedReseedCheapConsensusParameterized(
                    variant.name() + "_START",
                    DynamicSeedReseedCheapConsensusParameterized.Mode.HYBRID_ANCHOR_FAST_RESEED,
                    variant.targetPoolSize(),
                    seed
            );
            case EXACT_DYNAMIC_RESEED_POOL10, EXACT_DYNAMIC_RESEED_POOL12 -> new DynamicSeedReseedCheapConsensusParameterized(
                    variant.name() + "_START",
                    DynamicSeedReseedCheapConsensusParameterized.Mode.EXACT_DYNAMIC_RESEED,
                    variant.targetPoolSize(),
                    seed
            );
        };
    }

    private static String[] instancePaths(String[] args, int offset) {
        String[] paths = new String[args.length - offset];
        System.arraycopy(args, offset, paths, 0, paths.length);
        return paths;
    }
}
