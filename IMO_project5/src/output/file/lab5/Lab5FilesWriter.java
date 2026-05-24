package output.file.lab5;

import experiment.lab5.Lab5InstanceResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public final class Lab5FilesWriter {
    private final Lab5InstanceResultJsonWriter jsonWriter;
    private final Lab5PointsCsvWriter pointsCsvWriter;
    private final Lab5CorrelationsCsvWriter correlationsCsvWriter;

    public Lab5FilesWriter() {
        this.jsonWriter = new Lab5InstanceResultJsonWriter();
        this.pointsCsvWriter = new Lab5PointsCsvWriter();
        this.correlationsCsvWriter = new Lab5CorrelationsCsvWriter();
    }

    public Path writeAll(Path outputDirectory, List<Lab5InstanceResult> instanceResults) throws IOException {
        recreateOutputDirectory(outputDirectory);

        for (Lab5InstanceResult instanceResult : instanceResults) {
            writeInstanceResult(outputDirectory, instanceResult);
        }

        return outputDirectory;
    }

    private void writeInstanceResult(
        Path outputDirectory,
        Lab5InstanceResult instanceResult
    ) throws IOException {
        Path instanceDirectory = outputDirectory.resolve(instanceResult.instanceName());
        Files.createDirectories(instanceDirectory);
        jsonWriter.write(instanceDirectory, instanceResult);
        pointsCsvWriter.write(instanceDirectory, instanceResult);
        correlationsCsvWriter.write(instanceDirectory, instanceResult);
    }

    private static void recreateOutputDirectory(Path outputDirectory) throws IOException {
        if (Files.exists(outputDirectory)) {
            clearDirectory(outputDirectory);
        }

        Files.createDirectories(outputDirectory);
    }

    private static void clearDirectory(Path directory) throws IOException {
        try (Stream<Path> paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(Lab5FilesWriter::deletePath);
        }
    }

    private static void deletePath(Path path) {
        try {
            Files.delete(path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete path: " + path, e);
        }
    }
}
