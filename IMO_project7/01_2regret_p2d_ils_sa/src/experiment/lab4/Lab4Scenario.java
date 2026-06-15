package experiment.lab4;

import algorithm.localsearch.SteepestLocalSearchWithCandidateMoves;
import algorithm.localsearch.candidate.CandidateEdges;
import algorithm.metaheuristic.IteratedLocalSearch;
import algorithm.metaheuristic.IteratedLocalSearchWithTwoRegretStart;
import algorithm.metaheuristic.IteratedLocalSearchWithSaAcceptance;
import algorithm.metaheuristic.IteratedLocalSearchWithTwoRegretStartAndSaAcceptance;
import algorithm.metaheuristic.perturbation.RandomSwapEdgesPerturbation;
import experiment.core.TestedAlgorithm;
import experiment.summary.ExperimentParameter;

import java.util.List;

/**
 * Zawiera sklad testu ILS i ILS+SA z ustalonym limitem czasu.
 */
public final class Lab4Scenario {
    private static final int RUNS_COUNT = 10;
    private static final long FIXED_TIME_LIMIT_NANOS = 500_000_000L;

    /**
     * Zwraca liczbe niezaleznych uruchomien kazdej metody.
     *
     * @return liczba uruchomien
     */
    public int runsCount() {
        return RUNS_COUNT;
    }

    /**
     * Zwraca staly limit czasu dla kazdego uruchomienia ILS.
     *
     * @return limit czasu w nanosekundach
     */
    public long fixedTimeLimitNanos() {
        return FIXED_TIME_LIMIT_NANOS;
    }

    /**
     * Zwraca porownywane warianty ILS.
     *
     * @return lista algorytmow ILS
     */
    public List<TestedAlgorithm> algorithms() {
        return List.of(
            new TestedAlgorithm(
                "ILS_RANDOM_START",
                (name, run) -> new IteratedLocalSearch(
                    name,
                    new SteepestLocalSearchWithCandidateMoves(),
                    new RandomSwapEdgesPerturbation(),
                    FIXED_TIME_LIMIT_NANOS,
                    run.runSeed()
                )
            ),
            new TestedAlgorithm(
                "ILS_2REGRET_P2D_START",
                (name, run) -> new IteratedLocalSearchWithTwoRegretStart(
                    name,
                    new SteepestLocalSearchWithCandidateMoves(),
                    new RandomSwapEdgesPerturbation(),
                    FIXED_TIME_LIMIT_NANOS,
                    run.runSeed()
                )
            ),
            new TestedAlgorithm(
                "ILS_RANDOM_START_SA_ACCEPT",
                (name, run) -> new IteratedLocalSearchWithSaAcceptance(
                    name,
                    new SteepestLocalSearchWithCandidateMoves(),
                    new RandomSwapEdgesPerturbation(),
                    FIXED_TIME_LIMIT_NANOS,
                    run.runSeed()
                )
            ),
            new TestedAlgorithm(
                "ILS_2REGRET_P2D_START_SA_ACCEPT",
                (name, run) -> new IteratedLocalSearchWithTwoRegretStartAndSaAcceptance(
                    name,
                    new SteepestLocalSearchWithCandidateMoves(),
                    new RandomSwapEdgesPerturbation(),
                    FIXED_TIME_LIMIT_NANOS,
                    run.runSeed()
                )
            )
        );
    }

    /**
     * Buduje metadane opisujace konfiguracje testu.
     *
     * @return lista parametrow eksperymentu
     */
    public List<ExperimentParameter> parameters() {
        return List.of(
            new ExperimentParameter("runsCount", Integer.toString(RUNS_COUNT)),
            new ExperimentParameter("fixedTimeLimitNanos", Long.toString(FIXED_TIME_LIMIT_NANOS)),
            new ExperimentParameter("fixedTimeLimitSeconds", "0.5"),
            new ExperimentParameter("localSearch", SteepestLocalSearchWithCandidateMoves.class.getSimpleName()),
            new ExperimentParameter("candidateEdgesCount", Integer.toString(CandidateEdges.DEFAULT_CANDIDATE_COUNT)),
            new ExperimentParameter("ilsPerturbation", RandomSwapEdgesPerturbation.class.getSimpleName()),
            new ExperimentParameter(
                "ilsPerturbationMoves",
                Integer.toString(RandomSwapEdgesPerturbation.DEFAULT_MOVES_COUNT)
            ),
            new ExperimentParameter(
                "saInitialTemperature",
                Double.toString(IteratedLocalSearchWithSaAcceptance.DEFAULT_INITIAL_TEMPERATURE)
            ),
            new ExperimentParameter(
                "saFinalTemperature",
                Double.toString(IteratedLocalSearchWithSaAcceptance.DEFAULT_FINAL_TEMPERATURE)
            )
        );
    }
}
