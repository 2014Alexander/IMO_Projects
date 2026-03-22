package experiment.run;

import algorithm.ConstructionAlgorithm;
import algorithm.improvement.PhaseTwoDelete;
import execute.AlgorithmExecutor;
import execute.ExecutionResult;
import model.Instance;

import java.util.ArrayList;
import java.util.List;

public class ExperimentRunner {
    private final AlgorithmExecutor algorithmExecutor = new AlgorithmExecutor();

    public List<ExecutionResult> runForAllVertices(
            Instance instance,
            String algorithmName,
            ConstructionAlgorithm algorithm
    ) {
        List<ExecutionResult> results = new ArrayList<>();

        for (int startVertexId = 0; startVertexId < instance.vertices.length; startVertexId++) {
            ExecutionResult result = algorithmExecutor.execute(
                    instance,
                    startVertexId,
                    algorithmName,
                    algorithm
            );
            results.add(result);
        }

        return results;
    }

    public List<ExecutionResult> runForAllVertices(
            Instance instance,
            String algorithmName,
            ConstructionAlgorithm phaseOneAlgorithm,
            PhaseTwoDelete phaseTwoDelete
    ) {
        List<ExecutionResult> results = new ArrayList<>();

        for (int startVertexId = 0; startVertexId < instance.vertices.length; startVertexId++) {
            ExecutionResult result = algorithmExecutor.execute(
                    instance,
                    startVertexId,
                    algorithmName,
                    phaseOneAlgorithm,
                    phaseTwoDelete
            );
            results.add(result);
        }

        return results;
    }
}