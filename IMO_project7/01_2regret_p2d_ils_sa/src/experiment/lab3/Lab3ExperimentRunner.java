package experiment.lab3;

import experiment.summary.InstanceExperimentResult;
import model.Instance;

import java.util.ArrayList;
import java.util.List;

public final class Lab3ExperimentRunner {

    private final InstanceExperimentRunner instanceExperimentRunner;

    public Lab3ExperimentRunner(long baseSeed) {
        Lab3Scenario scenario = new Lab3Scenario();
        this.instanceExperimentRunner = new InstanceExperimentRunner(baseSeed, scenario);
    }

    public List<InstanceExperimentResult> run(List<Instance> instances) {
        List<InstanceExperimentResult> results = new ArrayList<>(instances.size());

        for (Instance instance : instances) {
            results.add(instanceExperimentRunner.run(instance));
        }

        return List.copyOf(results);
    }
}
