package faircomparison;

import algorithm.OptimizationAlgorithm;
import algorithm.localsearch.SteepestLocalSearchWithCandidateMoves;
import algorithm.metaheuristic.CoolingSchedule;
import algorithm.metaheuristic.IteratedLocalSearchWithConstructionStartAndSaAcceptance;
import algorithm.metaheuristic.ParallelIteratedLocalSearchWithConstructionStartAndSaAcceptance;
import algorithm.metaheuristic.perturbation.RandomSwapEdgesPerturbation;

/** Definicje wariantów STAGED_P2D_15_35_55_75 + ILS-SA. */
public record ParallelStagedIlsSaDefinition(String name, AlgorithmFactory factory) {
    public interface AlgorithmFactory {
        OptimizationAlgorithm create(long runSeed, long timeLimitNanos);
    }

    private static final CoolingSchedule COOLING = CoolingSchedule.GEOMETRIC;
    private static final double T0 = 500.0;
    private static final double TMIN = 10.0;
    private static final int SWAPS = 30;

    public static ParallelStagedIlsSaDefinition singleThread() {
        String name = "STAGED_P2D_15_35_55_75_ILS_SA_1_THREAD";
        return new ParallelStagedIlsSaDefinition(name, (runSeed, timeLimitNanos) ->
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

    public static ParallelStagedIlsSaDefinition parallel(int threadCount) {
        String name = "STAGED_P2D_15_35_55_75_ILS_SA_" + threadCount + "_THREADS";
        return new ParallelStagedIlsSaDefinition(name, (runSeed, timeLimitNanos) ->
                new ParallelIteratedLocalSearchWithConstructionStartAndSaAcceptance(
                        name,
                        () -> new StagedCycleP2D15355575Start("LAB_CYCLE_STAGED_P2D_15_35_55_75"),
                        SteepestLocalSearchWithCandidateMoves::new,
                        () -> new RandomSwapEdgesPerturbation(SWAPS),
                        timeLimitNanos,
                        runSeed,
                        T0,
                        TMIN,
                        COOLING,
                        threadCount
                ));
    }

    public static java.util.List<ParallelStagedIlsSaDefinition> defaultAlgorithms() {
        return java.util.List.of(
                singleThread(),
                parallel(2),
                parallel(4),
                parallel(8)
        );
    }
}
