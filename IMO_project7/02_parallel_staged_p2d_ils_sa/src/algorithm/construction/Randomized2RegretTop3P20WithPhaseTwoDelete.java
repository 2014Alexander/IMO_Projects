package algorithm.construction;

import algorithm.OptimizationAlgorithm;
import algorithm.improvement.PhaseTwoDelete;
import model.Instance;
import model.Solution;

import java.util.Random;

/**
 * Randomized2RegretTop3P20 followed by ordinary PhaseTwoDelete.
 */
public final class Randomized2RegretTop3P20WithPhaseTwoDelete implements OptimizationAlgorithm {
    private final Randomized2RegretTop3P20 construction;
    private final PhaseTwoDelete delete;

    public Randomized2RegretTop3P20WithPhaseTwoDelete() {
        this.construction = new Randomized2RegretTop3P20();
        this.delete = new PhaseTwoDelete();
    }

    public Randomized2RegretTop3P20WithPhaseTwoDelete(Random random) {
        this.construction = new Randomized2RegretTop3P20(random);
        this.delete = new PhaseTwoDelete();
    }

    @Override
    public String name() {
        return "randomized_2regret_top3p20_phaseTwoDelete";
    }

    @Override
    public Solution solve(Instance instance, int startVertexId) {
        Solution constructed = construction.solve(instance, startVertexId);
        return delete.improve(instance, constructed);
    }
}
