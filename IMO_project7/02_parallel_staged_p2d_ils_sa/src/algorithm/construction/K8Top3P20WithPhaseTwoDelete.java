package algorithm.construction;

import algorithm.OptimizationAlgorithm;
import algorithm.improvement.PhaseTwoDelete;
import model.Instance;
import model.Solution;

import java.util.Random;

/**
 * Gotowy wariant: k8_top3p20 + zwykły PhaseTwoDelete.
 */
public final class K8Top3P20WithPhaseTwoDelete implements OptimizationAlgorithm {
    private final K8Top3P20Lookahead construction;
    private final PhaseTwoDelete delete;

    public K8Top3P20WithPhaseTwoDelete() {
        this(new Random());
    }

    public K8Top3P20WithPhaseTwoDelete(Random random) {
        this.construction = new K8Top3P20Lookahead(random);
        this.delete = new PhaseTwoDelete();
    }

    @Override
    public String name() {
        return "k8_top3p20_phaseTwoDelete";
    }

    @Override
    public Solution solve(Instance instance, int startVertexId) {
        Solution constructed = construction.solve(instance, startVertexId);
        return delete.improve(instance, constructed);
    }
}
