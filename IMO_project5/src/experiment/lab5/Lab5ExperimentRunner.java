package experiment.lab5;

import model.Instance;

import java.util.ArrayList;
import java.util.List;

public final class Lab5ExperimentRunner {
    private final Lab5InstanceExperimentRunner instanceExperimentRunner;

    public Lab5ExperimentRunner(long baseSeed) {
        this.instanceExperimentRunner = new Lab5InstanceExperimentRunner(baseSeed, new Lab5Scenario());
    }

    public List<Lab5InstanceResult> run(List<Instance> instances) {
        List<Lab5InstanceResult> results = new ArrayList<>(instances.size());

        for (Instance instance : instances) {
            results.add(instanceExperimentRunner.run(instance));
        }

        return List.copyOf(results);
    }
}
