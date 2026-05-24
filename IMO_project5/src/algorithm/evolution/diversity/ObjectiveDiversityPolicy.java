package algorithm.evolution.diversity;

import algorithm.evolution.EvolutionIndividual;

import java.util.List;

/**
 * Traktuje rozwiazania o tej samej wartosci objective jako kopie.
 */
public final class ObjectiveDiversityPolicy implements PopulationDiversityPolicy {

    @Override
    public boolean isDifferent(EvolutionIndividual candidate, List<EvolutionIndividual> population) {
        int candidateObjective = candidate.objective();

        for (EvolutionIndividual individual : population) {
            if (individual.objective() == candidateObjective) {
                return false;
            }
        }

        return true;
    }
}
