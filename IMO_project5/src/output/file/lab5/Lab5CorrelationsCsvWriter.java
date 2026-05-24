package output.file.lab5;

import experiment.lab5.Lab5CorrelationSummary;
import experiment.lab5.Lab5InstanceResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Lab5CorrelationsCsvWriter {
    private static final String FILE_NAME = "lab5_correlations.csv";
    private static final String HEADER =
        "instanceName,correlationObjectiveVsVerticesToBest,correlationObjectiveVsEdgesToBest,"
            + "correlationObjectiveVsAvgVerticesToOthers,correlationObjectiveVsAvgEdgesToOthers";

    public Path write(Path instanceDirectory, Lab5InstanceResult result) throws IOException {
        Files.createDirectories(instanceDirectory);
        Path outputFile = instanceDirectory.resolve(FILE_NAME);
        Files.writeString(outputFile, buildContent(result), StandardCharsets.UTF_8);
        return outputFile;
    }

    private String buildContent(Lab5InstanceResult result) {
        Lab5CorrelationSummary summary = result.correlations();
        StringBuilder sb = new StringBuilder();
        sb.append(HEADER).append(System.lineSeparator());
        sb.append(escape(result.instanceName())).append(",");
        sb.append(summary.objectiveVsVerticesToBest()).append(",");
        sb.append(summary.objectiveVsEdgesToBest()).append(",");
        sb.append(summary.objectiveVsAvgVerticesToOthers()).append(",");
        sb.append(summary.objectiveVsAvgEdgesToOthers()).append(System.lineSeparator());
        return sb.toString();
    }

    private static String escape(String value) {
        if (value.indexOf(',') >= 0 || value.indexOf('"') >= 0 || value.indexOf('\n') >= 0) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
