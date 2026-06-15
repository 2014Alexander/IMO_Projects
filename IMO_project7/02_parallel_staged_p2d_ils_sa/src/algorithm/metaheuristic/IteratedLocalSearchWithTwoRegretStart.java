package algorithm.metaheuristic;

import algorithm.CycleImprover;
import algorithm.IterationCountingAlgorithm;
import algorithm.OptimizationAlgorithm;
import algorithm.SolutionObjective;
import algorithm.construction.TwoRegretCost;
import algorithm.improvement.PhaseTwoDelete;
import algorithm.localsearch.Cycle;
import algorithm.metaheuristic.perturbation.IlsPerturbation;
import model.Instance;
import model.Solution;

import java.util.Random;

/**
 * Wariant ILS z konstrukcyjnym startem 2-regret i druga faza usuwania.
 */
public final class IteratedLocalSearchWithTwoRegretStart implements OptimizationAlgorithm, IterationCountingAlgorithm {
    private final String name;
    private final CycleImprover localSearch;
    private final IlsPerturbation perturbation;
    private final long timeLimitNanos;
    private final Random random;
    private final TwoRegretCost twoRegretCost;
    private final PhaseTwoDelete phaseTwoDelete;

    private int lastIterationCount;

    public IteratedLocalSearchWithTwoRegretStart(
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
        this.twoRegretCost = new TwoRegretCost();
        this.phaseTwoDelete = new PhaseTwoDelete();
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Solution solve(Instance instance, int startVertexId) {
        lastIterationCount = 0;
        long endTime = System.nanoTime() + timeLimitNanos;

        Solution start = twoRegretCost.solve(instance, startVertexId);
        Cycle currentCycle = new Cycle(start.cycle(), instance.size);
        phaseTwoDelete.improve(instance, currentCycle);
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
