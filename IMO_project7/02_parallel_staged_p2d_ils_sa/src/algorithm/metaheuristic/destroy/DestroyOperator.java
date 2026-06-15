package algorithm.metaheuristic.destroy;

import algorithm.localsearch.Cycle;
import model.Instance;

import java.util.Random;

public interface DestroyOperator {
    void destroy(Instance instance, Cycle cycle, Random random);
}
