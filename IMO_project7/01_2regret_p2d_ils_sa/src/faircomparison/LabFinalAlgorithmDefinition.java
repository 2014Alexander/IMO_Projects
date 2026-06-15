package faircomparison;

import algorithm.OptimizationAlgorithm;
import algorithm.construction.FastTop2ExactTwoRegretWithPhaseTwoDelete;
import algorithm.localsearch.SteepestLocalSearchWithCandidateMoves;
import algorithm.metaheuristic.CoolingSchedule;
import algorithm.metaheuristic.IteratedLocalSearch;
import algorithm.metaheuristic.IteratedLocalSearchWithConstructionStartAndSaAcceptance;
import algorithm.metaheuristic.IteratedLocalSearchWithSaAcceptance;
import algorithm.metaheuristic.perturbation.RandomSwapEdgesPerturbation;

/**
 * Final-lab fair benchmark definitions.
 *
 * Every factory creates a fresh independent algorithm instance for one run config.
 * Construction-only variants ignore the timeLimitNanos argument.
 */
public record LabFinalAlgorithmDefinition(String name, AlgorithmFactory factory, boolean timeLimited) {
    public interface AlgorithmFactory {
        OptimizationAlgorithm create(long runSeed, long timeLimitNanos);
    }

    private static final CoolingSchedule COOLING = CoolingSchedule.GEOMETRIC;
    private static final double T0 = 500.0;
    private static final double TMIN = 10.0;
    private static final int SWAPS = 30;

    public static LabFinalAlgorithmDefinition fullscanTwoRegretP2D() {
        String name = "2R_P2D";
        return new LabFinalAlgorithmDefinition(name,
                (runSeed, timeLimitNanos) -> new OriginalTwoRegretP2DStart(name),
                false);
    }

    public static LabFinalAlgorithmDefinition fastTop2ExactTwoRegretP2D() {
        String name = "2R_FAST_P2D";
        return new LabFinalAlgorithmDefinition(name,
                (runSeed, timeLimitNanos) -> new FastTop2ExactTwoRegretWithPhaseTwoDelete(),
                false);
    }

    public static LabFinalAlgorithmDefinition stagedP2D15355575() {
        String name = "2R_FAST_P2D_15_35_55_75";
        return new LabFinalAlgorithmDefinition(name,
                (runSeed, timeLimitNanos) -> new StagedCycleP2D15355575Start(name),
                false);
    }

    public static LabFinalAlgorithmDefinition stagedP2D15355575IlsSa() {
        String name = "2R_FAST_P2D_15_35_55_75_ILS_SA";
        return new LabFinalAlgorithmDefinition(name, (runSeed, timeLimitNanos) ->
                new IteratedLocalSearchWithConstructionStartAndSaAcceptance(
                        name,
                        new StagedCycleP2D15355575Start("2R_FAST_P2D_15_35_55_75"),
                        new SteepestLocalSearchWithCandidateMoves(),
                        new RandomSwapEdgesPerturbation(SWAPS),
                        timeLimitNanos,
                        runSeed,
                        T0,
                        TMIN,
                        COOLING),
                true);
    }

    public static LabFinalAlgorithmDefinition fastTop2ExactTwoRegretP2DIlsSa() {
        String name = "2R_FAST_P2D_ILS_SA";
        return new LabFinalAlgorithmDefinition(name, (runSeed, timeLimitNanos) ->
                new IteratedLocalSearchWithConstructionStartAndSaAcceptance(
                        name,
                        new FastTop2ExactTwoRegretWithPhaseTwoDelete(),
                        new SteepestLocalSearchWithCandidateMoves(),
                        new RandomSwapEdgesPerturbation(SWAPS),
                        timeLimitNanos,
                        runSeed,
                        T0,
                        TMIN,
                        COOLING),
                true);
    }

    public static LabFinalAlgorithmDefinition fullscanTwoRegretP2DIlsSa() {
        String name = "2R_P2D_ILS_SA";
        return new LabFinalAlgorithmDefinition(name, (runSeed, timeLimitNanos) ->
                new IteratedLocalSearchWithConstructionStartAndSaAcceptance(
                        name,
                        new OriginalTwoRegretP2DStart("2R_P2D"),
                        new SteepestLocalSearchWithCandidateMoves(),
                        new RandomSwapEdgesPerturbation(SWAPS),
                        timeLimitNanos,
                        runSeed,
                        T0,
                        TMIN,
                        COOLING),
                true);
    }

    public static LabFinalAlgorithmDefinition randomStartIls() {
        String name = "RANDOM_START_ILS";
        return new LabFinalAlgorithmDefinition(name, (runSeed, timeLimitNanos) ->
                new IteratedLocalSearch(
                        name,
                        new SteepestLocalSearchWithCandidateMoves(),
                        new RandomSwapEdgesPerturbation(SWAPS),
                        timeLimitNanos,
                        runSeed),
                true);
    }

    public static LabFinalAlgorithmDefinition randomStartIlsSa() {
        String name = "RANDOM_START_ILS_SA";
        return new LabFinalAlgorithmDefinition(name, (runSeed, timeLimitNanos) ->
                new IteratedLocalSearchWithSaAcceptance(
                        name,
                        new SteepestLocalSearchWithCandidateMoves(),
                        new RandomSwapEdgesPerturbation(SWAPS),
                        timeLimitNanos,
                        runSeed,
                        T0,
                        TMIN),
                true);
    }

    public static java.util.List<LabFinalAlgorithmDefinition> defaultAlgorithms() {
        return java.util.List.of(
                fullscanTwoRegretP2D(),
                fastTop2ExactTwoRegretP2D(),
                stagedP2D15355575(),
                fullscanTwoRegretP2DIlsSa(),
                fastTop2ExactTwoRegretP2DIlsSa(),
                stagedP2D15355575IlsSa(),
                randomStartIls(),
                randomStartIlsSa()
        );
    }
}
