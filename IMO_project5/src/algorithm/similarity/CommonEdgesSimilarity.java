package algorithm.similarity;

/**
 * Liczy liczbe wspolnych krawedzi nieskierowanych.
 */
public final class CommonEdgesSimilarity implements SolutionSimilarityMeasure {

    @Override
    public int calculate(SolutionFeatures first, SolutionFeatures second) {
        long[] firstEdges = first.edges();
        long[] secondEdges = second.edges();
        int firstIndex = 0;
        int secondIndex = 0;
        int common = 0;

        while (firstIndex < firstEdges.length && secondIndex < secondEdges.length) {
            long firstEdge = firstEdges[firstIndex];
            long secondEdge = secondEdges[secondIndex];

            if (firstEdge == secondEdge) {
                common++;
                firstIndex++;
                secondIndex++;
            } else if (firstEdge < secondEdge) {
                firstIndex++;
            } else {
                secondIndex++;
            }
        }

        return common;
    }
}
