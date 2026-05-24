package algorithm.evolution;

import algorithm.CycleImprover;
import algorithm.SolutionObjective;
import algorithm.construction.RandomSolution;
import algorithm.evolution.diversity.PopulationDiversityPolicy;
import algorithm.localsearch.Cycle;
import algorithm.similarity.SolutionFeatures;
import algorithm.similarity.SolutionFeaturesBuilder;
import model.Instance;
import model.Solution;

/**
 * Tworzy poczatkowa populacje HAE z lokalnych optimow.
 */
public final class PopulationInitializer {
    private static final int ATTEMPT_MULTIPLIER = 1000;

    private final RandomSolution randomSolution;
    private final CycleImprover localSearch;
    private final PopulationDiversityPolicy diversityPolicy;
    private final SolutionFeaturesBuilder featuresBuilder;

    /**
     * Tworzy inicjalizator populacji.
     */
    public PopulationInitializer(
        RandomSolution randomSolution,
        CycleImprover localSearch,
        PopulationDiversityPolicy diversityPolicy
    ) {
        this.randomSolution = randomSolution;
        this.localSearch = localSearch;
        this.diversityPolicy = diversityPolicy;
        this.featuresBuilder = new SolutionFeaturesBuilder();
    }

    /**
     * Buduje populacje poczatkowa o zadanym rozmiarze.
     */
    public ElitePopulation initialize(
        Instance instance,
        int startVertexId,
        int populationSize
    ) {
        ElitePopulation population = new ElitePopulation(populationSize, diversityPolicy);
        int maxAttempts = populationSize * ATTEMPT_MULTIPLIER;
        int attempts = 0;

        while (population.size() < populationSize && attempts < maxAttempts) {
            int currentStartVertexId = (startVertexId + attempts) % instance.size;
            Solution start = randomSolution.solve(instance, currentStartVertexId);
            Cycle cycle = new Cycle(start.cycle(), instance.size);
            localSearch.improve(instance, cycle);

            int objective = SolutionObjective.calculate(instance, cycle);
            SolutionFeatures features = featuresBuilder.build(instance, cycle);
            population.addInitial(new EvolutionIndividual(cycle, objective, features));

            attempts++;
        }

        if (population.size() < populationSize) {
            throw new IllegalStateException("Nie udalo sie zbudowac dostatecznie roznorodnej populacji HAE.");
        }

        return population;
    }
}
