package experiment.lab5;

import algorithm.OptimizationPipeline;
import algorithm.construction.RandomSolution;
import algorithm.localsearch.GreedyLocalSearch;
import algorithm.localsearch.NeighborhoodType;
import experiment.core.TestedAlgorithm;
import experiment.lab4.Lab4Scenario;

import java.util.ArrayList;
import java.util.List;

public final class Lab5Scenario {
    private static final int LOCAL_OPTIMA_COUNT = 1000;
    private static final NeighborhoodType NEIGHBORHOOD = NeighborhoodType.SWAP_EDGES;

    public int localOptimaCount() {
        return LOCAL_OPTIMA_COUNT;
    }

    public String localSearchName() {
        return GreedyLocalSearch.class.getSimpleName();
    }

    public String neighborhoodName() {
        return NEIGHBORHOOD.name();
    }

    public TestedAlgorithm localOptimaAlgorithm() {
        return new TestedAlgorithm(
            "GREEDY_LS_LOCAL_OPTIMUM",
            (name, run) -> OptimizationPipeline.startWith(name, new RandomSolution(run.runSeed()))
                .then(new GreedyLocalSearch(NEIGHBORHOOD, run.runSeed()))
        );
    }

    public Lab4Scenario bestSolutionScenario() {
        return new Lab4Scenario();
    }

    public List<String> bestSolutionSelectionPoolAlgorithms(long timedAlgorithmsLimitNanos) {
        Lab4Scenario lab4Scenario = bestSolutionScenario();
        List<String> names = new ArrayList<>();
        names.add(lab4Scenario.mslsAlgorithm().name());
        for (TestedAlgorithm algorithm : lab4Scenario.timedAlgorithms(timedAlgorithmsLimitNanos)) {
            names.add(algorithm.name());
        }
        return List.copyOf(names);
    }
}
