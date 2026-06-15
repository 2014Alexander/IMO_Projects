package app;

import algorithm.IterationCountingAlgorithm;
import algorithm.OptimizationAlgorithm;
import algorithm.construction.CheapConsensusR2Top3P30P2DAll10LSTop5Top3;
import algorithm.construction.ConsensusR2Top3P30LSExtractAll10Top3;
import algorithm.construction.Randomized2RegretParamBestNWithPhaseTwoDeleteStart;
import algorithm.localsearch.SteepestLocalSearchWithCandidateMoves;
import algorithm.metaheuristic.CoolingSchedule;
import algorithm.metaheuristic.IteratedLocalSearchWithConstructionStartAndSaAcceptance;
import algorithm.metaheuristic.IteratedLocalSearchWithDiverseDynamicEliteBackboneSaAcceptance;
import algorithm.metaheuristic.IteratedLocalSearchWithDynamicEliteBackboneSaAcceptance;
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

public final class ConsensusDiverseBackboneSaRun {
    private static final long TIME_LIMIT_NANOS = 500_000_000L;
    private static final int DEFAULT_RUNS_COUNT = 10;
    private static final long DEFAULT_BASE_SEED = 12345L;
    private static final double T0 = 300.0;
    private static final double TMIN = 10.0;
    private static final int SWAP_EDGES = 30;
    private static final double PROTECTED_BREAK_PROBABILITY = 0.20;
    private static final CoolingSchedule COOLING = CoolingSchedule.GEOMETRIC;

    private enum Kind {
        TWO_REGRET,
        R2_BEST12,
        CONSENSUS_R2,
        CHEAP_DYNAMIC_OBJECTIVE,
        CHEAP_DIVERSE_MIN_DIST_010,
        CHEAP_DIVERSE_MIN_DIST_015,
        CHEAP_DIVERSE_MIN_DIST_020,
        CHEAP_ARCHIVE5_DIVERSE_TOP3_ALL_COMMON,
        CHEAP_ARCHIVE5_DIVERSE_TOP3_MAJORITY2OF3,
        CHEAP_BEST_FAR_COMPATIBLE
    }

    private record Variant(String name, Kind kind) {}

    public static void main(String[] args) {
        int runsCount = args.length >= 1 ? Integer.parseInt(args[0]) : DEFAULT_RUNS_COUNT;
        long baseSeed = args.length >= 2 ? Long.parseLong(args[1]) : DEFAULT_BASE_SEED;
        String[] instancePaths = args.length >= 3 ? instancePaths(args) : new String[] {"data/TSPA.csv", "data/TSPB.csv"};

        System.out.println("instance,algorithm,runIndex,startVertexId,seed,objective,runtimeNanos,iterationCount,acceptedBetterCount,acceptedWorseCount,rejectedWorseCount,bestFoundIteration,eliteUpdateCount,lastProtectedEdgesCount,minProtectedEdgesCount,maxProtectedEdgesCount,avgProtectedEdgesOnUpdate,swapEdges,cooling,T0,Tmin,protectedBreakProbability,eliteMode,minDistanceThreshold");
        System.out.flush();

        for (String instancePath : instancePaths) {
            Instance instance = new CsvInstanceReader().read(instancePath);
            List<RunConfig> runs = RunPreparator.prepareRuns(instance, baseSeed, runsCount);
            for (Variant variant : variants()) {
                runVariant(instance, runs, variant);
            }
        }
    }

