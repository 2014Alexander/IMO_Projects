package experiment.lab6;

import experiment.summary.InstanceExperimentResult;
import model.Instance;

import java.util.ArrayList;
import java.util.List;

public final class Lab6ExperimentRunner {
    private final Lab6InstanceExperimentRunner instanceExperimentRunner;

    public Lab6ExperimentRunner(long baseSeed) {
        Lab6Scenario scenario = new Lab6Scenario();
        this.instanceExperimentRunner = new Lab6InstanceExperimentRunner(baseSeed, scenario);
    }

    public List<InstanceExperimentResult> run(List<Instance> instances) {
        List<InstanceExperimentResult> results = new ArrayList<>(instances.size());

        for (Instance instance : instances) {
            results.add(instanceExperimentRunner.run(instance));
        }

        return List.copyOf(results);
    }
}
