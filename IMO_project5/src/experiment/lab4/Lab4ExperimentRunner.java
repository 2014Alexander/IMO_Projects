package experiment.lab4;

import experiment.summary.InstanceExperimentResult;
import model.Instance;

import java.util.ArrayList;
import java.util.List;

public final class Lab4ExperimentRunner {

    private final Lab4InstanceExperimentRunner instanceExperimentRunner;

    public Lab4ExperimentRunner(long baseSeed) {
        Lab4Scenario scenario = new Lab4Scenario();
        this.instanceExperimentRunner = new Lab4InstanceExperimentRunner(baseSeed, scenario);
    }

    /**
     * Uruchamia eksperyment lab4 dla podanych instancji.
     *
     * @param instances instancje problemu
     * @return wyniki eksperymentu
     */
    public List<InstanceExperimentResult> run(List<Instance> instances) {
        List<InstanceExperimentResult> results = new ArrayList<>(instances.size());

        for (Instance instance : instances) {
            results.add(instanceExperimentRunner.run(instance));
        }

        return List.copyOf(results);
    }
}
