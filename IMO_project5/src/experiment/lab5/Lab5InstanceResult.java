package experiment.lab5;

import java.util.List;

public record Lab5InstanceResult(
    String instanceName,
    int localOptimaCount,
    String localSearchName,
    String neighborhoodName,
    Lab5BestSolutionMetadata bestSolution,
    List<Lab5Point> points,
    Lab5CorrelationSummary correlations
) {
    public Lab5InstanceResult {
        points = List.copyOf(points);
    }
}
