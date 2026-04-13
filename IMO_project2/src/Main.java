import experiment.run.InstancesExperimentRunner;
import experiment.summary.InstanceExperimentResult;
import io.CsvInstanceReader;
import model.Instance;
import output.console.BestCyclesConsolePrinter;
import output.console.ExperimentConsolePrinter;
import output.file.ExperimentFilesWriter;

import java.io.IOException;
import java.util.List;

public final class Main {

    private static final String TSPA_PATH = "data/TSPA.csv";
    private static final String TSPB_PATH = "data/TSPB.csv";

    /*
     * Jeden seed steruje całym eksperymentem.
     * Zmiana tej wartości zmienia w pełni powtarzalny przebieg wszystkich uruchomień.
     */
    private static final long BASE_SEED = 123456789L;

    public static void main(String[] args) throws IOException {
        List<Instance> instances = loadInstances();
        List<InstanceExperimentResult> results = runExperiments(instances);

        printResults(results);
        saveResults(results);
    }

    private static List<Instance> loadInstances() {
        CsvInstanceReader reader = new CsvInstanceReader();

        Instance tspa = reader.read(TSPA_PATH);
        Instance tspb = reader.read(TSPB_PATH);

        return List.of(tspa, tspb);
    }

    private static List<InstanceExperimentResult> runExperiments(List<Instance> instances) {
        InstancesExperimentRunner runner = new InstancesExperimentRunner(BASE_SEED);
        return runner.run(instances);
    }

    private static void printResults(List<InstanceExperimentResult> results) {
        ExperimentConsolePrinter experimentPrinter = new ExperimentConsolePrinter();
        experimentPrinter.print(results);

        System.out.println();
        System.out.println("Best cycles:");
        System.out.println();

        BestCyclesConsolePrinter bestCyclesPrinter = new BestCyclesConsolePrinter();
        bestCyclesPrinter.print(results);
    }

    private static void saveResults(List<InstanceExperimentResult> results) throws IOException {
        ExperimentFilesWriter filesWriter = new ExperimentFilesWriter();
        filesWriter.writeAll(results);
    }
}
