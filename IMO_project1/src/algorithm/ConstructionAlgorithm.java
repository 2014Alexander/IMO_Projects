package algorithm;

import model.Instance;
import model.Solution;

public interface ConstructionAlgorithm {
    String name();

    Solution solve(Instance instance, Integer startVertexId);
}