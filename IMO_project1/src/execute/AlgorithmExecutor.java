package execute;

import algorithm.ConstructionAlgorithm;
import algorithm.improvement.PhaseTwoDelete;
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
            ConstructionAlgorithm algorithm
    ) {
        long startTime = System.nanoTime();

        Solution solution = algorithm.solve(instance, startVertexId);
        long runtimeNanos = System.nanoTime() - startTime;

        SolutionMetrics solutionMetrics = solutionEvaluator.evaluate(instance, solution);

        return new ExecutionResult(
                algorithmName,
                solution,
                solutionMetrics,
                runtimeNanos
        );
    }

    public ExecutionResult execute(
            Instance instance,
            int startVertexId,
            String algorithmName,
            ConstructionAlgorithm phaseOneAlgorithm,
            PhaseTwoDelete phaseTwoDelete
    ) {
        long startTime = System.nanoTime();

        Solution phaseOneSolution = phaseOneAlgorithm.solve(instance, startVertexId);
        Solution finalSolution = phaseTwoDelete.improve(instance, phaseOneSolution);

        long runtimeNanos = System.nanoTime() - startTime;

        SolutionMetrics solutionMetrics = solutionEvaluator.evaluate(instance, finalSolution);

        return new ExecutionResult(
                algorithmName,
                finalSolution,
                solutionMetrics,
                runtimeNanos
        );
    }
}