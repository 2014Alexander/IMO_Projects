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

public final class IteratedLocalSearch implements OptimizationAlgorithm, IterationCountingAlgorithm {
    private final String name;
    private final CycleImprover localSearch;
    private final IlsPerturbation perturbation;
    private final long timeLimitNanos;
    private final Random random;
    private final RandomSolution randomSolution;

    private int lastIterationCount;

    public IteratedLocalSearch(
        String name,
        CycleImprover localSearch,
        IlsPerturbation perturbation,
        long timeLimitNanos,
        long seed
    ) {
        this.name = name;
        this.localSearch = localSearch;
        this.perturbation = perturbation;
        this.timeLimitNanos = timeLimitNanos;
        this.random = new Random(seed);
        this.randomSolution = new RandomSolution(seed);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Solution solve(Instance instance, int startVertexId) {
        lastIterationCount = 0;
        long endTime = System.nanoTime() + timeLimitNanos;

        Solution start = randomSolution.solve(instance, startVertexId);
        Cycle currentCycle = new Cycle(start.cycle(), instance.size);
        localSearch.improve(instance, currentCycle);
        int currentObjective = SolutionObjective.calculate(instance, currentCycle);

        while (System.nanoTime() < endTime) {
            Cycle candidateCycle = new Cycle(currentCycle);
            perturbation.perturb(instance, candidateCycle, random);
            localSearch.improve(instance, candidateCycle);
            int candidateObjective = SolutionObjective.calculate(instance, candidateCycle);

            lastIterationCount++;

            if (candidateObjective > currentObjective) {
                currentCycle = candidateCycle;
                currentObjective = candidateObjective;
            }
        }

        return new Solution(
            start.instanceName(),
            start.startVertexId(),
            currentCycle.toList()
        );
    }

    @Override
    public int lastIterationCount() {
        return lastIterationCount;
    }
}
