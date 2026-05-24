package output.file.lab5;

import experiment.lab5.Lab5BestSolutionMetadata;
import experiment.lab5.Lab5CorrelationSummary;
import experiment.lab5.Lab5InstanceResult;
import experiment.lab5.Lab5Point;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class Lab5InstanceResultJsonWriter {
    private static final String FILE_NAME = "lab5_metadata.json";
    private static final String NL = System.lineSeparator();

    public Path write(Path instanceDirectory, Lab5InstanceResult result) throws IOException {
        Files.createDirectories(instanceDirectory);
        Path outputFile = instanceDirectory.resolve(FILE_NAME);
        Files.writeString(outputFile, buildJson(result), StandardCharsets.UTF_8);
        return outputFile;
    }

    private String buildJson(Lab5InstanceResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("{").append(NL);
        appendField(sb, 1, "instanceName", result.instanceName(), true);
        appendField(sb, 1, "localOptimaCount", result.localOptimaCount(), true);
        appendField(sb, 1, "localSearch", result.localSearchName(), true);
        appendField(sb, 1, "neighborhood", result.neighborhoodName(), true);
        appendBestSolution(sb, result.bestSolution());
        sb.append(",").append(NL);
        appendCorrelations(sb, result.correlations());
        sb.append(",").append(NL);
        appendPointsPreview(sb, result.points());
        sb.append(NL).append("}").append(NL);
        return sb.toString();
    }

    private void appendBestSolution(StringBuilder sb, Lab5BestSolutionMetadata bestSolution) {
        indent(sb, 1);
        sb.append("\"bestSolution\": {").append(NL);
        appendField(sb, 2, "sourceAlgorithmName", bestSolution.sourceAlgorithmName(), true);
        appendField(sb, 2, "objective", bestSolution.objective(), true);
        appendField(sb, 2, "startVertexId", bestSolution.startVertexId(), true);
        appendField(sb, 2, "runSeed", bestSolution.runSeed(), true);
        indent(sb, 2);
        sb.append("\"selectionPoolAlgorithms\": [");
        for (int index = 0; index < bestSolution.selectionPoolAlgorithms().size(); index++) {
            if (index > 0) {
                sb.append(", ");
            }
            sb.append("\"").append(escape(bestSolution.selectionPoolAlgorithms().get(index))).append("\"");
        }
        sb.append("]").append(NL);
        indent(sb, 1);
        sb.append("}");
    }

    private void appendCorrelations(StringBuilder sb, Lab5CorrelationSummary correlations) {
        indent(sb, 1);
        sb.append("\"correlations\": {").append(NL);
        appendField(sb, 2, "objectiveVsVerticesToBest", correlations.objectiveVsVerticesToBest(), true);
        appendField(sb, 2, "objectiveVsEdgesToBest", correlations.objectiveVsEdgesToBest(), true);
        appendField(sb, 2, "objectiveVsAvgVerticesToOthers", correlations.objectiveVsAvgVerticesToOthers(), true);
        appendField(sb, 2, "objectiveVsAvgEdgesToOthers", correlations.objectiveVsAvgEdgesToOthers(), false);
        indent(sb, 1);
        sb.append("}");
    }

    private void appendPointsPreview(StringBuilder sb, List<Lab5Point> points) {
        indent(sb, 1);
        sb.append("\"pointsFile\": \"lab5_points.csv\",").append(NL);
        indent(sb, 1);
        sb.append("\"pointsCount\": ").append(points.size());
    }

    private void appendField(StringBuilder sb, int level, String name, String value, boolean trailingComma) {
        indent(sb, level);
        sb.append("\"").append(name).append("\": \"").append(escape(value)).append("\"");
        if (trailingComma) {
            sb.append(",");
        }
        sb.append(NL);
    }

    private void appendField(StringBuilder sb, int level, String name, long value, boolean trailingComma) {
        indent(sb, level);
        sb.append("\"").append(name).append("\": ").append(value);
        if (trailingComma) {
            sb.append(",");
        }
        sb.append(NL);
    }

    private void appendField(StringBuilder sb, int level, String name, int value, boolean trailingComma) {
        appendField(sb, level, name, (long) value, trailingComma);
    }

    private void appendField(StringBuilder sb, int level, String name, double value, boolean trailingComma) {
        indent(sb, level);
        sb.append("\"").append(name).append("\": ").append(value);
        if (trailingComma) {
            sb.append(",");
        }
        sb.append(NL);
    }

    private static void indent(StringBuilder sb, int level) {
        sb.append("  ".repeat(level));
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
