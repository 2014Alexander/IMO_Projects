package algorithm;

import algorithm.localsearch.Cycle;
import model.Instance;
import model.Solution;

public final class SolutionObjective {

    private SolutionObjective() {
    }

    public static int calculate(Instance instance, Solution solution) {
        Cycle cycle = new Cycle(solution.cycle(), solution.cycle().size());
        return calculate(instance, cycle);
    }

    public static int calculate(Instance instance, Cycle cycle) {
        int[][] distances = instance.distanceMatrix.distances;
        int objective = 0;

        for (int position = 0; position < cycle.size(); position++) {
            int from = cycle.cycle[position];
            int to = cycle.cycle[cycle.nextIndex(position)];

            objective += instance.vertices[from].profit;
            objective -= distances[from][to];
        }

        return objective;
    }

}
