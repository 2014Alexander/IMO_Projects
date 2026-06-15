package faircomparison;

import algorithm.OptimizationAlgorithm;
import algorithm.construction.DynamicSeedReseedCheapConsensusParameterized;
import algorithm.localsearch.SteepestLocalSearchWithCandidateMoves;
import algorithm.metaheuristic.CoolingSchedule;
import algorithm.metaheuristic.IteratedLocalSearch;
import algorithm.metaheuristic.IteratedLocalSearchWithConstructionStartAndSaAcceptance;
import algorithm.metaheuristic.IteratedLocalSearchWithFrequencyBackboneSaAcceptance;
import algorithm.metaheuristic.IteratedLocalSearchWithTwoRegretStart;
import algorithm.metaheuristic.perturbation.RandomSwapEdgesPerturbation;

/** Factory for independent algorithm instances created separately for every run config. */
public record FairAlgorithmDefinition(String name, AlgorithmFactory factory) {
    public interface AlgorithmFactory {
        OptimizationAlgorithm create(long runSeed, long timeLimitNanos);
    }

    private static final CoolingSchedule COOLING = CoolingSchedule.GEOMETRIC;
    private static final double T0 = 500.0;
    private static final double TMIN = 10.0;
    private static final int SWAPS = 30;
    private static final double BREAK_PROBABILITY = 0.20;
    private static final int ARCHIVE_CAPACITY = 3;
    private static final int MIN_EDGE_FREQUENCY = 3;
    private static final int TARGET_POOL_SIZE = 10;

    public static FairAlgorithmDefinition currentBestStagedExactPool10() {
        String name = "CHEAP_CONSENSUS_EXACT_DYNAMIC_VERTEX_RESEED_POOL10_STAGED_CYCLE_P2D_LSTOP5_OR_BEST_PARENT_DYNAMIC_FIXED_SA_T500";
        return new FairAlgorithmDefinition(name, (runSeed, timeLimitNanos) ->
                new IteratedLocalSearchWithFrequencyBackboneSaAcceptance(
                        name,
                        new DynamicSeedReseedCheapConsensusParameterized(
                                name + "_START",
                                DynamicSeedReseedCheapConsensusParameterized.Mode.EXACT_DYNAMIC_RESEED_STAGED_CYCLE_P2D,
                                TARGET_POOL_SIZE,
                                runSeed
                        ),
                        new SteepestLocalSearchWithCandidateMoves(),
                        timeLimitNanos,
                        runSeed,
                        T0,
                        TMIN,
                        COOLING,
                        SWAPS,
                        ARCHIVE_CAPACITY,
                        MIN_EDGE_FREQUENCY,
                        BREAK_PROBABILITY,
                        false
                ));
    }

    public static FairAlgorithmDefinition labIlsRandomStart() {
        String name = "LAB_ILS_RANDOM_START";
        return new FairAlgorithmDefinition(name, (runSeed, timeLimitNanos) ->
                new IteratedLocalSearch(
                        name,
                        new SteepestLocalSearchWithCandidateMoves(),
                        new RandomSwapEdgesPerturbation(SWAPS),
                        timeLimitNanos,
                        runSeed
                ));
    }

    public static FairAlgorithmDefinition labIlsOriginalTwoRegretP2DStart() {
        String name = "LAB_ILS_ORIGINAL_2REGRET_P2D_START";
        return new FairAlgorithmDefinition(name, (runSeed, timeLimitNanos) ->
                new IteratedLocalSearchWithTwoRegretStart(
                        name,
                        new SteepestLocalSearchWithCandidateMoves(),
                        new RandomSwapEdgesPerturbation(SWAPS),
                        timeLimitNanos,
                        runSeed
                ));
    }

    public static FairAlgorithmDefinition labIlsSaOriginalTwoRegretP2DStart() {
        String name = "LAB_ILS_SA_ORIGINAL_2REGRET_P2D_START_T500";
        return new FairAlgorithmDefinition(name, (runSeed, timeLimitNanos) ->
                new IteratedLocalSearchWithConstructionStartAndSaAcceptance(
                        name,
                        new OriginalTwoRegretP2DStart(name + "_START"),
                        new SteepestLocalSearchWithCandidateMoves(),
                        new RandomSwapEdgesPerturbation(SWAPS),
                        timeLimitNanos,
                        runSeed,
                        T0,
                        TMIN,
                        COOLING
                ));
    }

    public static java.util.List<FairAlgorithmDefinition> defaultAlgorithms() {
        return java.util.List.of(
                currentBestStagedExactPool10(),
                labIlsRandomStart(),
                labIlsOriginalTwoRegretP2DStart(),
                labIlsSaOriginalTwoRegretP2DStart()
        );
    }
}
