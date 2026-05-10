package experiment.lab3;

import algorithm.OptimizationPipeline;
import algorithm.construction.RandomSolution;
import algorithm.construction.TwoRegretCost;
import algorithm.improvement.PhaseTwoDelete;
import algorithm.localsearch.NeighborhoodType;
import algorithm.localsearch.SteepestLocalSearch;
import algorithm.localsearch.SteepestLocalSearchWithCandidateMoves;
import algorithm.localsearch.SteepestLocalSearchWithMoveList;
import algorithm.localsearch.candidate.CandidateEdges;
import experiment.core.TestedAlgorithm;

import java.util.List;

/**
 * Zawiera skład bieżącego eksperymentu dla laboratorium 3.
 */
public final class Lab3Scenario {
    private static final int RUNS_COUNT = 100;

    private final List<TestedAlgorithm> testedAlgorithms = List.of(
        new TestedAlgorithm(
            "2-Regret+P2D",
            (name, run) -> OptimizationPipeline
                .startWith(name, new TwoRegretCost())
                .then(new PhaseTwoDelete())
        ),
        new TestedAlgorithm(
            "SteepestLS_RandomStart_SWAP_EDGES",
            (name, run) -> OptimizationPipeline
                .startWith(name, new RandomSolution(run.runSeed()))
                .then(new SteepestLocalSearch(NeighborhoodType.SWAP_EDGES))
        ),
        new TestedAlgorithm(
            "SteepestLS_LM_RandomStart_SWAP_EDGES",
            (name, run) -> OptimizationPipeline
                .startWith(name, new RandomSolution(run.runSeed()))
                .then(new SteepestLocalSearchWithMoveList())
        ),
        new TestedAlgorithm(
            "SteepestLS_CM_RandomStart_SWAP_EDGES",
            (name, run) -> OptimizationPipeline
                .startWith(name, new RandomSolution(run.runSeed()))
                .then(new SteepestLocalSearchWithCandidateMoves(
                    CandidateEdges.DEFAULT_CANDIDATE_COUNT
                ))
        )
    );

    /**
     * Zwraca liczbę uruchomień każdego algorytmu dla pojedynczej instancji.
     *
     * @return liczba uruchomień
     */
    public int runsCount() {
        return RUNS_COUNT;
    }

    /**
     * Zwraca listę algorytmów porównywanych w eksperymencie.
     *
     * @return lista testowanych algorytmów
     */
    public List<TestedAlgorithm> testedAlgorithms() {
        return testedAlgorithms;
    }
}
