package algorithm.similarity;

/**
 * Liczy srednie podobienstwo kazdego rozwiazania do pozostalych rozwiazan.
 */
public final class AverageSimilarityToOthers {

    /**
     * Zwraca tablice srednich podobienstw w tej samej kolejnosci co wejscie.
     */
    public double[] calculate(
        SolutionFeatures[] features,
        SolutionSimilarityMeasure measure
    ) {
        double[] sums = new double[features.length];

        for (int first = 0; first < features.length - 1; first++) {
            for (int second = first + 1; second < features.length; second++) {
                int similarity = measure.calculate(features[first], features[second]);
                sums[first] += similarity;
                sums[second] += similarity;
            }
        }

        if (features.length < 2) {
            return sums;
        }

        double denominator = features.length - 1;
        for (int index = 0; index < sums.length; index++) {
            sums[index] /= denominator;
        }

        return sums;
    }
}
