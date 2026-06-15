package algorithm.construction;

import algorithm.OptimizationAlgorithm;
import algorithm.improvement.PhaseTwoDelete;
import model.Instance;
import model.Solution;

import java.util.Random;

/**
 * Zrandomizowany 2-regret z parametrami, zakonczony druga faza usuwania.
 */
public final class Randomized2RegretParameterizedWithPhaseTwoDelete implements OptimizationAlgorithm {
    private final String name;
    private final int topK;
    private final double explorationProbability;
    private final Random random;

    public Randomized2RegretParameterizedWithPhaseTwoDelete(
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
        Solution constructed = new Randomized2RegretTopKProbability(
                name + "_construction",
                topK,
                explorationProbability,
                random
        ).solve(instance, startVertexId);
        return new PhaseTwoDelete().improve(instance, constructed);
    }
}
