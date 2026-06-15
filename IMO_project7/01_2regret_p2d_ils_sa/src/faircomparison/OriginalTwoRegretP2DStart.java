package faircomparison;

import algorithm.OptimizationAlgorithm;
import algorithm.construction.TwoRegretCost;
import algorithm.improvement.PhaseTwoDelete;
import model.Instance;
import model.Solution;

/** Original lab 2-regret construction followed by PhaseTwoDelete. */
public final class OriginalTwoRegretP2DStart implements OptimizationAlgorithm {
    private final String name;
    private final TwoRegretCost twoRegretCost;
    private final PhaseTwoDelete phaseTwoDelete;

    public OriginalTwoRegretP2DStart(String name) {
        this.name = name;
        this.twoRegretCost = new TwoRegretCost();
        this.phaseTwoDelete = new PhaseTwoDelete();
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Solution solve(Instance instance, int startVertexId) {
        Solution constructed = twoRegretCost.solve(instance, startVertexId);
        return phaseTwoDelete.improve(instance, constructed);
    }
}
