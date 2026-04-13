package output.file;

import experiment.summary.AlgorithmExperimentSummary;
import experiment.summary.InstanceExperimentResult;
import statistics.RuntimeStatistics;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class AlgorithmRuntimeStatisticsCsvWriter {

    private static final String FILE_NAME = "algorithm_runtime_statistics.csv";
    private static final String HEADER =
            "instanceName,algorithmName,runsCount,avgRuntimeNanos,minRuntimeNanos,maxRuntimeNanos";

    public Path write(Path instanceDirectory, InstanceExperimentResult result) throws IOException {
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

    private String buildCsv(InstanceExperimentResult result) {
        StringBuilder sb = new StringBuilder();

        sb.append(HEADER).append(System.lineSeparator());

        for (AlgorithmExperimentSummary summary : result.algorithmSummaries()) {
            appendRow(sb, summary.runtimeStatistics());
        }

        return sb.toString();
    }

    private void appendRow(StringBuilder sb, RuntimeStatistics statistics) {
        sb.append(escape(statistics.instanceName())).append(",");
        sb.append(escape(statistics.algorithmName())).append(",");
        sb.append(statistics.runsCount()).append(",");
        sb.append(formatDouble(statistics.avgRuntimeNanos())).append(",");
        sb.append(statistics.minRuntimeNanos()).append(",");
        sb.append(statistics.maxRuntimeNanos()).append(System.lineSeparator());
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
