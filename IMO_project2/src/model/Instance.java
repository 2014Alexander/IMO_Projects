package model;

public class Instance {
    public final String name;
    public final Vertex[] vertices;
    public final DistanceMatrix distanceMatrix;
    public final int size;

    public Instance(String name, Vertex[] vertices) {
        if (name == null) {
            throw new IllegalArgumentException("Nazwa instancji nie może być null.");
        }
        if (vertices == null) {
            throw new IllegalArgumentException("Tablica wierzchołków nie może być null.");
        }

        this.name = name;
        this.vertices = vertices;
        this.distanceMatrix = DistanceMatrix.fromVertices(vertices);
        this.size = vertices.length;
    }

    @Override
    public String toString() {
        return "Instance{" +
                "name='" + name + '\'' +
                ", size=" + size +
                '}';
    }
}