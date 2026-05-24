package algorithm.metaheuristic.repair;

import algorithm.localsearch.Cycle;
import model.Instance;

import java.util.Random;

public interface RepairOperator {
    void repair(Instance instance, Cycle cycle, Random random);
}
