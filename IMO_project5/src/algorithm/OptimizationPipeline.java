package algorithm;

import model.Instance;
import model.Solution;

import java.util.ArrayList;
import java.util.List;

public final class OptimizationPipeline implements OptimizationAlgorithm {
    private final String name;
    private final OptimizationAlgorithm initialAlgorithm;
    private final List<SolutionImprover> improvers = new ArrayList<>();

    private OptimizationPipeline(
        String name,
        OptimizationAlgorithm initialAlgorithm
    ) {
        this.name = name;
        this.initialAlgorithm = initialAlgorithm;
    }

    public static OptimizationPipeline startWith(
        String name,
        OptimizationAlgorithm initialAlgorithm
    ) {
        return new OptimizationPipeline(name, initialAlgorithm);
    }

    public OptimizationPipeline then(SolutionImprover improver) {
        improvers.add(improver);
        return this;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Solution solve(Instance instance, int startVertexId) {
        Solution solution = initialAlgorithm.solve(instance, startVertexId);

        for (SolutionImprover improver : improvers) {
            solution = improver.improve(instance, solution);
        }

        return solution;
    }
}
