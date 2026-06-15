package app;

import experiment.core.RunConfig;
import experiment.core.RunPreparator;
import io.CsvInstanceReader;
import model.Instance;

import java.util.List;

public final class PrintRuns {
    public static void main(String[] args) {
        Instance instance = new CsvInstanceReader().read(args[0]);
        List<RunConfig> runs = RunPreparator.prepareRuns(instance, 12345L, 20);
        for (int i = 0; i < runs.size(); i++) {
            RunConfig run = runs.get(i);
            System.out.println(i + "," + run.startVertexId() + "," + run.runSeed());
        }
    }
}
