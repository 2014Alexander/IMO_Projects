package statistics;

public record RuntimeStatistics(
        String instanceName,
        String algorithmName,
        int runsCount,
        long minRuntimeNanos,
        long maxRuntimeNanos,
        double avgRuntimeNanos
) {
    public RuntimeStatistics {
        validate(
                instanceName,
                algorithmName,
                runsCount,
                minRuntimeNanos,
                maxRuntimeNanos,
                avgRuntimeNanos
        );
    }

    private static void validate(
            String instanceName,
            String algorithmName,
            int runsCount,
            long minRuntimeNanos,
            long maxRuntimeNanos,
            double avgRuntimeNanos
    ) {
        if (instanceName == null) {
            throw new IllegalArgumentException("instanceName cannot be null");
        }
        if (algorithmName == null) {
            throw new IllegalArgumentException("algorithmName cannot be null");
        }
        if (runsCount <= 0) {
            throw new IllegalArgumentException("runsCount must be greater than 0");
        }
        if (minRuntimeNanos < 0) {
            throw new IllegalArgumentException("minRuntimeNanos cannot be negative");
        }
        if (maxRuntimeNanos < 0) {
            throw new IllegalArgumentException("maxRuntimeNanos cannot be negative");
        }
        if (minRuntimeNanos > maxRuntimeNanos) {
            throw new IllegalArgumentException("minRuntimeNanos cannot be greater than maxRuntimeNanos");
        }
        if (!Double.isFinite(avgRuntimeNanos)) {
            throw new IllegalArgumentException("avgRuntimeNanos must be a finite number");
        }
        if (avgRuntimeNanos < 0.0) {
            throw new IllegalArgumentException("avgRuntimeNanos cannot be negative");
        }
    }
}
