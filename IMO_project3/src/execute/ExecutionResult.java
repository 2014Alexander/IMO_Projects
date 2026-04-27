package execute;

import evaluation.SolutionMetrics;
import model.Solution;

public record ExecutionResult(
        String algorithmName,
        Solution solution,
        SolutionMetrics solutionMetrics,
        long runtimeNanos
) {
    public ExecutionResult {
        if (algorithmName == null) {
            throw new IllegalArgumentException("algorithmName cannot be null");
        }
        if (solution == null) {
            throw new IllegalArgumentException("solution cannot be null");
        }
        if (solutionMetrics == null) {
            throw new IllegalArgumentException("solutionMetrics cannot be null");
        }

    }
}