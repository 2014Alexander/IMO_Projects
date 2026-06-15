package algorithm.metaheuristic.perturbation;

import algorithm.localsearch.Cycle;
import model.Instance;

import java.util.Random;

public interface IlsPerturbation {
    void perturb(Instance instance, Cycle cycle, Random random);
}
