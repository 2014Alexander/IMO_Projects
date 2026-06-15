package algorithm.construction;

import algorithm.CycleImprover;
import algorithm.OptimizationAlgorithm;
import algorithm.SolutionObjective;
import algorithm.localsearch.Cycle;
import model.Instance;
import model.Solution;

/**
 * Wybiera najlepszy start z 12 prób R2REGRET_TOP3P30 oraz consensus zbudowanego też na TOP3P30.
 */
public final class R2Top3P30Best12PlusConsensusR2BestStart implements OptimizationAlgorithm {
    private final String name;
    private final long seed;
    private final CycleImprover localSearch;

    public R2Top3P30Best12PlusConsensusR2BestStart(String name, long seed, CycleImprover localSearch) {
        this.name = name;
        this.seed = seed;
        this.localSearch = localSearch;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Solution solve(Instance instance, int startVertexId) {
        OptimizationAlgorithm r2Start = new Randomized2RegretParamBestNWithPhaseTwoDeleteStart(
                name + "_R2_START",
                3,
                0.30,
                12,
                seed,
                localSearch
        );
        OptimizationAlgorithm consensusStart = new ConsensusR2Top3P30LSExtractAll10Top3(seed ^ 0x9E3779B97F4A7C15L);

        Solution r2Solution = improveAgain(instance, r2Start.solve(instance, startVertexId));
        int r2Objective = SolutionObjective.calculate(instance, new Cycle(r2Solution.cycle(), instance.size));

        Solution consensusSolution = improveAgain(instance, consensusStart.solve(instance, startVertexId));
        int consensusObjective = SolutionObjective.calculate(instance, new Cycle(consensusSolution.cycle(), instance.size));

        return consensusObjective > r2Objective ? consensusSolution : r2Solution;
    }

    private Solution improveAgain(Instance instance, Solution solution) {
        Cycle cycle = new Cycle(solution.cycle(), instance.size);
        localSearch.improve(instance, cycle);
        return new Solution(solution.instanceName(), solution.startVertexId(), cycle.toList());
    }
}
