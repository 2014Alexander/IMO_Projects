package output.file;

import experiment.summary.InstanceExperimentResult;
import experiment.summary.SinglePhaseExperimentSummary;
import statistics.ObjectiveStatistics;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class SinglePhaseObjectiveStatisticsCsvWriter {

    private static final String FILE_NAME = "single_phase_objective_statistics.csv";
    private static final String HEADER =
            "instanceName,algorithmName,runsCount,avgObjective,minObjective,maxObjective";

    public Path write(Path instanceDirectory, InstanceExperimentResult result) throws IOException {
        validateArguments(instanceDirectory, result);

        Files.createDirectories(instanceDirectory);

        Path filePath = instanceDirectory.resolve(FILE_NAME);
        String csv = buildCsv(result);

        Files.writeString(
                filePath,
                csv,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        );

        return filePath;
    }

    private static void validateArguments(Path instanceDirectory, InstanceExperimentResult result) {
        if (instanceDirectory == null) {
            throw new IllegalArgumentException("instanceDirectory cannot be null");
        }
        if (result == null) {
            throw new IllegalArgumentException("result cannot be null");
        }
    }

    private String buildCsv(InstanceExperimentResult result) {
        StringBuilder sb = new StringBuilder();

        sb.append(HEADER).append(System.lineSeparator());

        for (SinglePhaseExperimentSummary summary : result.singlePhaseSummaries()) {
            appendRow(sb, summary.objectiveStatistics());
        }

        return sb.toString();
    }

    private void appendRow(StringBuilder sb, ObjectiveStatistics statistics) {
        sb.append(escape(statistics.instanceName())).append(",");
        sb.append(escape(statistics.algorithmName())).append(",");
        sb.append(statistics.runsCount()).append(",");
        sb.append(formatDouble(statistics.avgObjective())).append(",");
        sb.append(statistics.minObjective()).append(",");
        sb.append(statistics.maxObjective()).append(System.lineSeparator());
    }

    private static String formatDouble(double value) {
        return Double.toString(value);
    }

    private static String escape(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}