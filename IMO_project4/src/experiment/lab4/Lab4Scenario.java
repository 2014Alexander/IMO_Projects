package experiment.lab4;

import algorithm.improvement.PhaseTwoDelete;
import algorithm.localsearch.SteepestLocalSearchWithCandidateMoves;
import algorithm.localsearch.candidate.CandidateEdges;
import algorithm.metaheuristic.IteratedLocalSearch;
import algorithm.metaheuristic.LargeNeighborhoodSearch;
import algorithm.metaheuristic.LargeNeighborhoodSearchWithoutLocalSearch;
import algorithm.metaheuristic.MultipleStartLocalSearch;
import algorithm.metaheuristic.destroy.SegmentDestroyOperator;
import algorithm.metaheuristic.perturbation.RandomSwapEdgesPerturbation;
import algorithm.metaheuristic.repair.TwoRegretRepairOperator;
import experiment.core.TestedAlgorithm;
import experiment.summary.ExperimentParameter;

import java.util.List;

/**
 * Zawiera sklad eksperymentu dla laboratorium 4.
 */
public final class Lab4Scenario {
    private static final int RUNS_COUNT = 20;
    private static final int MSLS_STARTS_COUNT = 200;

    /**
     * Zwraca liczbe niezaleznych uruchomien kazdej metody.
     *
     * @return liczba uruchomien
     */
    public int runsCount() {
        return RUNS_COUNT;
    }

    /**
     * Zwraca liczbe startow lokalnego przeszukiwania w jednym uruchomieniu MSLS.
     *
     * @return liczba startow MSLS
     */
    public int mslsStartsCount() {
        return MSLS_STARTS_COUNT;
    }

    /**
     * Zwraca konfiguracje algorytmu MSLS.
     *
     * @return algorytm MSLS
     */
    public TestedAlgorithm mslsAlgorithm() {
        return new TestedAlgorithm(
            "MSLS",
            (name, run) -> new MultipleStartLocalSearch(
                name,
                new SteepestLocalSearchWithCandidateMoves(),
                MSLS_STARTS_COUNT,
                run.runSeed()
            )
        );
    }

    /**
     * Zwraca algorytmy uruchamiane z limitem czasu wyznaczonym z MSLS.
     *
     * @param timeLimitNanos limit czasu w nanosekundach
     * @return lista algorytmow zaleznch od limitu czasu
     */
    public List<TestedAlgorithm> timedAlgorithms(long timeLimitNanos) {
        return List.of(
            new TestedAlgorithm(
                "ILS",
                (name, run) -> new IteratedLocalSearch(
                    name,
                    new SteepestLocalSearchWithCandidateMoves(),
                    new RandomSwapEdgesPerturbation(),
                    timeLimitNanos,
                    run.runSeed()
                )
            ),
            new TestedAlgorithm(
                "LNS",
                (name, run) -> new LargeNeighborhoodSearch(
                    name,
                    new SteepestLocalSearchWithCandidateMoves(),
                    new SegmentDestroyOperator(),
                    new TwoRegretRepairOperator(),
                    timeLimitNanos,
                    run.runSeed()
                )
            ),
            new TestedAlgorithm(
                "LNSa",
                (name, run) -> new LargeNeighborhoodSearchWithoutLocalSearch(
                    name,
                    new SteepestLocalSearchWithCandidateMoves(),
                    new SegmentDestroyOperator(),
                    new TwoRegretRepairOperator(),
                    timeLimitNanos,
                    run.runSeed()
                )
            )
        );
    }

    /**
     * Buduje metadane opisujace konfiguracje eksperymentu lab4.
     *
     * @param mslsAverageRuntimeNanos sredni czas MSLS w nanosekundach
     * @param timedAlgorithmsLimitNanos limit czasu dla ILS, LNS i LNSa
     * @return lista parametrow eksperymentu
     */
    public List<ExperimentParameter> parameters(
        double mslsAverageRuntimeNanos,
        long timedAlgorithmsLimitNanos
    ) {
        return List.of(
            new ExperimentParameter("runsCount", Integer.toString(RUNS_COUNT)),
            new ExperimentParameter("mslsStartsCount", Integer.toString(MSLS_STARTS_COUNT)),
            new ExperimentParameter("mslsAverageRuntimeNanos", Double.toString(mslsAverageRuntimeNanos)),
            new ExperimentParameter("timedAlgorithmsLimitNanos", Long.toString(timedAlgorithmsLimitNanos)),
            new ExperimentParameter("localSearch", SteepestLocalSearchWithCandidateMoves.class.getSimpleName()),
            new ExperimentParameter("candidateEdgesCount", Integer.toString(CandidateEdges.DEFAULT_CANDIDATE_COUNT)),
            new ExperimentParameter("ilsPerturbation", RandomSwapEdgesPerturbation.class.getSimpleName()),
            new ExperimentParameter(
                "ilsPerturbationMoves",
                Integer.toString(RandomSwapEdgesPerturbation.DEFAULT_MOVES_COUNT)
            ),
            new ExperimentParameter("lnsDestroy", SegmentDestroyOperator.class.getSimpleName()),
            new ExperimentParameter(
                "lnsDestroyRatio",
                Double.toString(SegmentDestroyOperator.DEFAULT_DESTROY_RATIO)
            ),
            new ExperimentParameter(
                "lnsSegmentsCount",
                Integer.toString(SegmentDestroyOperator.DEFAULT_SEGMENTS_COUNT)
            ),
            new ExperimentParameter(
                "lnsSegmentCandidateListSize",
                Integer.toString(SegmentDestroyOperator.DEFAULT_CANDIDATE_LIST_SIZE)
            ),
            new ExperimentParameter("repair", TwoRegretRepairOperator.class.getSimpleName()),
            new ExperimentParameter("repairFinalImprover", PhaseTwoDelete.class.getSimpleName())
        );
    }
}
