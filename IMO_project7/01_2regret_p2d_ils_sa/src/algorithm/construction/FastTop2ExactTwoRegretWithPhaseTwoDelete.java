package algorithm.construction;

import algorithm.OptimizationAlgorithm;
import algorithm.improvement.PhaseTwoDelete;
import model.Instance;
import model.Solution;

/**
 * Szybka dokladna konstrukcja 2-regret zakonczona PhaseTwoDelete.
 */
public final class FastTop2ExactTwoRegretWithPhaseTwoDelete implements OptimizationAlgorithm {
    private final FastTop2ExactTwoRegretCost fastTwoRegret;
    private final PhaseTwoDelete phaseTwoDelete;

    public FastTop2ExactTwoRegretWithPhaseTwoDelete() {
        this.fastTwoRegret = new FastTop2ExactTwoRegretCost();
        this.phaseTwoDelete = new PhaseTwoDelete();
    }

    @Override
    public String name() {
        return "FastTop2Exact2Regret_P2D";
    }

    @Override
    public Solution solve(Instance instance, int startVertexId) {
        Solution constructed = fastTwoRegret.solve(instance, startVertexId);
        return phaseTwoDelete.improve(instance, constructed);
    }
}
