package algorithm.similarity;

/**
 * Miara podobienstwa dwoch rozwiazan reprezentowanych przez cechy strukturalne.
 */
public interface SolutionSimilarityMeasure {

    /**
     * Liczy podobienstwo dwoch rozwiazan.
     */
    int calculate(SolutionFeatures first, SolutionFeatures second);
}
