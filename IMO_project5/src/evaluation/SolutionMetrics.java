package evaluation;

public class SolutionMetrics {
    private final int tourLength;
    private final int profitSum;
    private final int objective;

    public SolutionMetrics(int tourLength, int profitSum) {
        this.tourLength = tourLength;
        this.profitSum = profitSum;
        this.objective = profitSum - tourLength;
    }

    public int tourLength() {
        return tourLength;
    }

    public int profitSum() {
        return profitSum;
    }

    public int objective() {
        return objective;
    }
}