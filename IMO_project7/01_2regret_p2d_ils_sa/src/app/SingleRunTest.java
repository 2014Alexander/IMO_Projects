package app;

import algorithm.OptimizationAlgorithm;
import algorithm.metaheuristic.IteratedLocalSearchWithTwoRegretStart;
import algorithm.localsearch.SteepestLocalSearchWithCandidateMoves;
import algorithm.metaheuristic.perturbation.RandomSwapEdgesPerturbation;
import evaluation.SolutionEvaluator;
import io.CsvInstanceReader;
import model.Instance;
import model.Solution;

public final class SingleRunTest {
    public static void main(String[] args) {
        Instance instance = new CsvInstanceReader().read("data/TSPB.csv");
        int startVertexId = Integer.parseInt(args[0]);
        long seed = Long.parseLong(args[1]);
        OptimizationAlgorithm algorithm = new IteratedLocalSearchWithTwoRegretStart(
            "ILS_2REGRET_P2D_START",
            new SteepestLocalSearchWithCandidateMoves(),
            new RandomSwapEdgesPerturbation(),
            1_000_000_000L,
            seed
        );
        long start = System.nanoTime();
        Solution solution = algorithm.solve(instance, startVertexId);
        long runtime = System.nanoTime() - start;
        int objective = new SolutionEvaluator().evaluate(instance, solution).objective();
        System.out.println("runtimeNanos=" + runtime);
        System.out.println("objective=" + objective);
    }
}
