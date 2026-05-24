package algorithm.evolution.recombination;

import algorithm.localsearch.Cycle;

/**
 * Fragment sciezki zachowany przez rekombinacje.
 */
public final class PathFragment {
    private final int[] vertices;

    /**
     * Tworzy fragment z podanej sekwencji wierzcholkow.
     */
    public PathFragment(int[] vertices) {
        this.vertices = vertices;
    }

    /**
     * Tworzy jednoelementowy fragment.
     */
    public static PathFragment singleVertex(int vertex) {
        return new PathFragment(new int[] { vertex });
    }

    /**
     * Zwraca liczbe wierzcholkow fragmentu.
     */
    public int size() {
        return vertices.length;
    }

    /**
     * Zwraca wierzcholek z podanej pozycji fragmentu.
     */
    public int vertexAt(int index) {
        return vertices[index];
    }

    /**
     * Dopisuje fragment do cyklu potomka.
     */
    public void appendTo(Cycle cycle, boolean reversed) {
        if (reversed) {
            for (int index = vertices.length - 1; index >= 0; index--) {
                cycle.append(vertices[index]);
            }
        } else {
            for (int vertex : vertices) {
                cycle.append(vertex);
            }
        }
    }

    /**
     * Zaznacza wierzcholki fragmentu w podanej tablicy.
     */
    public void markVertices(boolean[] used) {
        for (int vertex : vertices) {
            used[vertex] = true;
        }
    }
}
