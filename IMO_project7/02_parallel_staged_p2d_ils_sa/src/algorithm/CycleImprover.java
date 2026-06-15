package algorithm;

import algorithm.localsearch.Cycle;
import model.Instance;

public interface CycleImprover {
    void improve(Instance instance, Cycle cycle);
}
