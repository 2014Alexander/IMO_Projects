package output.file;

import experiment.summary.InstanceExperimentResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public final class ExperimentFilesWriter {

    private static final Path DEFAULT_OUTPUT_DIRECTORY = Path.of("data", "out");

    private final InstanceExperimentResultJsonWriter jsonWriter;
    private final AlgorithmObjectiveStatisticsCsvWriter objectiveStatisticsCsvWriter;
    private final AlgorithmRuntimeStatisticsCsvWriter runtimeStatisticsCsvWriter;

    public ExperimentFilesWriter() {
        this.jsonWriter = new InstanceExperimentResultJsonWriter();
        this.objectiveStatisticsCsvWriter = new AlgorithmObjectiveStatisticsCsvWriter();
        this.runtimeStatisticsCsvWriter = new AlgorithmRuntimeStatisticsCsvWriter();
    }

    public Path writeAll(List<InstanceExperimentResult> instanceResults) throws IOException {
        return writeAll(DEFAULT_OUTPUT_DIRECTORY, instanceResults);
    }

    public Path writeAll(Path outputDirectory, List<InstanceExperimentResult> instanceResults) throws IOException {
        recreateOutputDirectory(outputDirectory);

        for (InstanceExperimentResult instanceResult : instanceResults) {
            writeInstanceResult(outputDirectory, instanceResult);
        }

        return outputDirectory;
    }

    private void writeInstanceResult(
            Path outputDirectory,
            InstanceExperimentResult instanceResult
    ) throws IOException {
        Path instanceDirectory = outputDirectory.resolve(instanceResult.instanceName());

        Files.createDirectories(instanceDirectory);

        jsonWriter.write(instanceDirectory, instanceResult);
        objectiveStatisticsCsvWriter.write(instanceDirectory, instanceResult);
        runtimeStatisticsCsvWriter.write(instanceDirectory, instanceResult);
    }

    private static void recreateOutputDirectory(Path outputDirectory) throws IOException {
        if (Files.exists(outputDirectory)) {
            clearDirectory(outputDirectory);
        }

        Files.createDirectories(outputDirectory);
    }

    private static void clearDirectory(Path directory) throws IOException {
        try (Stream<Path> paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(ExperimentFilesWriter::deletePath);
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
