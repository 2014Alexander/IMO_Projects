package algorithm.construction;

import algorithm.CycleImprover;
import algorithm.OptimizationAlgorithm;
import algorithm.SolutionObjective;
import algorithm.localsearch.Cycle;
import model.Instance;
import model.Solution;

import java.util.Random;

/**
 * Start ILS: kilka zrandomizowanych konstrukcji 2-regret z tej samej wierzchołka startowego.
 * Każda konstrukcja przechodzi PhaseTwoDelete oraz LS, a dalej wybierany jest najlepszy lokalny optimum.
 */
public final class Randomized2RegretBestNWithPhaseTwoDeleteStart implements OptimizationAlgorithm {
    private final String name;
    private final boolean top5;
    private final int triesCount;
    private final long seed;
    private final CycleImprover localSearch;

    public Randomized2RegretBestNWithPhaseTwoDeleteStart(
        String name,
        boolean top5,
        int triesCount,
        long seed,
        CycleImprover localSearch
    ) {
        this.name = name;
        this.top5 = top5;
        this.triesCount = triesCount;
        this.seed = seed;
        this.localSearch = localSearch;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Solution solve(Instance instance, int startVertexId) {
        Random seedRandom = new Random(seed);
        Cycle bestCycle = null;
        int bestObjective = Integer.MIN_VALUE;

        for (int i = 0; i < triesCount; i++) {
            OptimizationAlgorithm construction = top5
                ? new Randomized2RegretTop5P20WithPhaseTwoDelete(new Random(seedRandom.nextLong()))
                : new Randomized2RegretTop3P20WithPhaseTwoDelete(new Random(seedRandom.nextLong()));

            Solution solution = construction.solve(instance, startVertexId);
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
