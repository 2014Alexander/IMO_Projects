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
    private final SinglePhaseObjectiveStatisticsCsvWriter singlePhaseObjectiveStatisticsCsvWriter;
    private final TwoPhaseFinalObjectiveStatisticsCsvWriter twoPhaseFinalObjectiveStatisticsCsvWriter;
    private final TwoPhasePhaseOneTourLengthStatisticsCsvWriter twoPhasePhaseOneTourLengthStatisticsCsvWriter;

    public ExperimentFilesWriter() {
        this.jsonWriter = new InstanceExperimentResultJsonWriter();
        this.singlePhaseObjectiveStatisticsCsvWriter = new SinglePhaseObjectiveStatisticsCsvWriter();
        this.twoPhaseFinalObjectiveStatisticsCsvWriter = new TwoPhaseFinalObjectiveStatisticsCsvWriter();
        this.twoPhasePhaseOneTourLengthStatisticsCsvWriter = new TwoPhasePhaseOneTourLengthStatisticsCsvWriter();
    }

    public Path writeAll(List<InstanceExperimentResult> instanceResults) throws IOException {
        return writeAll(DEFAULT_OUTPUT_DIRECTORY, instanceResults);
    }

    public Path writeAll(Path outputDirectory, List<InstanceExperimentResult> instanceResults) throws IOException {
        validateArguments(outputDirectory, instanceResults);

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
        singlePhaseObjectiveStatisticsCsvWriter.write(instanceDirectory, instanceResult);
        twoPhaseFinalObjectiveStatisticsCsvWriter.write(instanceDirectory, instanceResult);
        twoPhasePhaseOneTourLengthStatisticsCsvWriter.write(instanceDirectory, instanceResult);
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
                    .forEach(path -> deletePath(path));
        }
    }

    private static void deletePath(Path path) {
        try {
            Files.delete(path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete path: " + path, e);
        }
    }

    private static void validateArguments(
            Path outputDirectory,
            List<InstanceExperimentResult> instanceResults
    ) {
        if (outputDirectory == null) {
            throw new IllegalArgumentException("outputDirectory cannot be null");
        }
        if (instanceResults == null) {
            throw new IllegalArgumentException("instanceResults cannot be null");
        }
        if (instanceResults.isEmpty()) {
            throw new IllegalArgumentException("instanceResults cannot be empty");
        }

        for (InstanceExperimentResult instanceResult : instanceResults) {
            if (instanceResult == null) {
                throw new IllegalArgumentException("instanceResults cannot contain null");
            }
        }
    }
}