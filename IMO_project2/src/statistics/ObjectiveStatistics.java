package statistics;

public record ObjectiveStatistics(
        String instanceName,
        String algorithmName,
        int runsCount,
        int minObjective,
        int maxObjective,
        double avgObjective
) {
    public ObjectiveStatistics {
        validate(
                instanceName,
                algorithmName,
                runsCount,
                minObjective,
                maxObjective,
                avgObjective
        );
    }

    private static void validate(
            String instanceName,
            String algorithmName,
            int runsCount,
            int minObjective,
            int maxObjective,
            double avgObjective
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
        if (minObjective > maxObjective) {
            throw new IllegalArgumentException("minObjective cannot be greater than maxObjective");
        }
        if (!Double.isFinite(avgObjective)) {
            throw new IllegalArgumentException("avgObjective must be a finite number");
        }
    }
}