package output.file;

import experiment.summary.AlgorithmExperimentSummary;
import experiment.summary.InstanceExperimentResult;
import experiment.summary.IterationStatistics;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class AlgorithmIterationStatisticsCsvWriter {

    private static final String FILE_NAME = "algorithm_iteration_statistics.csv";
    private static final String HEADER =
            "summaryGroup,instanceName,algorithmName,runsCount,avgIterationCount,minIterationCount,maxIterationCount";

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
            IterationStatistics statistics = summary.iterationStatistics();
            if (statistics != null) {
                appendRow(sb, "main", statistics);
            }
        }

        for (AlgorithmExperimentSummary summary : result.referenceSummaries()) {
            IterationStatistics statistics = summary.iterationStatistics();
            if (statistics != null) {
                appendRow(sb, "reference", statistics);
            }
        }

        return sb.toString();
    }

    private void appendRow(StringBuilder sb, String summaryGroup, IterationStatistics statistics) {
        sb.append(escape(summaryGroup)).append(",");
        sb.append(escape(statistics.instanceName())).append(",");
        sb.append(escape(statistics.algorithmName())).append(",");
        sb.append(statistics.runsCount()).append(",");
        sb.append(formatDouble(statistics.avgIterationCount())).append(",");
        sb.append(statistics.minIterationCount()).append(",");
        sb.append(statistics.maxIterationCount()).append(System.lineSeparator());
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
