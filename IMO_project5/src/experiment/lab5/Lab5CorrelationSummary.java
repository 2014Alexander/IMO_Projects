package experiment.lab5;

public record Lab5CorrelationSummary(
    double objectiveVsVerticesToBest,
    double objectiveVsEdgesToBest,
    double objectiveVsAvgVerticesToOthers,
    double objectiveVsAvgEdgesToOthers
) {
}
