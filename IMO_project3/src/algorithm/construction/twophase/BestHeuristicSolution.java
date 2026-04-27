package algorithm.construction.twophase;

import algorithm.OptimizationAlgorithm;
import algorithm.construction.singlephase.TwoRegretCost;
import algorithm.improvement.PhaseTwoDelete;
import model.Instance;
import model.Solution;

public final class BestHeuristicSolution implements OptimizationAlgorithm {
    private final TwoRegretCost twoRegretCost;
    private final PhaseTwoDelete phaseTwoDelete;

    public BestHeuristicSolution() {
        this.twoRegretCost = new TwoRegretCost();
        this.phaseTwoDelete = new PhaseTwoDelete();
    }

    @Override
    public String name() {
        return "2-Regret+P2D";
    }

    @Override
    public Solution solve(Instance instance, int startVertexId) {
        Solution solution = twoRegretCost.solve(instance, startVertexId);
        return phaseTwoDelete.improve(instance, solution);
    }
}
