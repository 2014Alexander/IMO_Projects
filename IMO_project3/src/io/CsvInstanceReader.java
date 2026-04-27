package io;

import model.Instance;
import model.Vertex;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CsvInstanceReader {

    public Instance read(String filePath) {
        List<Vertex> vertices = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            int id = 0;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty()) {
                    continue;
                }

                vertices.add(parseVertex(line, id));
                id++;
            }
        } catch (IOException e) {
            throw new RuntimeException("Nie udało się odczytać pliku: " + filePath, e);
        }

        if (vertices.isEmpty()) {
            throw new RuntimeException("Plik jest pusty: " + filePath);
        }

        String instanceName = extractInstanceName(filePath);
        Vertex[] vertexArray = vertices.toArray(new Vertex[0]);

        return new Instance(instanceName, vertexArray);
    }

    private Vertex parseVertex(String line, int id) {
        String[] parts = line.split(";");

        if (parts.length != 3) {
            throw new RuntimeException(
                    "Niepoprawny wiersz w pliku wejściowym: \"" + line + "\". Oczekiwano formatu x;y;profit"
            );
        }

        int x = Integer.parseInt(parts[0].trim());
        int y = Integer.parseInt(parts[1].trim());
        int profit = Integer.parseInt(parts[2].trim());

        return new Vertex(id, x, y, profit);
    }

    private String extractInstanceName(String filePath) {
        String normalizedPath = filePath.replace("\\", "/");
        int slashIndex = normalizedPath.lastIndexOf('/');

        String fileName;
        if (slashIndex == -1) {
            fileName = normalizedPath;
        } else {
            fileName = normalizedPath.substring(slashIndex + 1);
        }

        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1) {
            return fileName;
        }

        return fileName.substring(0, dotIndex);
    }
}