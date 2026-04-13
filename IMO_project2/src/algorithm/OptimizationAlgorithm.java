package algorithm;

import model.Instance;
import model.Solution;

public interface OptimizationAlgorithm {
    String name();

    Solution solve(Instance instance, int startVertexId);
}