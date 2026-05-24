package algorithm.evolution;

import algorithm.localsearch.Cycle;
import algorithm.similarity.SolutionFeatures;

/**
 * Osobnik populacji HAE.
 */
public final class EvolutionIndividual {
    private final Cycle cycle;
    private final int objective;
    private final SolutionFeatures features;

    /**
     * Tworzy osobnika populacji.
     */
    public EvolutionIndividual(
        Cycle cycle,
        int objective,
        SolutionFeatures features
    ) {
        this.cycle = cycle;
        this.objective = objective;
        this.features = features;
    }

    /**
     * Zwraca cykl osobnika.
     */
    public Cycle cycle() {
        return cycle;
    }

    /**
     * Zwraca wartosc funkcji celu osobnika.
     */
    public int objective() {
        return objective;
    }

    /**
     * Zwraca strukturalne cechy osobnika.
     */
    public SolutionFeatures features() {
        return features;
    }
}
