package algorithm.evolution.recombination;

import algorithm.evolution.EvolutionIndividual;
import algorithm.localsearch.Cycle;
import model.Instance;

import java.util.Random;

/**
 * Operator 3: filtruje rodzica bazowego tylko po wspolnych wierzcholkach.
 */
public final class CommonVerticesRecombination implements RecombinationOperator {

    @Override
    public Cycle recombine(
        Instance instance,
        EvolutionIndividual firstParent,
        EvolutionIndividual secondParent,
        Random random
    ) {
        EvolutionIndividual baseParent = random.nextBoolean() ? firstParent : secondParent;
        EvolutionIndividual filterParent = baseParent == firstParent ? secondParent : firstParent;
        Cycle baseCycle = baseParent.cycle();

        Cycle child = new Cycle(instance.size);
        for (int position = 0; position < baseCycle.size(); position++) {
            int vertex = baseCycle.cycle[position];
            if (filterParent.features().hasVertex(vertex)) {
                child.append(vertex);
            }
        }

        return child;
    }
}
