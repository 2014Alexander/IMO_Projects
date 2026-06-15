package app;

import algorithm.IterationCountingAlgorithm;
import algorithm.OptimizationAlgorithm;
import algorithm.localsearch.SteepestLocalSearchWithCandidateMoves;
import algorithm.metaheuristic.IteratedLocalSearch;
import algorithm.metaheuristic.IteratedLocalSearchWithTwoRegretStart;
import algorithm.metaheuristic.IteratedLocalSearchWithSaAcceptance;
import algorithm.metaheuristic.IteratedLocalSearchWithTwoRegretStartAndSaAcceptance;
import algorithm.metaheuristic.perturbation.RandomSwapEdgesPerturbation;
import evaluation.SolutionEvaluator;
import io.CsvInstanceReader;
import model.Instance;
import model.Solution;

public final class SingleIlsRun {
    public static void main(String[] args) {
        String instancePath = args[0];
        String algorithmName = args[1];
        int startVertexId = Integer.parseInt(args[2]);
        long seed = Long.parseLong(args[3]);
        long timeLimitNanos = Long.parseLong(args[4]);

        Instance instance = new CsvInstanceReader().read(instancePath);
        OptimizationAlgorithm algorithm = createAlgorithm(algorithmName, timeLimitNanos, seed);

        long startTime = System.nanoTime();
        Solution solution = algorithm.solve(instance, startVertexId);
        long runtimeNanos = System.nanoTime() - startTime;

        int objective = new SolutionEvaluator().evaluate(instance, solution).objective();
        int iterationCount = ((IterationCountingAlgorithm) algorithm).lastIterationCount();

        System.out.println(objective + "," + runtimeNanos + "," + iterationCount);
    }

    private static OptimizationAlgorithm createAlgorithm(
        String algorithmName,
        long timeLimitNanos,
        long seed
    ) {
        if ("ILS_RANDOM_START".equals(algorithmName)) {
            return new IteratedLocalSearch(
                algorithmName,
                new SteepestLocalSearchWithCandidateMoves(),
                new RandomSwapEdgesPerturbation(),
                timeLimitNanos,
                seed
            );
        }

        if ("ILS_2REGRET_P2D_START".equals(algorithmName)) {
            return new IteratedLocalSearchWithTwoRegretStart(
                algorithmName,
                new SteepestLocalSearchWithCandidateMoves(),
                new RandomSwapEdgesPerturbation(),
                timeLimitNanos,
                seed
            );
        }

        if ("ILS_RANDOM_START_SA_ACCEPT".equals(algorithmName)) {
            return new IteratedLocalSearchWithSaAcceptance(
                algorithmName,
                new SteepestLocalSearchWithCandidateMoves(),
                new RandomSwapEdgesPerturbation(),
                timeLimitNanos,
                seed
            );
        }

        if ("ILS_2REGRET_P2D_START_SA_ACCEPT".equals(algorithmName)) {
            return new IteratedLocalSearchWithTwoRegretStartAndSaAcceptance(
                algorithmName,
                new SteepestLocalSearchWithCandidateMoves(),
                new RandomSwapEdgesPerturbation(),
                timeLimitNanos,
                seed
            );
        }

        throw new IllegalArgumentException("Nieznany algorytm: " + algorithmName);
    }
}
