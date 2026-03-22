package model;

public class DistanceMatrix {
    public final int size;
    public final int[][] distances;

    public DistanceMatrix(int size) {
        this.size = size;
        this.distances = new int[size][size];
    }

    public static DistanceMatrix fromVertices(Vertex[] vertices) {
        int n = vertices.length;
        DistanceMatrix matrix = new DistanceMatrix(n);

        for (int i = 0; i < n; i++) {
            matrix.distances[i][i] = 0;

            for (int j = i + 1; j < n; j++) {
                int distance = euclideanDistanceRounded(vertices[i], vertices[j]);
                matrix.distances[i][j] = distance;
                matrix.distances[j][i] = distance;
            }
        }

        return matrix;
    }

    private static int euclideanDistanceRounded(Vertex a, Vertex b) {
        int dx = a.x - b.x;
        int dy = a.y - b.y;
        return (int) Math.round(Math.sqrt((long) dx * dx + (long) dy * dy));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("DistanceMatrix{size=").append(size).append("}\n");

        int rowsToShow = Math.min(size, 5);
        int colsToShow = Math.min(size, 5);

        for (int i = 0; i < rowsToShow; i++) {
            for (int j = 0; j < colsToShow; j++) {
                sb.append(distances[i][j]);
                if (j < colsToShow - 1) {
                    sb.append(' ');
                }
            }

            if (colsToShow < size) {
                sb.append(" ...");
            }
            sb.append('\n');
        }

        if (rowsToShow < size) {
            sb.append("...\n");
        }

        return sb.toString();
    }
}