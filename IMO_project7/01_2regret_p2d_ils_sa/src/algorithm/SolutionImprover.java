package algorithm;

import model.Instance;
import model.Solution;

public interface SolutionImprover {
    Solution improve(Instance instance, Solution solution);
}
