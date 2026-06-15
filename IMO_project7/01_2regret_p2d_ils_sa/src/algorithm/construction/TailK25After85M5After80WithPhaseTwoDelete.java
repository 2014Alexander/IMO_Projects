package algorithm.construction;

import algorithm.OptimizationAlgorithm;
import algorithm.improvement.PhaseTwoDelete;
import model.Instance;
import model.Solution;

/**
 * Gotowy wariant: tailK25after85_m5after80 + zwykły PhaseTwoDelete.
 */
public final class TailK25After85M5After80WithPhaseTwoDelete implements OptimizationAlgorithm {
    private final TailK25After85M5After80Lookahead construction;
    private final PhaseTwoDelete delete;

    public TailK25After85M5After80WithPhaseTwoDelete() {
        this.construction = new TailK25After85M5After80Lookahead();
        this.delete = new PhaseTwoDelete();
    }

    @Override
    public String name() {
        return "tailK25after85_m5after80_phaseTwoDelete";
    }

    @Override
    public Solution solve(Instance instance, int startVertexId) {
        Solution constructed = construction.solve(instance, startVertexId);
        return delete.improve(instance, constructed);
    }
}
