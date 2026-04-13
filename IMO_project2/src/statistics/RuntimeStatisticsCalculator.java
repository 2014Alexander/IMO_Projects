package statistics;

import execute.ExecutionResult;

import java.util.List;

public final class RuntimeStatisticsCalculator {

    private RuntimeStatisticsCalculator() {
    }

    public static RuntimeStatistics calculate(List<ExecutionResult> results) {
        ExecutionResult firstResult = results.get(0);

        long minRuntimeNanos = firstResult.runtimeNanos();
        long maxRuntimeNanos = firstResult.runtimeNanos();
        long sumRuntimeNanos = 0L;

        for (ExecutionResult result : results) {
            long runtimeNanos = result.runtimeNanos();

            if (runtimeNanos < minRuntimeNanos) {
                minRuntimeNanos = runtimeNanos;
            }

            if (runtimeNanos > maxRuntimeNanos) {
                maxRuntimeNanos = runtimeNanos;
            }

            sumRuntimeNanos += runtimeNanos;
        }

        double avgRuntimeNanos = (double) sumRuntimeNanos / results.size();

        return new RuntimeStatistics(
                firstResult.solution().instanceName(),
                firstResult.algorithmName(),
                results.size(),
                minRuntimeNanos,
                maxRuntimeNanos,
                avgRuntimeNanos
        );
    }
}
