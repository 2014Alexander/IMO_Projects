package app;

import algorithm.IterationCountingAlgorithm;
import algorithm.OptimizationAlgorithm;
import algorithm.construction.CheapConsensusR2Top3P30Parameterized;
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

import java.util.ArrayList;
import java.util.List;

/** Runs repair/reseed variants around CHEAP_CONSENSUS_FAST_R2_TOP3P30 baseline. */
public final class RepairReseedFastR2TestsRun {
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
        BASELINE,
        ALL_REPAIR_PASS,
        TOP3_REPAIR_PASS,
        VERTEX_RESEED_TOP3,
        FRAGMENT_RESEED_BEST
    }

    private record Variant(String name, Kind kind) {}

    public static void main(String[] args) {
        int runsCount = args.length >= 1 ? Integer.parseInt(args[0]) : DEFAULT_RUNS_COUNT;
        long baseSeed = args.length >= 2 ? Long.parseLong(args[1]) : DEFAULT_BASE_SEED;
        String variantFilter = args.length >= 3 && !args[2].endsWith(".csv") ? args[2] : null;
        int pathOffset = variantFilter == null ? 2 : 3;
        String[] instancePaths = args.length > pathOffset ? instancePaths(args, pathOffset) : new String[] {"data/TSPA.csv", "data/TSPB.csv"};

        System.out.println("instance,algorithm,runIndex,startVertexId,seed,objective,runtimeNanos,iterationCount,acceptedBetterCount,acceptedWorseCount,rejectedWorseCount,bestFoundIteration,eliteUpdateCount,lastProtectedEdgesCount,minProtectedEdgesCount,maxProtectedEdgesCount,avgProtectedEdgesOnUpdate,swapEdges,cooling,T0,Tmin,breakProbability,archiveCapacity,minEdgeFrequency,variantKind");
        System.out.flush();

        for (String instancePath : instancePaths) {
            Instance instance = new CsvInstanceReader().read(instancePath);
            List<RunConfig> runs = RunPreparator.prepareRuns(instance, baseSeed, runsCount);
            for (Variant variant : variants()) {
                if (variantFilter != null && !variant.name().equals(variantFilter) && !variant.kind().name().equals(variantFilter)) {
                    continue;
                }
                runVariant(instance, runs, variant);
            }
        }
    }

    private static List<Variant> variants() {
        List<Variant> variants = new ArrayList<>();
        variants.add(new Variant("ILS_CHEAP_CONSENSUS_FAST_R2_TOP3P30_LSTOP5_OR_BEST_PARENT_DYNAMIC_FIXED", Kind.BASELINE));
        variants.add(new Variant("ILS_CHEAP_CONSENSUS_FAST_R2_TOP3P30_ALL_REPAIRPASS_LSTOP5_OR_BEST_PARENT_DYNAMIC_FIXED", Kind.ALL_REPAIR_PASS));
        variants.add(new Variant("ILS_CHEAP_CONSENSUS_FAST_R2_TOP3P30_TOP3_REPAIRPASS_LSTOP5_OR_BEST_PARENT_DYNAMIC_FIXED", Kind.TOP3_REPAIR_PASS));
        variants.add(new Variant("ILS_CHEAP_CONSENSUS_FAST_R2_TOP3P30_VERTEX_RESEED_TOP3_LSTOP5_OR_BEST_PARENT_DYNAMIC_FIXED", Kind.VERTEX_RESEED_TOP3));
        variants.add(new Variant("ILS_CHEAP_CONSENSUS_FAST_R2_TOP3P30_FRAGMENT_RESEED_BEST_LSTOP5_OR_BEST_PARENT_DYNAMIC_FIXED", Kind.FRAGMENT_RESEED_BEST));
        return variants;
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

            int eliteUpdateCount = 0;
            int lastProtectedEdgesCount = 0;
            int minProtectedEdgesCount = 0;
            int maxProtectedEdgesCount = 0;
            double avgProtectedEdgesOnUpdate = 0.0;
            if (algorithm instanceof IteratedLocalSearchWithFrequencyBackboneSaAcceptance frequency) {
                eliteUpdateCount = frequency.eliteUpdateCount();
                lastProtectedEdgesCount = frequency.lastProtectedEdgesCount();
                minProtectedEdgesCount = frequency.minProtectedEdgesCount();
                maxProtectedEdgesCount = frequency.maxProtectedEdgesCount();
                avgProtectedEdgesOnUpdate = frequency.avgProtectedEdgesOnUpdate();
            }

            System.out.println(instance.name + "," + algorithm.name() + "," + runIndex + "," + run.startVertexId() + "," + run.runSeed() + "," + objective + "," + runtime + "," + iterationCount + "," + stats.acceptedBetterCount() + "," + stats.acceptedWorseCount() + "," + stats.rejectedWorseCount() + "," + stats.bestFoundIteration() + "," + eliteUpdateCount + "," + lastProtectedEdgesCount + "," + minProtectedEdgesCount + "," + maxProtectedEdgesCount + "," + avgProtectedEdgesOnUpdate + "," + SWAP_EDGES + "," + COOLING + "," + T0 + "," + TMIN + "," + BREAK_PROBABILITY + "," + ARCHIVE_CAPACITY + "," + MIN_EDGE_FREQUENCY + "," + variant.kind());
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
        if (variant.kind() == Kind.BASELINE) {
            return new CheapConsensusR2Top3P30Parameterized(seed, 5, 3, true, true);
        }
        RepairReseedCheapConsensusFastR2Top3P30Parameterized.Mode mode = switch (variant.kind()) {
            case BASELINE -> throw new IllegalStateException("BASELINE handled separately");
            case ALL_REPAIR_PASS -> RepairReseedCheapConsensusFastR2Top3P30Parameterized.Mode.ALL_REPAIR_PASS;
            case TOP3_REPAIR_PASS -> RepairReseedCheapConsensusFastR2Top3P30Parameterized.Mode.TOP3_REPAIR_PASS_ADD;
            case VERTEX_RESEED_TOP3 -> RepairReseedCheapConsensusFastR2Top3P30Parameterized.Mode.VERTEX_RESEED_TOP3;
            case FRAGMENT_RESEED_BEST -> RepairReseedCheapConsensusFastR2Top3P30Parameterized.Mode.FRAGMENT_RESEED_BEST;
        };
        return new RepairReseedCheapConsensusFastR2Top3P30Parameterized(variant.name() + "_START", mode, seed);
    }

    private static String[] instancePaths(String[] args, int offset) {
        String[] paths = new String[args.length - offset];
        System.arraycopy(args, offset, paths, 0, paths.length);
        return paths;
    }
}
