package algorithm.evolution.diversity;

import algorithm.evolution.EvolutionIndividual;
import algorithm.similarity.CanonicalCycleSignature;

import java.util.List;

/**
 * Traktuje identyczne kanoniczne cykle jako kopie.
 */
public final class CanonicalCycleDiversityPolicy implements PopulationDiversityPolicy {

    @Override
    public boolean isDifferent(EvolutionIndividual candidate, List<EvolutionIndividual> population) {
        CanonicalCycleSignature candidateSignature = CanonicalCycleSignature.from(candidate.cycle());

        for (EvolutionIndividual individual : population) {
            CanonicalCycleSignature signature = CanonicalCycleSignature.from(individual.cycle());
            if (candidateSignature.equals(signature)) {
                return false;
            }
        }

        return true;
    }
}
