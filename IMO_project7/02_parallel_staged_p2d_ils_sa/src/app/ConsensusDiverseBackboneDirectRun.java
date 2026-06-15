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
import io.CsvInstanceReader;
import model.Instance;
import model.Solution;

public final class ConsensusDiverseBackboneDirectRun {
    private static final long TIME_LIMIT_NANOS = 500_000_000L;
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

    public static void main(String[] args) {
        Kind kind = Kind.valueOf(args[0]);
        String instancePath = args[1];
        int runIndex = Integer.parseInt(args[2]);
        int startVertexId = Integer.parseInt(args[3]);
        long seed = Long.parseLong(args[4]);
        Instance instance = new CsvInstanceReader().read(instancePath);
        String name = name(kind);
        OptimizationAlgorithm algorithm = createAlgorithm(kind, name, seed);
        long start = System.nanoTime();
        Solution solution = algorithm.solve(instance, startVertexId);
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
        EliteDescriptor descriptor = eliteDescriptor(kind);
        System.out.println(instance.name + "," + algorithm.name() + "," + runIndex + "," + startVertexId + "," + seed + "," + objective + "," + runtime + "," + iterationCount + "," + stats.acceptedBetterCount() + "," + stats.acceptedWorseCount() + "," + stats.rejectedWorseCount() + "," + stats.bestFoundIteration() + "," + eliteUpdateCount + "," + lastProtectedEdgesCount + "," + minProtectedEdgesCount + "," + maxProtectedEdgesCount + "," + avgProtectedEdgesOnUpdate + "," + SWAP_EDGES + "," + COOLING + "," + T0 + "," + TMIN + "," + PROTECTED_BREAK_PROBABILITY + "," + descriptor.modeName + "," + descriptor.minDistanceThreshold);
    }

    private static String name(Kind kind) {
        return switch (kind) {
            case TWO_REGRET -> "ILS_2REGRET_P2D_START_SA_GEO_T0_300_TMIN_10";
            case R2_BEST12 -> "ILS_R2REGRET_TOP3P30_BEST12_P2D_LS_START_SA_GEO_T0_300_TMIN_10";
            case CONSENSUS_R2 -> "ILS_CONSENSUS_R2_TOP3P30_LSEXTRACT_ALL10_TOP3_START_SA_GEO_T0_300_TMIN_10";
            case CHEAP_DYNAMIC_OBJECTIVE -> "ILS_CHEAP_CONSENSUS_R2_TOP3P30_DYNAMIC_ELITE_BACKBONE_OBJECTIVE_SA_GEO_T0_300_TMIN_10";
            case CHEAP_DIVERSE_MIN_DIST_010 -> "ILS_CHEAP_CONSENSUS_R2_TOP3P30_DYNAMIC_BACKBONE_DIVERSE_MIN_DIST_010_SA_GEO_T0_300_TMIN_10";
            case CHEAP_DIVERSE_MIN_DIST_015 -> "ILS_CHEAP_CONSENSUS_R2_TOP3P30_DYNAMIC_BACKBONE_DIVERSE_MIN_DIST_015_SA_GEO_T0_300_TMIN_10";
            case CHEAP_DIVERSE_MIN_DIST_020 -> "ILS_CHEAP_CONSENSUS_R2_TOP3P30_DYNAMIC_BACKBONE_DIVERSE_MIN_DIST_020_SA_GEO_T0_300_TMIN_10";
            case CHEAP_ARCHIVE5_DIVERSE_TOP3_ALL_COMMON -> "ILS_CHEAP_CONSENSUS_R2_TOP3P30_DYNAMIC_BACKBONE_ARCHIVE5_DIVERSE_TOP3_ALL_COMMON_SA_GEO_T0_300_TMIN_10";
            case CHEAP_ARCHIVE5_DIVERSE_TOP3_MAJORITY2OF3 -> "ILS_CHEAP_CONSENSUS_R2_TOP3P30_DYNAMIC_BACKBONE_ARCHIVE5_DIVERSE_TOP3_MAJORITY2OF3_SA_GEO_T0_300_TMIN_10";
            case CHEAP_BEST_FAR_COMPATIBLE -> "ILS_CHEAP_CONSENSUS_R2_TOP3P30_DYNAMIC_BACKBONE_BEST_FAR_COMPATIBLE_SA_GEO_T0_300_TMIN_10";
        };
    }

