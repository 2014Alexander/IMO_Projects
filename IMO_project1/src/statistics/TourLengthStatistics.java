package statistics;

public record TourLengthStatistics(
        String instanceName,
        String algorithmName,
        int runsCount,
        int minTourLength,
        int maxTourLength,
        double avgTourLength
) {
    public TourLengthStatistics {
        validate(
                instanceName,
                algorithmName,
                runsCount,
                minTourLength,
                maxTourLength,
                avgTourLength
        );
    }

    private static void validate(
            String instanceName,
            String algorithmName,
            int runsCount,
            int minTourLength,
            int maxTourLength,
            double avgTourLength
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
        if (minTourLength > maxTourLength) {
            throw new IllegalArgumentException("minTourLength cannot be greater than maxTourLength");
        }
        if (!Double.isFinite(avgTourLength)) {
            throw new IllegalArgumentException("avgTourLength must be a finite number");
        }
    }
}