package algorithm.construction;

import algorithm.OptimizationAlgorithm;
import algorithm.improvement.PhaseTwoDelete;
import model.Instance;
import model.Solution;

import java.util.Random;

/**
 * Fast randomized 2-regret with topK/probability parameters, followed by PhaseTwoDelete.
 */
public final class FastRandomized2RegretParameterizedWithPhaseTwoDelete implements OptimizationAlgorithm {
    private final String name;
    private final int topK;
    private final double explorationProbability;
    private final Random random;

    public FastRandomized2RegretParameterizedWithPhaseTwoDelete(
            String name,
            int topK,
            double explorationProbability,
            Random random
    ) {
        this.name = name;
        this.topK = topK;
        this.explorationProbability = explorationProbability;
        this.random = random;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Solution solve(Instance instance, int startVertexId) {
        Solution constructed = new FastRandomized2RegretTopKProbability(
                name + "_construction",
                topK,
                explorationProbability,
                random
        ).solve(instance, startVertexId);
        return new PhaseTwoDelete().improve(instance, constructed);
    }
}
