package output.file.lab5;

import experiment.lab5.Lab5InstanceResult;
import experiment.lab5.Lab5Point;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Lab5PointsCsvWriter {
    private static final String FILE_NAME = "lab5_points.csv";
    private static final String HEADER =
        "instanceName,localOptimumIndex,startVertexId,runSeed,objective,"
            + "similarityVerticesToBest,similarityEdgesToBest,"
            + "avgSimilarityVerticesToOthers,avgSimilarityEdgesToOthers";

    public Path write(Path instanceDirectory, Lab5InstanceResult result) throws IOException {
        Files.createDirectories(instanceDirectory);
        Path outputFile = instanceDirectory.resolve(FILE_NAME);
        Files.writeString(outputFile, buildContent(result), StandardCharsets.UTF_8);
        return outputFile;
    }

    private String buildContent(Lab5InstanceResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append(HEADER).append(System.lineSeparator());

        for (Lab5Point point : result.points()) {
            sb.append(escape(result.instanceName())).append(",");
            sb.append(point.localOptimumIndex()).append(",");
            sb.append(point.startVertexId()).append(",");
            sb.append(point.runSeed()).append(",");
            sb.append(point.objective()).append(",");
            sb.append(point.similarityVerticesToBest()).append(",");
            sb.append(point.similarityEdgesToBest()).append(",");
            sb.append(point.avgSimilarityVerticesToOthers()).append(",");
            sb.append(point.avgSimilarityEdgesToOthers()).append(System.lineSeparator());
        }

        return sb.toString();
    }

    private static String escape(String value) {
        if (value.indexOf(',') >= 0 || value.indexOf('"') >= 0 || value.indexOf('\n') >= 0) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
