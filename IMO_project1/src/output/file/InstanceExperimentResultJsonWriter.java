package output.file;

import evaluation.SolutionMetrics;
import execute.ExecutionResult;
import experiment.summary.InstanceExperimentResult;
import experiment.summary.SinglePhaseExperimentSummary;
import experiment.summary.TwoPhaseExperimentSummary;
import model.Solution;
import statistics.ObjectiveStatistics;
import statistics.TourLengthStatistics;

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
        validateArguments(instanceDirectory, result);

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

    private static void validateArguments(Path instanceDirectory, InstanceExperimentResult result) {
        if (instanceDirectory == null) {
            throw new IllegalArgumentException("instanceDirectory cannot be null");
        }
        if (result == null) {
            throw new IllegalArgumentException("result cannot be null");
        }
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
        sb.append("\"singlePhaseSummaries\": [").append(NL);
        appendSinglePhaseSummaries(sb, result.singlePhaseSummaries(), level + 2);
        appendIndent(sb, level + 1);
        sb.append("],").append(NL);

        appendIndent(sb, level + 1);
        sb.append("\"twoPhaseSummaries\": [").append(NL);
        appendTwoPhaseSummaries(sb, result.twoPhaseSummaries(), level + 2);
        appendIndent(sb, level + 1);
        sb.append("]").append(NL);

        appendIndent(sb, level);
        sb.append("}");
    }

    private void appendSinglePhaseSummaries(
            StringBuilder sb,
            List<SinglePhaseExperimentSummary> summaries,
            int level
    ) {
        for (int i = 0; i < summaries.size(); i++) {
            appendSinglePhaseSummary(sb, summaries.get(i), level);

            if (i < summaries.size() - 1) {
                sb.append(",");
            }
            sb.append(NL);
        }
    }

    private void appendTwoPhaseSummaries(
            StringBuilder sb,
            List<TwoPhaseExperimentSummary> summaries,
            int level
    ) {
        for (int i = 0; i < summaries.size(); i++) {
            appendTwoPhaseSummary(sb, summaries.get(i), level);

            if (i < summaries.size() - 1) {
                sb.append(",");
            }
            sb.append(NL);
        }
    }

    private void appendSinglePhaseSummary(
            StringBuilder sb,
            SinglePhaseExperimentSummary summary,
            int level
    ) {
        appendIndent(sb, level);
        sb.append("{").append(NL);

        appendIndent(sb, level + 1);
        sb.append("\"instanceName\": ").append(jsonString(summary.instanceName())).append(",").append(NL);

        appendIndent(sb, level + 1);
        sb.append("\"algorithmName\": ").append(jsonString(summary.algorithmName())).append(",").append(NL);

        appendIndent(sb, level + 1);
        sb.append("\"objectiveStatistics\": ");
        appendObjectiveStatistics(sb, summary.objectiveStatistics(), level + 1);
        sb.append(",").append(NL);

        appendIndent(sb, level + 1);
        sb.append("\"bestResult\": ");
        appendExecutionResult(sb, summary.bestResult(), level + 1);
        sb.append(",").append(NL);

        appendIndent(sb, level + 1);
        sb.append("\"worstResult\": ");
        appendExecutionResult(sb, summary.worstResult(), level + 1);
        sb.append(NL);

        appendIndent(sb, level);
        sb.append("}");
    }

    private void appendTwoPhaseSummary(
            StringBuilder sb,
            TwoPhaseExperimentSummary summary,
            int level
    ) {
        appendIndent(sb, level);
        sb.append("{").append(NL);

        appendIndent(sb, level + 1);
        sb.append("\"instanceName\": ").append(jsonString(summary.instanceName())).append(",").append(NL);

        appendIndent(sb, level + 1);
        sb.append("\"algorithmName\": ").append(jsonString(summary.algorithmName())).append(",").append(NL);

        appendIndent(sb, level + 1);
        sb.append("\"phaseOneTourLengthStatistics\": ");
        appendTourLengthStatistics(sb, summary.phaseOneTourLengthStatistics(), level + 1);
        sb.append(",").append(NL);

        appendIndent(sb, level + 1);
        sb.append("\"finalObjectiveStatistics\": ");
        appendObjectiveStatistics(sb, summary.finalObjectiveStatistics(), level + 1);
        sb.append(",").append(NL);

        appendIndent(sb, level + 1);
        sb.append("\"bestFinalResult\": ");
        appendExecutionResult(sb, summary.bestFinalResult(), level + 1);
        sb.append(",").append(NL);

        appendIndent(sb, level + 1);
        sb.append("\"worstFinalResult\": ");
        appendExecutionResult(sb, summary.worstFinalResult(), level + 1);
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

    private void appendTourLengthStatistics(
            StringBuilder sb,
            TourLengthStatistics statistics,
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
        sb.append("\"minTourLength\": ").append(statistics.minTourLength()).append(",").append(NL);

        appendIndent(sb, level + 1);
        sb.append("\"maxTourLength\": ").append(statistics.maxTourLength()).append(",").append(NL);

        appendIndent(sb, level + 1);
        sb.append("\"avgTourLength\": ").append(formatDouble(statistics.avgTourLength())).append(NL);

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
        appendIntegerList(sb, solution.cycle(), level + 1);
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
        sb.append("\"profitSum\": ").append(metrics.profitSum()).append(",").append(NL);

        appendIndent(sb, level + 1);
        sb.append("\"tourLength\": ").append(metrics.tourLength()).append(",").append(NL);

        appendIndent(sb, level + 1);
        sb.append("\"objective\": ").append(metrics.objective()).append(NL);

        appendIndent(sb, level);
        sb.append("}");
    }

    private void appendIntegerList(
            StringBuilder sb,
            List<Integer> values,
            int level
    ) {
        sb.append("[");

        if (!values.isEmpty()) {
            sb.append(NL);

            for (int i = 0; i < values.size(); i++) {
                appendIndent(sb, level + 1);
                sb.append(values.get(i));

                if (i < values.size() - 1) {
                    sb.append(",");
                }
                sb.append(NL);
            }

            appendIndent(sb, level);
        }

        sb.append("]");
    }

    private static void appendIndent(StringBuilder sb, int level) {
        for (int i = 0; i < level; i++) {
            sb.append(INDENT);
        }
    }

    private static String jsonString(String value) {
        return "\"" + escape(value) + "\"";
    }

    private static String escape(String value) {
        StringBuilder escaped = new StringBuilder();

        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);

            switch (ch) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> escaped.append(ch);
            }
        }

        return escaped.toString();
    }

    private static String formatDouble(double value) {
        return Double.toString(value);
    }
}