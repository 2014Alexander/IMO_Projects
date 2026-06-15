package algorithm.construction;

import algorithm.OptimizationAlgorithm;
import algorithm.improvement.PhaseTwoDelete;
import model.Instance;
import model.Solution;

/**
 * Gotowy wariant: k12_m3_std + zwykły PhaseTwoDelete.
 */
public final class K12M3StdWithPhaseTwoDelete implements OptimizationAlgorithm {
    private final K12M3StdLookahead construction;
    private final PhaseTwoDelete delete;

    public K12M3StdWithPhaseTwoDelete() {
        this.construction = new K12M3StdLookahead();
        this.delete = new PhaseTwoDelete();
    }

    @Override
    public String name() {
        return "k12_m3_std_phaseTwoDelete";
    }

    @Override
    public Solution solve(Instance instance, int startVertexId) {
        Solution constructed = construction.solve(instance, startVertexId);
        return delete.improve(instance, constructed);
    }
}
