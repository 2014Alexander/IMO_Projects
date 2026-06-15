package faircomparison;

import algorithm.OptimizationAlgorithm;
import algorithm.construction.FastTop2ExactTwoRegretWithPhaseTwoDelete;
import algorithm.localsearch.SteepestLocalSearchWithCandidateMoves;
import algorithm.metaheuristic.CoolingSchedule;
import algorithm.metaheuristic.IteratedLocalSearchWithConstructionStartAndSaAcceptance;
import algorithm.metaheuristic.perturbation.RandomSwapEdgesPerturbation;

/** Factory for fair ILS-SA variants that differ only by construction start algorithm. */
public record IlsSaStartVariantDefinition(String name, AlgorithmFactory factory) {
    public interface AlgorithmFactory {
        OptimizationAlgorithm create(long runSeed, long timeLimitNanos);
    }

    private static final CoolingSchedule COOLING = CoolingSchedule.GEOMETRIC;
    private static final double T0 = 500.0;
    private static final double TMIN = 10.0;
    private static final int SWAPS = 30;

    public static IlsSaStartVariantDefinition fullscanTwoRegretP2D() {
        String name = "LAB_ILS_SA_START_FULLSCAN_2REGRET_P2D_T500";
        return new IlsSaStartVariantDefinition(name, (runSeed, timeLimitNanos) ->
                new IteratedLocalSearchWithConstructionStartAndSaAcceptance(
                        name,
                        new OriginalTwoRegretP2DStart("LAB_CYCLE_FULLSCAN_2REGRET_P2D"),
                        new SteepestLocalSearchWithCandidateMoves(),
                        new RandomSwapEdgesPerturbation(SWAPS),
                        timeLimitNanos,
                        runSeed,
                        T0,
                        TMIN,
                        COOLING
                ));
    }

    public static IlsSaStartVariantDefinition fastTop2ExactTwoRegretP2D() {
        String name = "LAB_ILS_SA_START_FAST_TOP2_EXACT_2REGRET_P2D_T500";
        return new IlsSaStartVariantDefinition(name, (runSeed, timeLimitNanos) ->
                new IteratedLocalSearchWithConstructionStartAndSaAcceptance(
                        name,
                        new FastTop2ExactTwoRegretWithPhaseTwoDelete(),
                        new SteepestLocalSearchWithCandidateMoves(),
                        new RandomSwapEdgesPerturbation(SWAPS),
                        timeLimitNanos,
                        runSeed,
                        T0,
                        TMIN,
                        COOLING
                ));
    }

    public static IlsSaStartVariantDefinition stagedP2D15355575() {
        String name = "LAB_ILS_SA_START_STAGED_P2D_15_35_55_75_T500";
        return new IlsSaStartVariantDefinition(name, (runSeed, timeLimitNanos) ->
                new IteratedLocalSearchWithConstructionStartAndSaAcceptance(
                        name,
                        new StagedCycleP2D15355575Start("LAB_CYCLE_STAGED_P2D_15_35_55_75"),
                        new SteepestLocalSearchWithCandidateMoves(),
                        new RandomSwapEdgesPerturbation(SWAPS),
                        timeLimitNanos,
                        runSeed,
                        T0,
                        TMIN,
                        COOLING
                ));
    }

    public static java.util.List<IlsSaStartVariantDefinition> defaultAlgorithms() {
        return java.util.List.of(
                fullscanTwoRegretP2D(),
                fastTop2ExactTwoRegretP2D(),
                stagedP2D15355575()
        );
    }
}
