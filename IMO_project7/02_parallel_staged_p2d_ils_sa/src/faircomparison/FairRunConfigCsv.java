package faircomparison;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class FairRunConfigCsv {
    private FairRunConfigCsv() {
    }

    public static void write(Path path, List<FairRunConfig> configs) throws IOException {
        Files.createDirectories(path.getParent());
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write("runIndex,startVertexId,runSeed");
            writer.newLine();
            for (FairRunConfig config : configs) {
                writer.write(config.runIndex() + "," + config.startVertexId() + "," + config.runSeed());
                writer.newLine();
            }
        }
    }

    public static List<FairRunConfig> read(Path path) throws IOException {
        List<FairRunConfig> configs = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String header = reader.readLine();
            if (!"runIndex,startVertexId,runSeed".equals(header)) {
                throw new IOException("Unexpected run-config header in " + path + ": " + header);
            }
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                String[] parts = line.split(",");
                if (parts.length != 3) {
                    throw new IOException("Invalid run-config line in " + path + ": " + line);
                }
                configs.add(new FairRunConfig(
                        Integer.parseInt(parts[0]),
                        Integer.parseInt(parts[1]),
                        Long.parseLong(parts[2])
                ));
            }
        }
        return configs;
    }
}
