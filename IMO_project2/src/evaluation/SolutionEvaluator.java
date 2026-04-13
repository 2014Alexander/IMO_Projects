package evaluation;

import model.Instance;
import model.Solution;

import java.util.List;

public class SolutionEvaluator {

    public SolutionMetrics evaluate(Instance instance, Solution solution) {
        List<Integer> cycle = solution.cycle();

        int tourLength = computeTourLength(instance, cycle);
        int profitSum = computeProfitSum(instance, cycle);

        return new SolutionMetrics(tourLength, profitSum);
    }

    private int computeTourLength(Instance instance, List<Integer> cycle) {
        if (cycle.isEmpty()) {
            return 0;
        }

        int[][] distances = instance.distanceMatrix.distances;
        int length = 0;

        for (int i = 0; i < cycle.size(); i++) {
            int from = cycle.get(i);
            int to = cycle.get((i + 1) % cycle.size());
            length += distances[from][to];
        }

        return length;
    }

    private int computeProfitSum(Instance instance, List<Integer> cycle) {
        int profitSum = 0;

        for (int vertexId : cycle) {
            profitSum += instance.vertices[vertexId].profit;
        }

        return profitSum;
    }
}