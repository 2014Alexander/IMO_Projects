package algorithm.construction;

import algorithm.CycleImprover;
import algorithm.OptimizationAlgorithm;
import algorithm.SolutionObjective;
import algorithm.localsearch.Cycle;
import model.Instance;
import model.Solution;

/**
 * Wybiera najlepszy start z dwóch źródeł:
 * 12 prób R2REGRET_TOP3P30 oraz jeden konstruktor consensus.
 */
public final class R2Top3P30Best12PlusConsensusBestStart implements OptimizationAlgorithm {
    private final String name;
    private final long seed;
    private final CycleImprover localSearch;

    public R2Top3P30Best12PlusConsensusBestStart(String name, long seed, CycleImprover localSearch) {
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
        OptimizationAlgorithm consensusStart = new ConsensusTop3LSExtractAll10Top3(seed ^ 0x5DEECE66DL);

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
