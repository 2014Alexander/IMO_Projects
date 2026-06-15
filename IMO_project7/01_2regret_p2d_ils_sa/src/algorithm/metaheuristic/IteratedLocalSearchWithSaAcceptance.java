package algorithm.metaheuristic;

import algorithm.CycleImprover;
import algorithm.IterationCountingAlgorithm;
import algorithm.OptimizationAlgorithm;
import algorithm.SolutionObjective;
import algorithm.construction.RandomSolution;
import algorithm.localsearch.Cycle;
import algorithm.metaheuristic.perturbation.IlsPerturbation;
import model.Instance;
import model.Solution;

import java.util.Random;

/**
 * Wariant ILS z akceptacja symulowanego wyzarzania.
 */
public final class IteratedLocalSearchWithSaAcceptance implements OptimizationAlgorithm, IterationCountingAlgorithm {
    public static final double DEFAULT_INITIAL_TEMPERATURE = 300.0;
    public static final double DEFAULT_FINAL_TEMPERATURE = 1.0;

    private final String name;
    private final CycleImprover localSearch;
    private final IlsPerturbation perturbation;
    private final long timeLimitNanos;
    private final Random random;
    private final RandomSolution randomSolution;
    private final double initialTemperature;
    private final double finalTemperature;

    private int lastIterationCount;

    public IteratedLocalSearchWithSaAcceptance(
        String name,
        CycleImprover localSearch,
        IlsPerturbation perturbation,
        long timeLimitNanos,
        long seed
    ) {
        this(
            name,
            localSearch,
            perturbation,
            timeLimitNanos,
            seed,
            DEFAULT_INITIAL_TEMPERATURE,
            DEFAULT_FINAL_TEMPERATURE
        );
    }

    public IteratedLocalSearchWithSaAcceptance(
        String name,
        CycleImprover localSearch,
        IlsPerturbation perturbation,
        long timeLimitNanos,
        long seed,
        double initialTemperature,
        double finalTemperature
    ) {
        this.name = name;
        this.localSearch = localSearch;
        this.perturbation = perturbation;
        this.timeLimitNanos = timeLimitNanos;
        this.random = new Random(seed);
        this.randomSolution = new RandomSolution(seed);
        this.initialTemperature = initialTemperature;
        this.finalTemperature = finalTemperature;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Solution solve(Instance instance, int startVertexId) {
        lastIterationCount = 0;
        long startTime = System.nanoTime();
        long endTime = startTime + timeLimitNanos;

        Solution start = randomSolution.solve(instance, startVertexId);
        Cycle currentCycle = new Cycle(start.cycle(), instance.size);
        localSearch.improve(instance, currentCycle);
        int currentObjective = SolutionObjective.calculate(instance, currentCycle);

        Cycle bestCycle = new Cycle(currentCycle);
        int bestObjective = currentObjective;

        while (System.nanoTime() < endTime) {
            long now = System.nanoTime();
            double temperature = temperature(now - startTime);

            Cycle candidateCycle = new Cycle(currentCycle);
            perturbation.perturb(instance, candidateCycle, random);
            localSearch.improve(instance, candidateCycle);
            int candidateObjective = SolutionObjective.calculate(instance, candidateCycle);
            int delta = candidateObjective - currentObjective;

            lastIterationCount++;

            if (delta > 0 || random.nextDouble() < Math.exp(delta / temperature)) {
                currentCycle = candidateCycle;
                currentObjective = candidateObjective;

                if (currentObjective > bestObjective) {
                    bestCycle = new Cycle(currentCycle);
                    bestObjective = currentObjective;
                }
            }
        }

        return new Solution(
            start.instanceName(),
            start.startVertexId(),
            bestCycle.toList()
        );
    }

    private double temperature(long elapsedNanos) {
        double progress = (double) elapsedNanos / (double) timeLimitNanos;
        if (progress > 1.0) {
            progress = 1.0;
        }

        return initialTemperature * Math.pow(finalTemperature / initialTemperature, progress);
    }

    @Override
    public int lastIterationCount() {
        return lastIterationCount;
    }
}
