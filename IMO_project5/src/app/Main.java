package app;

import experiment.lab5.Lab5ExperimentRunner;
import experiment.lab5.Lab5InstanceResult;
import experiment.lab6.Lab6ExperimentRunner;
import experiment.summary.InstanceExperimentResult;
import io.CsvInstanceReader;
import model.Instance;
import output.console.BestCyclesConsolePrinter;
import output.console.ExperimentConsolePrinter;
import output.file.ExperimentFilesWriter;
import output.file.lab5.Lab5FilesWriter;

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
        List<Lab5InstanceResult> lab5Results = runLab5Experiments(instances);
        saveLab5Results(lab5Results);

        List<InstanceExperimentResult> lab6Results = runLab6Experiments(instances);

        printLab6Results(lab6Results);
        saveLab6Results(lab6Results);
    }

    private static List<Instance> loadInstances() {
        CsvInstanceReader reader = new CsvInstanceReader();

        Instance tspa = reader.read(TSPA_PATH);
        Instance tspb = reader.read(TSPB_PATH);

        return List.of(tspa, tspb);
    }

    private static List<Lab5InstanceResult> runLab5Experiments(List<Instance> instances) {
        Lab5ExperimentRunner runner = new Lab5ExperimentRunner(BASE_SEED);
        return runner.run(instances);
    }

    private static List<InstanceExperimentResult> runLab6Experiments(List<Instance> instances) {
        Lab6ExperimentRunner runner = new Lab6ExperimentRunner(BASE_SEED);
        return runner.run(instances);
    }

    private static void printLab6Results(List<InstanceExperimentResult> results) {
        ExperimentConsolePrinter experimentPrinter = new ExperimentConsolePrinter();
        experimentPrinter.print(results);

        System.out.println();
        System.out.println("Best cycles:");
        System.out.println();

        BestCyclesConsolePrinter bestCyclesPrinter = new BestCyclesConsolePrinter();
        bestCyclesPrinter.print(results);
    }

    private static void saveLab5Results(List<Lab5InstanceResult> results) throws IOException {
        Lab5FilesWriter filesWriter = new Lab5FilesWriter();
        filesWriter.writeAll(java.nio.file.Path.of("data", "out", "lab5"), results);
    }

    private static void saveLab6Results(List<InstanceExperimentResult> results) throws IOException {
        ExperimentFilesWriter filesWriter = new ExperimentFilesWriter();
        filesWriter.writeAll(java.nio.file.Path.of("data", "out", "lab6"), results);
    }
}
