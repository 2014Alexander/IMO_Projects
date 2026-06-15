package algorithm.metaheuristic;

import algorithm.CycleImprover;
import algorithm.IterationCountingAlgorithm;
import algorithm.OptimizationAlgorithm;
import algorithm.SolutionObjective;
import algorithm.construction.RandomSolution;
import algorithm.localsearch.Cycle;
import model.Instance;
import model.Solution;

public final class MultipleStartLocalSearch implements OptimizationAlgorithm, IterationCountingAlgorithm {
    private final String name;
    private final CycleImprover localSearch;
    private final int startsCount;
    private final RandomSolution randomSolution;

    private int lastIterationCount;

    public MultipleStartLocalSearch(
        String name,
        CycleImprover localSearch,
        int startsCount,
        long seed
    ) {
        this.name = name;
        this.localSearch = localSearch;
        this.startsCount = startsCount;
        this.randomSolution = new RandomSolution(seed);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Solution solve(Instance instance, int startVertexId) {
        lastIterationCount = startsCount;

        Cycle bestCycle = null;
        Solution bestStart = null;
        int bestObjective = Integer.MIN_VALUE;

        for (int start = 0; start < startsCount; start++) {
            int currentStartVertexId = (startVertexId + start) % instance.size;
            Solution randomStart = randomSolution.solve(instance, currentStartVertexId);
            Cycle candidateCycle = new Cycle(randomStart.cycle(), instance.size);
            localSearch.improve(instance, candidateCycle);
            int candidateObjective = SolutionObjective.calculate(instance, candidateCycle);

            if (candidateObjective > bestObjective) {
                bestCycle = candidateCycle;
                bestStart = randomStart;
                bestObjective = candidateObjective;
            }
        }

        return new Solution(
            bestStart.instanceName(),
            bestStart.startVertexId(),
            bestCycle.toList()
        );
    }

    @Override
    public int lastIterationCount() {
        return lastIterationCount;
    }
}
