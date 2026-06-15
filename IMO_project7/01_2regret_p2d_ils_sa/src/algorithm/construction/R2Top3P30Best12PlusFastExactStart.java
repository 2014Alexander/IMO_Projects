package algorithm.construction;

import algorithm.CycleImprover;
import algorithm.OptimizationAlgorithm;
import algorithm.SolutionObjective;
import algorithm.improvement.PhaseTwoDelete;
import algorithm.localsearch.Cycle;
import model.Instance;
import model.Solution;

import java.util.Random;

/**
 * 12 losowych startow R2 TOP3 P30 plus jeden szybki dokladny 2-regret.
 * Kazdy kandydat przechodzi PhaseTwoDelete oraz LS, a zwracany jest najlepszy lokalny optimum.
 */
public final class R2Top3P30Best12PlusFastExactStart implements OptimizationAlgorithm {
    private final String name;
    private final long seed;
    private final CycleImprover localSearch;
    private final PhaseTwoDelete phaseTwoDelete;

    public R2Top3P30Best12PlusFastExactStart(String name, long seed, CycleImprover localSearch) {
        this.name = name;
        this.seed = seed;
        this.localSearch = localSearch;
        this.phaseTwoDelete = new PhaseTwoDelete();
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Solution solve(Instance instance, int startVertexId) {
        Random seedRandom = new Random(seed);
        Cycle bestCycle = null;
        int bestObjective = Integer.MIN_VALUE;

        for (int i = 0; i < 12; i++) {
            OptimizationAlgorithm construction = new Randomized2RegretParameterizedWithPhaseTwoDelete(
                    name + "_R2_try" + i,
                    3,
                    0.30,
                    new Random(seedRandom.nextLong())
            );
            Solution solution = construction.solve(instance, startVertexId);
            Candidate candidate = improveAndScore(instance, solution);
            if (bestCycle == null || candidate.objective > bestObjective) {
                bestCycle = new Cycle(candidate.cycle);
                bestObjective = candidate.objective;
            }
        }

        Solution fastConstructed = new FastTop2ExactTwoRegretCost().solve(instance, startVertexId);
        Solution fastCleaned = phaseTwoDelete.improve(instance, fastConstructed);
        Candidate fastCandidate = improveAndScore(instance, fastCleaned);
        if (bestCycle == null || fastCandidate.objective > bestObjective) {
            bestCycle = new Cycle(fastCandidate.cycle);
        }

        return new Solution(instance.name, startVertexId, bestCycle.toList());
    }

    private Candidate improveAndScore(Instance instance, Solution solution) {
        Cycle cycle = new Cycle(solution.cycle(), instance.size);
        localSearch.improve(instance, cycle);
        int objective = SolutionObjective.calculate(instance, cycle);
        return new Candidate(cycle, objective);
    }

    private record Candidate(Cycle cycle, int objective) {}
}
