package algorithm.construction;

import algorithm.CycleImprover;
import algorithm.OptimizationAlgorithm;
import algorithm.SolutionObjective;
import algorithm.localsearch.Cycle;
import model.Instance;
import model.Solution;

import java.util.Random;

/**
 * Start ILS: trzy zrandomizowane próby k8_top3p20 z tej samej startowej wierzchołka.
 * Każdy kandydat jest poprawiany LS, a dalej wybierany jest najlepszy.
 */
public final class K8Top3P20BestOf3WithPhaseTwoDeleteStart implements OptimizationAlgorithm {
    private final OptimizationAlgorithm[] randomizedAlgorithms;
    private final CycleImprover localSearch;

    public K8Top3P20BestOf3WithPhaseTwoDeleteStart(long seed, CycleImprover localSearch) {
        Random seedRandom = new Random(seed);
        this.randomizedAlgorithms = new OptimizationAlgorithm[] {
            new K8Top3P20WithPhaseTwoDelete(new Random(seedRandom.nextLong())),
            new K8Top3P20WithPhaseTwoDelete(new Random(seedRandom.nextLong())),
            new K8Top3P20WithPhaseTwoDelete(new Random(seedRandom.nextLong()))
        };
        this.localSearch = localSearch;
    }

    @Override
    public String name() {
        return "k8_top3p20_best_of_3_phaseTwoDelete_start";
    }

    @Override
    public Solution solve(Instance instance, int startVertexId) {
        Cycle bestCycle = null;
        int bestObjective = Integer.MIN_VALUE;

        for (OptimizationAlgorithm algorithm : randomizedAlgorithms) {
            Solution solution = algorithm.solve(instance, startVertexId);
            Cycle cycle = new Cycle(solution.cycle(), instance.size);
            localSearch.improve(instance, cycle);
            int objective = SolutionObjective.calculate(instance, cycle);

            if (bestCycle == null || objective > bestObjective) {
                bestCycle = new Cycle(cycle);
                bestObjective = objective;
            }
        }

        return new Solution(instance.name, startVertexId, bestCycle.toList());
    }
}
