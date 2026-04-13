package algorithm.construction.twophase;

import algorithm.OptimizationAlgorithm;
import algorithm.improvement.PhaseTwoDelete;
import model.Instance;
import model.Solution;

public final class BestHeuristicSolution implements OptimizationAlgorithm {
    private final GreedyCycle greedyCycle;
    private final PhaseTwoDelete phaseTwoDelete;

    public BestHeuristicSolution() {
        this.greedyCycle = new GreedyCycle();
        this.phaseTwoDelete = new PhaseTwoDelete();
    }

    @Override
    public String name() {
        return "GC+P2D";
    }

    @Override
    public Solution solve(Instance instance, int startVertexId) {
        Solution solution = greedyCycle.solve(instance, startVertexId);
        return phaseTwoDelete.improve(instance, solution);
    }
}
