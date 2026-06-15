package app;

import algorithm.IterationCountingAlgorithm;
import algorithm.OptimizationAlgorithm;
import algorithm.localsearch.SteepestLocalSearchWithCandidateMoves;
import algorithm.metaheuristic.IteratedLocalSearch;
import algorithm.metaheuristic.IteratedLocalSearchWithSaAcceptance;
import algorithm.metaheuristic.IteratedLocalSearchWithTwoRegretStart;
import algorithm.metaheuristic.IteratedLocalSearchWithTwoRegretStartAndSaAcceptance;
import algorithm.metaheuristic.perturbation.RandomSwapEdgesPerturbation;
import evaluation.SolutionEvaluator;
import experiment.core.RunConfig;
import experiment.core.RunPreparator;
import io.CsvInstanceReader;
import model.Instance;
import model.Solution;

import java.util.List;

/**
 * Uruchamia cala serie testow ILS w jednym procesie JVM.
 */
public final class BatchIlsRun {
    private static final String[] ALGORITHMS = {
        "ILS_RANDOM_START",
        "ILS_RANDOM_START_SA_ACCEPT",
        "ILS_2REGRET_P2D_START",
        "ILS_2REGRET_P2D_START_SA_ACCEPT"
    };

    public static void main(String[] args) {
        long timeLimitNanos = Long.parseLong(args[0]);
        int runsCount = Integer.parseInt(args[1]);
        long baseSeed = Long.parseLong(args[2]);

        for (int instanceArgument = 3; instanceArgument < args.length; instanceArgument++) {
            String instancePath = args[instanceArgument];
            Instance instance = new CsvInstanceReader().read(instancePath);
            List<RunConfig> runs = RunPreparator.prepareRuns(instance, baseSeed, runsCount);

            for (String algorithmName : ALGORITHMS) {
                for (int runIndex = 0; runIndex < runs.size(); runIndex++) {
                    RunConfig run = runs.get(runIndex);
                    OptimizationAlgorithm algorithm = createAlgorithm(algorithmName, timeLimitNanos, run.runSeed());

                    long startTime = System.nanoTime();
                    Solution solution = algorithm.solve(instance, run.startVertexId());
                    long runtimeNanos = System.nanoTime() - startTime;

                    int objective = new SolutionEvaluator().evaluate(instance, solution).objective();
                    int iterationCount = ((IterationCountingAlgorithm) algorithm).lastIterationCount();

                    System.out.println(
                        instance.name + "," + algorithmName + "," + runIndex + ","
                            + run.startVertexId() + "," + run.runSeed() + ","
                            + objective + "," + runtimeNanos + "," + iterationCount
                    );
                }
            }
        }
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

        if ("ILS_RANDOM_START_SA_ACCEPT".equals(algorithmName)) {
            return new IteratedLocalSearchWithSaAcceptance(
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

        return new IteratedLocalSearchWithTwoRegretStartAndSaAcceptance(
            algorithmName,
            new SteepestLocalSearchWithCandidateMoves(),
            new RandomSwapEdgesPerturbation(),
            timeLimitNanos,
            seed
        );
    }
}
