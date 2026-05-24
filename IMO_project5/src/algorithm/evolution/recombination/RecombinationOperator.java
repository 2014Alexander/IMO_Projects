package algorithm.evolution.recombination;

import algorithm.evolution.EvolutionIndividual;
import algorithm.localsearch.Cycle;
import model.Instance;

import java.util.Random;

/**
 * Operator rekombinacji dwoch rodzicow HAE.
 */
public interface RecombinationOperator {

    /**
     * Tworzy jeden czesciowy cykl potomny wynikajacy z semantyki operatora.
     */
    Cycle recombine(
        Instance instance,
        EvolutionIndividual firstParent,
        EvolutionIndividual secondParent,
        Random random
    );
}
