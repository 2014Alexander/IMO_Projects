package output.file;

import evaluation.SolutionMetrics;
import execute.ExecutionResult;
import experiment.summary.AlgorithmExperimentSummary;
import experiment.summary.InstanceExperimentResult;
import model.Solution;
import statistics.ObjectiveStatistics;
import statistics.RuntimeStatistics;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

public final class InstanceExperimentResultJsonWriter {

    private static final String FILE_NAME = "instance_experiment_result.json";
    private static final String INDENT = "    ";
    private static final String NL = System.lineSeparator();

    public Path write(Path instanceDirectory, InstanceExperimentResult result) throws IOException {
        Files.createDirectories(instanceDirectory);

        Path filePath = instanceDirectory.resolve(FILE_NAME);
        String json = toJson(result);

        Files.writeString(
                filePath,
                json,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        );

        return filePath;
    }

    private String toJson(InstanceExperimentResult result) {
        StringBuilder sb = new StringBuilder();

        appendInstanceExperimentResult(sb, result, 0);
        sb.append(NL);

        return sb.toString();
    }

    private void appendInstanceExperimentResult(
            StringBuilder sb,
            InstanceExperimentResult result,
            int level
    ) {
        sb.append("{").append(NL);

        appendIndent(sb, level + 1);
        sb.append("\"instanceName\": ").append(jsonString(result.instanceName())).append(",").append(NL);

        appendIndent(sb, level + 1);
        sb.append("\"algorithmSummaries\": [").append(NL);
        appendAlgorithmSummaries(sb, result.algorithmSummaries(), level + 2);
        appendIndent(sb, level + 1);
        sb.append("]").append(NL);

        appendIndent(sb, level);
        sb.append("}");
    }

    private void appendAlgorithmSummaries(
            StringBuilder sb,
            List<AlgorithmExperimentSummary> summaries,
            int level
    ) {
        for (int i = 0; i < summaries.size(); i++) {
            appendAlgorithmSummary(sb, summaries.get(i), level);

            if (i < summaries.size() - 1) {
                sb.append(",");
            }
            sb.append(NL);
        }
    }

    private void appendAlgorithmSummary(
            StringBuilder sb,
            AlgorithmExperimentSummary summary,
            int level
    ) {
        appendIndent(sb, level);
        sb.append("{").append(NL);

        appendIndent(sb, level + 1);
        sb.append("\"algorithmName\": ").append(jsonString(summary.algorithmName())).append(",").append(NL);

        appendIndent(sb, level + 1);
        sb.append("\"objectiveStatistics\": ");
        appendObjectiveStatistics(sb, summary.objectiveStatistics(), level + 1);
        sb.append(",").append(NL);

        appendIndent(sb, level + 1);
        sb.append("\"runtimeStatistics\": ");
        appendRuntimeStatistics(sb, summary.runtimeStatistics(), level + 1);
        sb.append(",").append(NL);

        appendIndent(sb, level + 1);
        sb.append("\"bestExecutionResult\": ");
        appendExecutionResult(sb, summary.bestExecutionResult(), level + 1);
        sb.append(NL);

        appendIndent(sb, level);
        sb.append("}");
    }

    private void appendObjectiveStatistics(
            StringBuilder sb,
            ObjectiveStatistics statistics,
            int level
    ) {
        sb.append("{").append(NL);

        appendIndent(sb, level + 1);
        sb.append("\"instanceName\": ").append(jsonString(statistics.instanceName())).append(",").append(NL);

        appendIndent(sb, level + 1);
        sb.append("\"algorithmName\": ").append(jsonString(statistics.algorithmName())).append(",").append(NL);

        appendIndent(sb, level + 1);
        sb.append("\"runsCount\": ").append(statistics.runsCount()).append(",").append(NL);

        appendIndent(sb, level + 1);
        sb.append("\"minObjective\": ").append(statistics.minObjective()).append(",").append(NL);

        appendIndent(sb, level + 1);
        sb.append("\"maxObjective\": ").append(statistics.maxObjective()).append(",").append(NL);

        appendIndent(sb, level + 1);
        sb.append("\"avgObjective\": ").append(formatDouble(statistics.avgObjective())).append(NL);

        appendIndent(sb, level);
        sb.append("}");
    }

