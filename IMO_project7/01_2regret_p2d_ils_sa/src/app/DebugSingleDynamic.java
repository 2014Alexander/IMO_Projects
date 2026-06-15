package app;

import algorithm.OptimizationAlgorithm;
import algorithm.construction.CheapConsensusR2Top3P30P2DAll10LSTop5Top3;
import algorithm.localsearch.SteepestLocalSearchWithCandidateMoves;
import algorithm.metaheuristic.CoolingSchedule;
import algorithm.metaheuristic.IteratedLocalSearchWithDynamicEliteBackboneSaAcceptance;
import evaluation.SolutionEvaluator;
import io.CsvInstanceReader;
import model.Instance;
import model.Solution;

public final class DebugSingleDynamic {
    public static void main(String[] args) {
        Instance instance = new CsvInstanceReader().read(args[0]);
        int startVertexId = Integer.parseInt(args[1]);
        long seed = Long.parseLong(args[2]);
        OptimizationAlgorithm alg = new IteratedLocalSearchWithDynamicEliteBackboneSaAcceptance(
            "debug", new CheapConsensusR2Top3P30P2DAll10LSTop5Top3(seed), new SteepestLocalSearchWithCandidateMoves(),
            500_000_000L, seed, 300.0, 10.0, CoolingSchedule.GEOMETRIC, 30, 0.2);
        System.out.println("before solve");
        long t = System.nanoTime();
        Solution s = alg.solve(instance, startVertexId);
        long dt = System.nanoTime() - t;
        System.out.println("after solve runtime="+dt+" obj="+new SolutionEvaluator().evaluate(instance,s).objective());
    }
}
