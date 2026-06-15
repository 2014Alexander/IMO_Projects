package algorithm.construction;

import algorithm.CycleImprover;
import algorithm.OptimizationAlgorithm;
import algorithm.SolutionObjective;
import algorithm.localsearch.Cycle;
import model.Instance;
import model.Solution;

import java.util.Random;

/**
 * Multiple fast randomized 2-regret constructions from the same start vertex.
 * Every construction is followed by PhaseTwoDelete and local search; the best local optimum is returned.
 */
public final class FastRandomized2RegretParamBestNWithPhaseTwoDeleteStart implements OptimizationAlgorithm {
    private final String name;
    private final int topK;
    private final double explorationProbability;
    private final int triesCount;
    private final long seed;
    private final CycleImprover localSearch;

    public FastRandomized2RegretParamBestNWithPhaseTwoDeleteStart(
            String name,
            int topK,
            double explorationProbability,
            int triesCount,
            long seed,
            CycleImprover localSearch
    ) {
        this.name = name;
        this.topK = topK;
        this.explorationProbability = explorationProbability;
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
            OptimizationAlgorithm construction = new FastRandomized2RegretParameterizedWithPhaseTwoDelete(
                    name + "_try" + i,
                    topK,
                    explorationProbability,
                    new Random(seedRandom.nextLong())
            );

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
