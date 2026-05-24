package algorithm.evolution.diversity;

import algorithm.evolution.EvolutionIndividual;

import java.util.List;

/**
 * Polityka roznorodnosci populacji HAE.
 */
public interface PopulationDiversityPolicy {

    /**
     * Decyduje, czy kandydat jest dostatecznie rozny od osobnikow populacji.
     */
    boolean isDifferent(EvolutionIndividual candidate, List<EvolutionIndividual> population);
}
