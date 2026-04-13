package experiment.run;

import experiment.summary.InstanceExperimentResult;
import model.Instance;

import java.util.ArrayList;
import java.util.List;

public final class InstancesExperimentRunner {

    private final InstanceExperimentRunner instanceExperimentRunner;

    public InstancesExperimentRunner(long baseSeed) {
        this.instanceExperimentRunner = new InstanceExperimentRunner(baseSeed);
    }

    public List<InstanceExperimentResult> run(List<Instance> instances) {
        validateInstances(instances);

        List<InstanceExperimentResult> results = new ArrayList<>(instances.size());

        for (Instance instance : instances) {
            results.add(instanceExperimentRunner.run(instance));
        }

        return List.copyOf(results);
    }

    private static void validateInstances(List<Instance> instances) {
        if (instances == null) {
            throw new IllegalArgumentException("instances cannot be null");
        }
        if (instances.isEmpty()) {
            throw new IllegalArgumentException("instances cannot be empty");
        }

        for (Instance instance : instances) {
            if (instance == null) {
                throw new IllegalArgumentException("instances cannot contain null");
            }
        }
    }
}
