package app;

import algorithm.OptimizationAlgorithm;
import algorithm.construction.ConsensusR2Top3P30LSExtractAll10Top3;
import algorithm.localsearch.SteepestLocalSearchWithCandidateMoves;
import algorithm.metaheuristic.CoolingSchedule;
import algorithm.metaheuristic.IteratedLocalSearchWithConstructionStartAndSaAcceptance;
import algorithm.metaheuristic.perturbation.RandomSwapEdgesPerturbation;
import evaluation.SolutionEvaluator;
import io.CsvInstanceReader;
import model.Instance;
import model.Solution;

public final class DebugSingleConsensus {
    public static void main(String[] args) {
        Instance instance = new CsvInstanceReader().read(args[0]);
        int startVertexId = Integer.parseInt(args[1]);
        long seed = Long.parseLong(args[2]);
        OptimizationAlgorithm alg = new IteratedLocalSearchWithConstructionStartAndSaAcceptance(
            "debug", new ConsensusR2Top3P30LSExtractAll10Top3(seed), new SteepestLocalSearchWithCandidateMoves(),
            new RandomSwapEdgesPerturbation(30), 500_000_000L, seed, 300.0, 10.0, CoolingSchedule.GEOMETRIC);
        System.out.println("before solve");
        long t = System.nanoTime();
        Solution s = alg.solve(instance, startVertexId);
        long dt = System.nanoTime() - t;
        System.out.println("after solve runtime="+dt+" obj="+new SolutionEvaluator().evaluate(instance,s).objective());
    }
}
