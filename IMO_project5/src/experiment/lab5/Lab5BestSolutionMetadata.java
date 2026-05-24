package experiment.lab5;

import java.util.List;

public record Lab5BestSolutionMetadata(
    String sourceAlgorithmName,
    int objective,
    int startVertexId,
    long runSeed,
    List<String> selectionPoolAlgorithms
) {
    public Lab5BestSolutionMetadata {
        selectionPoolAlgorithms = List.copyOf(selectionPoolAlgorithms);
    }
}
