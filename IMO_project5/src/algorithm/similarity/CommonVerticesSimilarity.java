package algorithm.similarity;

/**
 * Liczy liczbe wspolnych wybranych wierzcholkow.
 */
public final class CommonVerticesSimilarity implements SolutionSimilarityMeasure {

    @Override
    public int calculate(SolutionFeatures first, SolutionFeatures second) {
        boolean[] firstSelected = first.selectedVertices();
        boolean[] secondSelected = second.selectedVertices();
        int common = 0;

        for (int vertex = 0; vertex < firstSelected.length; vertex++) {
            if (firstSelected[vertex] && secondSelected[vertex]) {
                common++;
            }
        }

        return common;
    }
}