    private void appendRuntimeStatistics(
            StringBuilder sb,
            RuntimeStatistics statistics,
            int level
    ) {
        sb.append("{").append(NL);

        appendIndent(sb, level + 1);
        sb.append("\"instanceName\": ").append(jsonString(statistics.instanceName())).append(",").append(NL);

        appendIndent(sb, level + 1);
        sb.append("\"algorithmName\": ").append(jsonString(statistics.algorithmName())).append(",").append(NL);

        appendIndent(sb, level + 1);
        sb.append("\"runsCount\": ").append(statistics.runsCount()).append(",").append(NL);

        appendIndent(sb, level + 1);
        sb.append("\"minRuntimeNanos\": ").append(statistics.minRuntimeNanos()).append(",").append(NL);

        appendIndent(sb, level + 1);
        sb.append("\"maxRuntimeNanos\": ").append(statistics.maxRuntimeNanos()).append(",").append(NL);

        appendIndent(sb, level + 1);
        sb.append("\"avgRuntimeNanos\": ").append(formatDouble(statistics.avgRuntimeNanos())).append(NL);

        appendIndent(sb, level);
        sb.append("}");
    }

    private void appendExecutionResult(
            StringBuilder sb,
            ExecutionResult result,
            int level
    ) {
        sb.append("{").append(NL);

        appendIndent(sb, level + 1);
        sb.append("\"algorithmName\": ").append(jsonString(result.algorithmName())).append(",").append(NL);

        appendIndent(sb, level + 1);
        sb.append("\"solution\": ");
        appendSolution(sb, result.solution(), level + 1);
        sb.append(",").append(NL);

        appendIndent(sb, level + 1);
        sb.append("\"solutionMetrics\": ");
        appendSolutionMetrics(sb, result.solutionMetrics(), level + 1);
        sb.append(",").append(NL);

        appendIndent(sb, level + 1);
        sb.append("\"runtimeNanos\": ").append(result.runtimeNanos()).append(NL);

        appendIndent(sb, level);
        sb.append("}");
    }

    private void appendSolution(
            StringBuilder sb,
            Solution solution,
            int level
    ) {
        sb.append("{").append(NL);

        appendIndent(sb, level + 1);
        sb.append("\"instanceName\": ").append(jsonString(solution.instanceName())).append(",").append(NL);

        appendIndent(sb, level + 1);
        sb.append("\"startVertexId\": ").append(solution.startVertexId()).append(",").append(NL);

        appendIndent(sb, level + 1);
        sb.append("\"cycle\": ");
        appendIntegerArray(sb, solution.cycle(), level + 1);
        sb.append(NL);

        appendIndent(sb, level);
        sb.append("}");
    }

    private void appendSolutionMetrics(
            StringBuilder sb,
            SolutionMetrics metrics,
            int level
    ) {
        sb.append("{").append(NL);

        appendIndent(sb, level + 1);
        sb.append("\"tourLength\": ").append(metrics.tourLength()).append(",").append(NL);

        appendIndent(sb, level + 1);
        sb.append("\"profitSum\": ").append(metrics.profitSum()).append(",").append(NL);

        appendIndent(sb, level + 1);
        sb.append("\"objective\": ").append(metrics.objective()).append(NL);

        appendIndent(sb, level);
        sb.append("}");
    }

    private void appendIntegerArray(
            StringBuilder sb,
            List<Integer> values,
            int level
    ) {
        sb.append("[");

        for (int i = 0; i < values.size(); i++) {
            sb.append(values.get(i));

            if (i < values.size() - 1) {
                sb.append(", ");
            }
        }

        sb.append("]");
    }

    private static void appendIndent(StringBuilder sb, int level) {
        for (int i = 0; i < level; i++) {
            sb.append(INDENT);
        }
    }

    private static String jsonString(String value) {
        return "\"" + escapeJson(value) + "\"";
    }

    private static String escapeJson(String value) {
        StringBuilder sb = new StringBuilder(value.length());

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);

            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }

        return sb.toString();
    }

    private static String formatDouble(double value) {
        return Double.toString(value);
    }
}