    private static OptimizationAlgorithm createAlgorithm(Kind kind, String name, long seed) {
        if (kind == Kind.TWO_REGRET) {
            return new IteratedLocalSearchWithTwoRegretStartAndSaAcceptance(name, new SteepestLocalSearchWithCandidateMoves(), new RandomSwapEdgesPerturbation(SWAP_EDGES), TIME_LIMIT_NANOS, seed, T0, TMIN, COOLING);
        }
        if (kind == Kind.CHEAP_DYNAMIC_OBJECTIVE) {
            return new IteratedLocalSearchWithDynamicEliteBackboneSaAcceptance(name, cheapStart(seed), new SteepestLocalSearchWithCandidateMoves(), TIME_LIMIT_NANOS, seed, T0, TMIN, COOLING, SWAP_EDGES, PROTECTED_BREAK_PROBABILITY);
        }
        if (isDiverseBackbone(kind)) {
            EliteDescriptor descriptor = eliteDescriptor(kind);
            return new IteratedLocalSearchWithDiverseDynamicEliteBackboneSaAcceptance(name, cheapStart(seed), new SteepestLocalSearchWithCandidateMoves(), TIME_LIMIT_NANOS, seed, T0, TMIN, COOLING, SWAP_EDGES, PROTECTED_BREAK_PROBABILITY, descriptor.mode, descriptor.minDistanceThreshold);
        }
        return new IteratedLocalSearchWithConstructionStartAndSaAcceptance(name, createStartAlgorithm(kind, name, seed), new SteepestLocalSearchWithCandidateMoves(), new RandomSwapEdgesPerturbation(SWAP_EDGES), TIME_LIMIT_NANOS, seed, T0, TMIN, COOLING);
    }

    private static OptimizationAlgorithm createStartAlgorithm(Kind kind, String name, long seed) {
        return switch (kind) {
            case R2_BEST12 -> new Randomized2RegretParamBestNWithPhaseTwoDeleteStart(name + "_START", 3, 0.30, 12, seed, new SteepestLocalSearchWithCandidateMoves());
            case CONSENSUS_R2 -> new ConsensusR2Top3P30LSExtractAll10Top3(seed);
            case TWO_REGRET -> throw new IllegalStateException("TWO_REGRET has separate ILS class");
            case CHEAP_DYNAMIC_OBJECTIVE, CHEAP_DIVERSE_MIN_DIST_010, CHEAP_DIVERSE_MIN_DIST_015, CHEAP_DIVERSE_MIN_DIST_020, CHEAP_ARCHIVE5_DIVERSE_TOP3_ALL_COMMON, CHEAP_ARCHIVE5_DIVERSE_TOP3_MAJORITY2OF3, CHEAP_BEST_FAR_COMPATIBLE -> cheapStart(seed);
        };
    }

    private static OptimizationAlgorithm cheapStart(long seed) {
        return new CheapConsensusR2Top3P30P2DAll10LSTop5Top3(seed);
    }

    private static boolean isDiverseBackbone(Kind kind) {
        return kind == Kind.CHEAP_DIVERSE_MIN_DIST_010 || kind == Kind.CHEAP_DIVERSE_MIN_DIST_015 || kind == Kind.CHEAP_DIVERSE_MIN_DIST_020 || kind == Kind.CHEAP_ARCHIVE5_DIVERSE_TOP3_ALL_COMMON || kind == Kind.CHEAP_ARCHIVE5_DIVERSE_TOP3_MAJORITY2OF3 || kind == Kind.CHEAP_BEST_FAR_COMPATIBLE;
    }

    private static EliteDescriptor eliteDescriptor(Kind kind) {
        return switch (kind) {
            case CHEAP_DIVERSE_MIN_DIST_010 -> new EliteDescriptor(IteratedLocalSearchWithDiverseDynamicEliteBackboneSaAcceptance.EliteMode.DIVERSE_MIN_DIST, "DIVERSE_MIN_DIST", 0.10);
            case CHEAP_DIVERSE_MIN_DIST_015 -> new EliteDescriptor(IteratedLocalSearchWithDiverseDynamicEliteBackboneSaAcceptance.EliteMode.DIVERSE_MIN_DIST, "DIVERSE_MIN_DIST", 0.15);
            case CHEAP_DIVERSE_MIN_DIST_020 -> new EliteDescriptor(IteratedLocalSearchWithDiverseDynamicEliteBackboneSaAcceptance.EliteMode.DIVERSE_MIN_DIST, "DIVERSE_MIN_DIST", 0.20);
            case CHEAP_ARCHIVE5_DIVERSE_TOP3_ALL_COMMON -> new EliteDescriptor(IteratedLocalSearchWithDiverseDynamicEliteBackboneSaAcceptance.EliteMode.ARCHIVE5_DIVERSE_TOP3_ALL_COMMON, "ARCHIVE5_DIVERSE_TOP3_ALL_COMMON", 0.0);
            case CHEAP_ARCHIVE5_DIVERSE_TOP3_MAJORITY2OF3 -> new EliteDescriptor(IteratedLocalSearchWithDiverseDynamicEliteBackboneSaAcceptance.EliteMode.ARCHIVE5_DIVERSE_TOP3_MAJORITY2OF3, "ARCHIVE5_DIVERSE_TOP3_MAJORITY2OF3", 0.0);
            case CHEAP_BEST_FAR_COMPATIBLE -> new EliteDescriptor(IteratedLocalSearchWithDiverseDynamicEliteBackboneSaAcceptance.EliteMode.BEST_FAR_COMPATIBLE, "BEST_FAR_COMPATIBLE", 0.0);
            case CHEAP_DYNAMIC_OBJECTIVE -> new EliteDescriptor(null, "OBJECTIVE_TOP3", 0.0);
            default -> new EliteDescriptor(null, "NONE", 0.0);
        };
    }

    private record EliteDescriptor(IteratedLocalSearchWithDiverseDynamicEliteBackboneSaAcceptance.EliteMode mode, String modeName, double minDistanceThreshold) {}
}
