package experiment.lab5;

public record Lab5Point(
    int localOptimumIndex,
    int startVertexId,
    long runSeed,
    int objective,
    int similarityVerticesToBest,
    int similarityEdgesToBest,
    double avgSimilarityVerticesToOthers,
    double avgSimilarityEdgesToOthers
) {
}
