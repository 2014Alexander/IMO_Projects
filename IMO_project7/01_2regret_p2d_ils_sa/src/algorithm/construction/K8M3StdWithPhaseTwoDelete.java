package algorithm.construction;

import algorithm.OptimizationAlgorithm;
import algorithm.improvement.PhaseTwoDelete;
import model.Instance;
import model.Solution;

/**
 * Gotowy wariant: k8_m3_std + zwykły PhaseTwoDelete.
 */
public final class K8M3StdWithPhaseTwoDelete implements OptimizationAlgorithm {
    private final K8M3StdLookahead construction;
    private final PhaseTwoDelete delete;

    public K8M3StdWithPhaseTwoDelete() {
        this.construction = new K8M3StdLookahead();
        this.delete = new PhaseTwoDelete();
    }

    @Override
    public String name() {
        return "k8_m3_std_phaseTwoDelete";
    }

    @Override
    public Solution solve(Instance instance, int startVertexId) {
        Solution constructed = construction.solve(instance, startVertexId);
        return delete.improve(instance, constructed);
    }
}
