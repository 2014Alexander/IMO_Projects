package algorithm.similarity;

import java.util.Arrays;

/**
 * Strukturalne cechy rozwiazania uzywane do porownywania cykli.
 */
public final class SolutionFeatures {
    private final int instanceSize;
    private final boolean[] selectedVertices;
    private final int selectedCount;
    private final long[] edges;

    /**
     * Tworzy zestaw strukturalnych cech rozwiazania.
     */
    public SolutionFeatures(
        int instanceSize,
        boolean[] selectedVertices,
        int selectedCount,
        long[] edges
    ) {
        this.instanceSize = instanceSize;
        this.selectedVertices = selectedVertices;
        this.selectedCount = selectedCount;
        this.edges = edges;
    }

    /**
     * Zwraca liczbe wierzcholkow instancji, dla ktorej zbudowano cechy.
     */
    public int instanceSize() {
        return instanceSize;
    }

    /**
     * Zwraca tablice wybranych wierzcholkow indeksowana identyfikatorem wierzcholka.
     */
    public boolean[] selectedVertices() {
        return selectedVertices;
    }

    /**
     * Zwraca liczbe wybranych wierzcholkow.
     */
    public int selectedCount() {
        return selectedCount;
    }

    /**
     * Zwraca posortowane kanoniczne krawedzie nieskierowane.
     */
    public long[] edges() {
        return edges;
    }

    /**
     * Sprawdza, czy rozwiazanie zawiera podany wierzcholek.
     */
    public boolean hasVertex(int vertex) {
        return selectedVertices[vertex];
    }

    /**
     * Sprawdza, czy rozwiazanie zawiera podana krawedz nieskierowana.
     */
    public boolean hasEdge(int firstVertex, int secondVertex) {
        return Arrays.binarySearch(edges, edgeKey(firstVertex, secondVertex)) >= 0;
    }

    /**
     * Koduje krawedz nieskierowana do postaci kanonicznej.
     */
    public static long edgeKey(int firstVertex, int secondVertex) {
        int from = Math.min(firstVertex, secondVertex);
        int to = Math.max(firstVertex, secondVertex);
        return ((long) from << 32) | (to & 0xffffffffL);
    }
}
