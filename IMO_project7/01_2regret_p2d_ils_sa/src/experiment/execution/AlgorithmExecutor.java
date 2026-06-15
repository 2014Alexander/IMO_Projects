package experiment.execution;

import algorithm.IterationCountingAlgorithm;
import algorithm.OptimizationAlgorithm;
import evaluation.SolutionEvaluator;
import evaluation.SolutionMetrics;
import model.Instance;
import model.Solution;

public class AlgorithmExecutor {
    private final SolutionEvaluator solutionEvaluator = new SolutionEvaluator();

    public ExecutionResult execute(
            Instance instance,
            int startVertexId,
            String algorithmName,
            OptimizationAlgorithm algorithm
    ) {
        long startTime = System.nanoTime();

        Solution solution = algorithm.solve(instance, startVertexId);
        long runtimeNanos = System.nanoTime() - startTime;

        Integer iterationCount = null;
        if (algorithm instanceof IterationCountingAlgorithm countingAlgorithm) {
            iterationCount = countingAlgorithm.lastIterationCount();
        }

        SolutionMetrics solutionMetrics = solutionEvaluator.evaluate(instance, solution);

        return new ExecutionResult(
                algorithmName,
                solution,
                solutionMetrics,
                runtimeNanos,
                iterationCount
        );
    }

}
