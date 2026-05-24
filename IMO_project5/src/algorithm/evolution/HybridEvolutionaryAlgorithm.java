package algorithm.evolution;

import algorithm.CycleImprover;
import algorithm.IterationCountingAlgorithm;
import algorithm.OptimizationAlgorithm;
import algorithm.SolutionObjective;
import algorithm.construction.RandomSolution;
import algorithm.evolution.diversity.PopulationDiversityPolicy;
import algorithm.evolution.repair.RepairSeedAdapter;
import algorithm.evolution.recombination.RecombinationOperator;
import algorithm.localsearch.Cycle;
import algorithm.metaheuristic.repair.RepairOperator;
import algorithm.similarity.SolutionFeatures;
import algorithm.similarity.SolutionFeaturesBuilder;
import model.Instance;
import model.Solution;

import java.util.Random;

/**
 * Hybrydowy algorytm ewolucyjny z populacja elitarna i steady state.
 */
public final class HybridEvolutionaryAlgorithm implements OptimizationAlgorithm, IterationCountingAlgorithm {
    public static final int DEFAULT_POPULATION_SIZE = 20;

    private final String name;
    private final int populationSize;
    private final CycleImprover localSearch;
    private final RecombinationOperator recombination;
    private final RepairOperator repair;
    private final PopulationDiversityPolicy diversityPolicy;
    private final long timeLimitNanos;
    private final boolean improveAfterRecombination;
    private final Random random;
    private final RandomSolution randomSolution;
    private final SolutionFeaturesBuilder featuresBuilder;
    private final RepairSeedAdapter repairSeedAdapter;

    private int lastIterationCount;

    /**
     * Tworzy HAE z domyslnym rozmiarem populacji.
     */
    public HybridEvolutionaryAlgorithm(
        String name,
        CycleImprover localSearch,
        RecombinationOperator recombination,
        RepairOperator repair,
        PopulationDiversityPolicy diversityPolicy,
        long timeLimitNanos,
        boolean improveAfterRecombination,
        long seed
    ) {
        this(
            name,
            DEFAULT_POPULATION_SIZE,
            localSearch,
            recombination,
            repair,
            diversityPolicy,
            timeLimitNanos,
            improveAfterRecombination,
            seed
        );
    }

    /**
     * Tworzy HAE z jawnie podanym rozmiarem populacji.
     */
    public HybridEvolutionaryAlgorithm(
        String name,
        int populationSize,
        CycleImprover localSearch,
        RecombinationOperator recombination,
        RepairOperator repair,
        PopulationDiversityPolicy diversityPolicy,
        long timeLimitNanos,
        boolean improveAfterRecombination,
        long seed
    ) {
        this.name = name;
        this.populationSize = populationSize;
        this.localSearch = localSearch;
        this.recombination = recombination;
        this.repair = repair;
        this.diversityPolicy = diversityPolicy;
        this.timeLimitNanos = timeLimitNanos;
        this.improveAfterRecombination = improveAfterRecombination;
        this.random = new Random(seed);
        this.randomSolution = new RandomSolution(seed);
        this.featuresBuilder = new SolutionFeaturesBuilder();
        this.repairSeedAdapter = new RepairSeedAdapter();
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Solution solve(Instance instance, int startVertexId) {
        lastIterationCount = 0;
        long endTime = System.nanoTime() + timeLimitNanos;

        PopulationInitializer initializer = new PopulationInitializer(
            randomSolution,
            localSearch,
            diversityPolicy
        );
        ElitePopulation population = initializer.initialize(instance, startVertexId, populationSize);

        while (System.nanoTime() < endTime) {
            EvolutionIndividual[] parents = population.selectTwoParents(random);
            Cycle childCycle = recombination.recombine(
                instance,
                parents[0],
                parents[1],
                random
            );

            // Rekombinacja zwraca surowy partial cycle; tu dopelniamy tylko minimalne wejscie dla repair.
            repairSeedAdapter.adapt(childCycle, parents[0], parents[1]);
            repair.repair(instance, childCycle, random);
            if (improveAfterRecombination) {
                localSearch.improve(instance, childCycle);
            }

            int objective = SolutionObjective.calculate(instance, childCycle);
            if (!population.canBeatWorst(objective)) {
                lastIterationCount++;
                continue;
            }

            SolutionFeatures features = featuresBuilder.build(instance, childCycle);
            population.tryReplaceWorst(new EvolutionIndividual(childCycle, objective, features));

            lastIterationCount++;
        }

        EvolutionIndividual best = population.best();
        return new Solution(instance.name, startVertexId, best.cycle().toList());
    }

    @Override
    public int lastIterationCount() {
        return lastIterationCount;
    }
}