    private static List<Variant> variants() {
        List<Variant> result = new ArrayList<>();
        result.add(new Variant("ILS_2REGRET_P2D_START_SA_GEO_T0_300_TMIN_10", Kind.TWO_REGRET));
        result.add(new Variant("ILS_R2REGRET_TOP3P30_BEST12_P2D_LS_START_SA_GEO_T0_300_TMIN_10", Kind.R2_BEST12));
        result.add(new Variant("ILS_CONSENSUS_R2_TOP3P30_LSEXTRACT_ALL10_TOP3_START_SA_GEO_T0_300_TMIN_10", Kind.CONSENSUS_R2));
        result.add(new Variant("ILS_CHEAP_CONSENSUS_R2_TOP3P30_DYNAMIC_ELITE_BACKBONE_OBJECTIVE_SA_GEO_T0_300_TMIN_10", Kind.CHEAP_DYNAMIC_OBJECTIVE));
        result.add(new Variant("ILS_CHEAP_CONSENSUS_R2_TOP3P30_DYNAMIC_BACKBONE_DIVERSE_MIN_DIST_010_SA_GEO_T0_300_TMIN_10", Kind.CHEAP_DIVERSE_MIN_DIST_010));
        result.add(new Variant("ILS_CHEAP_CONSENSUS_R2_TOP3P30_DYNAMIC_BACKBONE_DIVERSE_MIN_DIST_015_SA_GEO_T0_300_TMIN_10", Kind.CHEAP_DIVERSE_MIN_DIST_015));
        result.add(new Variant("ILS_CHEAP_CONSENSUS_R2_TOP3P30_DYNAMIC_BACKBONE_DIVERSE_MIN_DIST_020_SA_GEO_T0_300_TMIN_10", Kind.CHEAP_DIVERSE_MIN_DIST_020));
        result.add(new Variant("ILS_CHEAP_CONSENSUS_R2_TOP3P30_DYNAMIC_BACKBONE_ARCHIVE5_DIVERSE_TOP3_ALL_COMMON_SA_GEO_T0_300_TMIN_10", Kind.CHEAP_ARCHIVE5_DIVERSE_TOP3_ALL_COMMON));
        result.add(new Variant("ILS_CHEAP_CONSENSUS_R2_TOP3P30_DYNAMIC_BACKBONE_ARCHIVE5_DIVERSE_TOP3_MAJORITY2OF3_SA_GEO_T0_300_TMIN_10", Kind.CHEAP_ARCHIVE5_DIVERSE_TOP3_MAJORITY2OF3));
        result.add(new Variant("ILS_CHEAP_CONSENSUS_R2_TOP3P30_DYNAMIC_BACKBONE_BEST_FAR_COMPATIBLE_SA_GEO_T0_300_TMIN_10", Kind.CHEAP_BEST_FAR_COMPATIBLE));
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

            int eliteUpdateCount = 0;
            int lastProtectedEdgesCount = 0;
            int minProtectedEdgesCount = 0;
            int maxProtectedEdgesCount = 0;
            double avgProtectedEdgesOnUpdate = 0.0;
            if (algorithm instanceof IteratedLocalSearchWithDynamicEliteBackboneSaAcceptance dynamic) {
                eliteUpdateCount = dynamic.eliteUpdateCount();
                lastProtectedEdgesCount = dynamic.lastProtectedEdgesCount();
                minProtectedEdgesCount = dynamic.lastProtectedEdgesCount();
                maxProtectedEdgesCount = dynamic.lastProtectedEdgesCount();
                avgProtectedEdgesOnUpdate = dynamic.lastProtectedEdgesCount();
            } else if (algorithm instanceof IteratedLocalSearchWithDiverseDynamicEliteBackboneSaAcceptance diverse) {
                eliteUpdateCount = diverse.eliteUpdateCount();
                lastProtectedEdgesCount = diverse.lastProtectedEdgesCount();
                minProtectedEdgesCount = diverse.minProtectedEdgesCount();
                maxProtectedEdgesCount = diverse.maxProtectedEdgesCount();
                avgProtectedEdgesOnUpdate = diverse.avgProtectedEdgesOnUpdate();
            }

            EliteDescriptor descriptor = eliteDescriptor(variant.kind());
            System.out.println(instance.name + "," + algorithm.name() + "," + runIndex + "," + run.startVertexId() + "," + run.runSeed() + "," + objective + "," + runtime + "," + iterationCount + "," + stats.acceptedBetterCount() + "," + stats.acceptedWorseCount() + "," + stats.rejectedWorseCount() + "," + stats.bestFoundIteration() + "," + eliteUpdateCount + "," + lastProtectedEdgesCount + "," + minProtectedEdgesCount + "," + maxProtectedEdgesCount + "," + avgProtectedEdgesOnUpdate + "," + SWAP_EDGES + "," + COOLING + "," + T0 + "," + TMIN + "," + PROTECTED_BREAK_PROBABILITY + "," + descriptor.modeName + "," + descriptor.minDistanceThreshold);
            System.out.flush();
        }
    }

    private static OptimizationAlgorithm createAlgorithm(Variant variant, long seed) {
        if (variant.kind() == Kind.TWO_REGRET) {
            return new IteratedLocalSearchWithTwoRegretStartAndSaAcceptance(
                    variant.name(),
                    new SteepestLocalSearchWithCandidateMoves(),
                    new RandomSwapEdgesPerturbation(SWAP_EDGES),
                    TIME_LIMIT_NANOS,
                    seed,
                    T0,
                    TMIN,
                    COOLING
            );
        }

        if (variant.kind() == Kind.CHEAP_DYNAMIC_OBJECTIVE) {
            return new IteratedLocalSearchWithDynamicEliteBackboneSaAcceptance(
                    variant.name(),
                    cheapStart(seed),
                    new SteepestLocalSearchWithCandidateMoves(),
                    TIME_LIMIT_NANOS,
                    seed,
                    T0,
                    TMIN,
                    COOLING,
                    SWAP_EDGES,
                    PROTECTED_BREAK_PROBABILITY
            );
        }

        if (isDiverseBackbone(variant.kind())) {
            EliteDescriptor descriptor = eliteDescriptor(variant.kind());
            return new IteratedLocalSearchWithDiverseDynamicEliteBackboneSaAcceptance(
                    variant.name(),
                    cheapStart(seed),
                    new SteepestLocalSearchWithCandidateMoves(),
                    TIME_LIMIT_NANOS,
                    seed,
                    T0,
                    TMIN,
                    COOLING,
                    SWAP_EDGES,
                    PROTECTED_BREAK_PROBABILITY,
                    descriptor.mode,
                    descriptor.minDistanceThreshold
            );
        }

        return new IteratedLocalSearchWithConstructionStartAndSaAcceptance(
                variant.name(),
                createStartAlgorithm(variant.kind(), variant.name(), seed),
                new SteepestLocalSearchWithCandidateMoves(),
                new RandomSwapEdgesPerturbation(SWAP_EDGES),
                TIME_LIMIT_NANOS,
                seed,
                T0,
                TMIN,
                COOLING
        );
    }

    private static OptimizationAlgorithm createStartAlgorithm(Kind kind, String name, long seed) {
        return switch (kind) {
            case R2_BEST12 -> new Randomized2RegretParamBestNWithPhaseTwoDeleteStart(
                    name + "_START", 3, 0.30, 12, seed, new SteepestLocalSearchWithCandidateMoves());
            case CONSENSUS_R2 -> new ConsensusR2Top3P30LSExtractAll10Top3(seed);
            case TWO_REGRET -> throw new IllegalStateException("TWO_REGRET has separate ILS class");
            case CHEAP_DYNAMIC_OBJECTIVE,
                 CHEAP_DIVERSE_MIN_DIST_010,
                 CHEAP_DIVERSE_MIN_DIST_015,
                 CHEAP_DIVERSE_MIN_DIST_020,
                 CHEAP_ARCHIVE5_DIVERSE_TOP3_ALL_COMMON,
                 CHEAP_ARCHIVE5_DIVERSE_TOP3_MAJORITY2OF3,
                 CHEAP_BEST_FAR_COMPATIBLE -> cheapStart(seed);
        };
    }

    private static OptimizationAlgorithm cheapStart(long seed) {
        return new CheapConsensusR2Top3P30P2DAll10LSTop5Top3(seed);
    }

    private static boolean isDiverseBackbone(Kind kind) {
        return kind == Kind.CHEAP_DIVERSE_MIN_DIST_010
                || kind == Kind.CHEAP_DIVERSE_MIN_DIST_015
                || kind == Kind.CHEAP_DIVERSE_MIN_DIST_020
                || kind == Kind.CHEAP_ARCHIVE5_DIVERSE_TOP3_ALL_COMMON
                || kind == Kind.CHEAP_ARCHIVE5_DIVERSE_TOP3_MAJORITY2OF3
                || kind == Kind.CHEAP_BEST_FAR_COMPATIBLE;
    }

    private static EliteDescriptor eliteDescriptor(Kind kind) {
        return switch (kind) {
            case CHEAP_DIVERSE_MIN_DIST_010 -> new EliteDescriptor(
                    IteratedLocalSearchWithDiverseDynamicEliteBackboneSaAcceptance.EliteMode.DIVERSE_MIN_DIST,
                    "DIVERSE_MIN_DIST", 0.10);
            case CHEAP_DIVERSE_MIN_DIST_015 -> new EliteDescriptor(
                    IteratedLocalSearchWithDiverseDynamicEliteBackboneSaAcceptance.EliteMode.DIVERSE_MIN_DIST,
                    "DIVERSE_MIN_DIST", 0.15);
            case CHEAP_DIVERSE_MIN_DIST_020 -> new EliteDescriptor(
                    IteratedLocalSearchWithDiverseDynamicEliteBackboneSaAcceptance.EliteMode.DIVERSE_MIN_DIST,
                    "DIVERSE_MIN_DIST", 0.20);
            case CHEAP_ARCHIVE5_DIVERSE_TOP3_ALL_COMMON -> new EliteDescriptor(
                    IteratedLocalSearchWithDiverseDynamicEliteBackboneSaAcceptance.EliteMode.ARCHIVE5_DIVERSE_TOP3_ALL_COMMON,
                    "ARCHIVE5_DIVERSE_TOP3_ALL_COMMON", 0.0);
            case CHEAP_ARCHIVE5_DIVERSE_TOP3_MAJORITY2OF3 -> new EliteDescriptor(
                    IteratedLocalSearchWithDiverseDynamicEliteBackboneSaAcceptance.EliteMode.ARCHIVE5_DIVERSE_TOP3_MAJORITY2OF3,
                    "ARCHIVE5_DIVERSE_TOP3_MAJORITY2OF3", 0.0);
            case CHEAP_BEST_FAR_COMPATIBLE -> new EliteDescriptor(
                    IteratedLocalSearchWithDiverseDynamicEliteBackboneSaAcceptance.EliteMode.BEST_FAR_COMPATIBLE,
                    "BEST_FAR_COMPATIBLE", 0.0);
            case CHEAP_DYNAMIC_OBJECTIVE -> new EliteDescriptor(null, "OBJECTIVE_TOP3", 0.0);
            default -> new EliteDescriptor(null, "NONE", 0.0);
        };
    }

    private static String[] instancePaths(String[] args) {
        String[] paths = new String[args.length - 2];
        System.arraycopy(args, 2, paths, 0, paths.length);
        return paths;
    }

    private record EliteDescriptor(
            IteratedLocalSearchWithDiverseDynamicEliteBackboneSaAcceptance.EliteMode mode,
            String modeName,
            double minDistanceThreshold
    ) {}
}
