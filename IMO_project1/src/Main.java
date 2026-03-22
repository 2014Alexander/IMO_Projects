import experiment.run.InstancesExperimentRunner;
import experiment.summary.InstanceExperimentResult;
import io.CsvInstanceReader;
import model.Instance;
import output.console.BestCyclesConsolePrinter;
import output.console.ExperimentConsolePrinter;
import output.file.ExperimentFilesWriter;

import java.io.IOException;
import java.util.List;

public class Main {

    private static final String TSPA_PATH = "data/TSPA.csv";
    private static final String TSPB_PATH = "data/TSPB.csv";

    public static void main(String[] args) throws IOException {
        List<InstanceExperimentResult> results = runExperiments();

        printResults(results);
        saveResults(results);
    }

    private static List<InstanceExperimentResult> runExperiments() {
        CsvInstanceReader reader = new CsvInstanceReader();

        Instance tspa = reader.read(TSPA_PATH);
        Instance tspb = reader.read(TSPB_PATH);

        List<Instance> instances = List.of(tspa, tspb);

        InstancesExperimentRunner runner = new InstancesExperimentRunner();
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