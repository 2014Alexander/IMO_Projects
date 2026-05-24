package experiment.lab6;

import algorithm.OptimizationPipeline;
import algorithm.construction.RandomSolution;
import algorithm.construction.TwoRegretCost;
import algorithm.evolution.HybridEvolutionaryAlgorithm;
import algorithm.evolution.diversity.ObjectiveDiversityPolicy;
import algorithm.evolution.recombination.CommonEdgesAndVerticesRecombination;
import algorithm.evolution.recombination.CommonPartsRecombination;
import algorithm.evolution.recombination.CommonVerticesRecombination;
import algorithm.improvement.PhaseTwoDelete;
import algorithm.localsearch.SteepestLocalSearchWithCandidateMoves;
import algorithm.localsearch.candidate.CandidateEdges;
import algorithm.metaheuristic.IteratedLocalSearch;
import algorithm.metaheuristic.LargeNeighborhoodSearch;
import algorithm.metaheuristic.MultipleStartLocalSearch;
import algorithm.metaheuristic.destroy.SegmentDestroyOperator;
import algorithm.metaheuristic.perturbation.RandomSwapEdgesPerturbation;
import algorithm.metaheuristic.repair.TwoRegretRepairOperator;
import experiment.core.TestedAlgorithm;
import experiment.summary.ExperimentParameter;

import java.util.List;

/**
 * Zawiera sklad eksperymentu dla laboratorium 6.
 */
public final class Lab6Scenario {
    private static final int RUNS_COUNT = 20;
    private static final int MSLS_STARTS_COUNT = 200;

    public int runsCount() {
        return RUNS_COUNT;
    }

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

    public List<TestedAlgorithm> mainTimedAlgorithms(long timeLimitNanos) {
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
            haeAlgorithm("HAE_OP1_LS", timeLimitNanos, new CommonPartsRecombination(), true),
            haeAlgorithm("HAE_OP2_LS", timeLimitNanos, new CommonEdgesAndVerticesRecombination(), true),
            haeAlgorithm("HAE_OP2_NO_LS", timeLimitNanos, new CommonEdgesAndVerticesRecombination(), false),
            haeAlgorithm("HAE_OP3_LS", timeLimitNanos, new CommonVerticesRecombination(), true),
            haeAlgorithm("HAE_OP3_NO_LS", timeLimitNanos, new CommonVerticesRecombination(), false)
        );
    }

    public List<TestedAlgorithm> referenceAlgorithms() {
        return List.of(
            new TestedAlgorithm(
                "2REGRET_P2D",
                (name, run) -> OptimizationPipeline.startWith(name, new TwoRegretCost())
                    .then(new PhaseTwoDelete())
            ),
            new TestedAlgorithm(
                "BASE_LS",
                (name, run) -> OptimizationPipeline.startWith(name, new RandomSolution(run.runSeed()))
                    .then(new SteepestLocalSearchWithCandidateMoves())
            )
        );
    }

    public List<ExperimentParameter> parameters(double mslsAverageRuntimeNanos, long timeLimitNanos) {
        return List.of(
            new ExperimentParameter("runsCount", Integer.toString(RUNS_COUNT)),
            new ExperimentParameter("mslsStartsCount", Integer.toString(MSLS_STARTS_COUNT)),
            new ExperimentParameter("mslsAverageRuntimeNanos", Double.toString(mslsAverageRuntimeNanos)),
            new ExperimentParameter("timedAlgorithmsLimitNanos", Long.toString(timeLimitNanos)),
            new ExperimentParameter("haePopulationSize", Integer.toString(HybridEvolutionaryAlgorithm.DEFAULT_POPULATION_SIZE)),
            new ExperimentParameter("localSearch", SteepestLocalSearchWithCandidateMoves.class.getSimpleName()),
            new ExperimentParameter("candidateEdgesCount", Integer.toString(CandidateEdges.DEFAULT_CANDIDATE_COUNT)),
            new ExperimentParameter("repair", TwoRegretRepairOperator.class.getSimpleName()),
            new ExperimentParameter("repairFinalImprover", PhaseTwoDelete.class.getSimpleName()),
            new ExperimentParameter("diversityPolicy", ObjectiveDiversityPolicy.class.getSimpleName()),
            new ExperimentParameter("ilsPerturbation", RandomSwapEdgesPerturbation.class.getSimpleName()),
            new ExperimentParameter("lnsDestroy", SegmentDestroyOperator.class.getSimpleName())
        );
    }

    private static TestedAlgorithm haeAlgorithm(
        String name,
        long timeLimitNanos,
        algorithm.evolution.recombination.RecombinationOperator recombination,
        boolean improveAfterRecombination
    ) {
        return new TestedAlgorithm(
            name,
            (algorithmName, run) -> new HybridEvolutionaryAlgorithm(
                algorithmName,
                new SteepestLocalSearchWithCandidateMoves(),
                recombination,
                new TwoRegretRepairOperator(),
                new ObjectiveDiversityPolicy(),
                timeLimitNanos,
                improveAfterRecombination,
                run.runSeed()
            )
        );
    }
}
