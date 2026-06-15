package algorithm.metaheuristic;

import algorithm.CycleImprover;
import algorithm.IterationCountingAlgorithm;
import algorithm.OptimizationAlgorithm;
import algorithm.SolutionObjective;
import algorithm.construction.RandomSolution;
import algorithm.localsearch.Cycle;
import algorithm.metaheuristic.destroy.DestroyOperator;
import algorithm.metaheuristic.repair.RepairOperator;
import model.Instance;
import model.Solution;

import java.util.Random;

public final class LargeNeighborhoodSearch implements OptimizationAlgorithm, IterationCountingAlgorithm {
    private final String name;
    private final CycleImprover localSearch;
    private final DestroyOperator destroy;
    private final RepairOperator repair;
    private final long timeLimitNanos;
    private final Random random;
    private final RandomSolution randomSolution;

    private int lastIterationCount;

    public LargeNeighborhoodSearch(
        String name,
        CycleImprover localSearch,
        DestroyOperator destroy,
        RepairOperator repair,
        long timeLimitNanos,
        long seed
    ) {
        this.name = name;
        this.localSearch = localSearch;
        this.destroy = destroy;
        this.repair = repair;
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
            destroy.destroy(instance, candidateCycle, random);
            repair.repair(instance, candidateCycle, random);
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
