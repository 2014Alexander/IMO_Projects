package algorithm.localsearch.candidate;

import model.Instance;

/**
 * Przechowuje krawędzie kandydackie dla jednej instancji problemu.
 *
 * <p>Krawędzie kandydackie są wyznaczane na podstawie list najbliższych
 * wierzchołków. Klasa nie zależy od bieżącego cyklu i nie generuje ruchów.</p>
 */
public final class CandidateEdges {
    public static final int DEFAULT_CANDIDATE_COUNT = 10;

    private final int vertexCount;
    private final int candidateCount;
    private final int[][] nearestVerticesByVertex;
    private final int[][] incidentVerticesByVertex;
    private final boolean[][] candidateEdge;

    /**
     * Tworzy strukturę krawędzi kandydackich z domyślną liczbą najbliższych
     * wierzchołków.
     *
     * @param instance instancja problemu
     */
    public CandidateEdges(Instance instance) {
        this(instance, DEFAULT_CANDIDATE_COUNT);
    }

    /**
     * Tworzy strukturę krawędzi kandydackich z podaną liczbą najbliższych
     * wierzchołków.
     *
     * @param instance       instancja problemu
     * @param candidateCount liczba najbliższych wierzchołków dla jednego wierzchołka
     */
    public CandidateEdges(Instance instance, int candidateCount) {
        this.vertexCount = instance.size;
        this.candidateCount = Math.min(candidateCount, vertexCount - 1);
        this.nearestVerticesByVertex = buildNearestVertices(instance);
        this.candidateEdge = buildCandidateEdgeMatrix();
        this.incidentVerticesByVertex = buildIncidentVertices();
    }

    /**
     * Zwraca liczbę najbliższych wierzchołków używaną przy budowie list
     * kandydackich.
     *
     * @return liczba najbliższych wierzchołków
     */
    public int candidateCount() {
        return candidateCount;
    }

    /**
     * Zwraca listę najbliższych wierzchołków dla podanego wierzchołka.
     *
     * @param vertexId identyfikator wierzchołka
     * @return najbliższe wierzchołki
     */
    public int[] nearestVertices(int vertexId) {
        return nearestVerticesByVertex[vertexId];
    }

    /**
     * Zwraca wierzchołki połączone z podanym wierzchołkiem krawędzią
     * kandydacką.
     *
     * @param vertexId identyfikator wierzchołka
     * @return wierzchołki incydentne z krawędziami kandydackimi
     */
    public int[] incidentVertices(int vertexId) {
        return incidentVerticesByVertex[vertexId];
    }

    /**
     * Sprawdza, czy krawędź między dwoma wierzchołkami jest kandydacka.
     *
     * @param firstVertex  pierwszy wierzchołek
     * @param secondVertex drugi wierzchołek
     * @return true, jeżeli krawędź jest kandydacka
     */
    public boolean isCandidateEdge(int firstVertex, int secondVertex) {
        return candidateEdge[firstVertex][secondVertex];
    }

    /**
     * Buduje listy najbliższych wierzchołków dla wszystkich wierzchołków.
     *
     * @param instance instancja problemu
     * @return tablica list najbliższych wierzchołków
     */
    private int[][] buildNearestVertices(Instance instance) {
        int[][] nearestVertices = new int[vertexCount][];

        for (int vertexId = 0; vertexId < vertexCount; vertexId++) {
            nearestVertices[vertexId] = nearestVerticesFor(instance, vertexId);
        }

        return nearestVertices;
    }

    /**
     * Buduje macierz krawędzi kandydackich na podstawie list najbliższych
     * wierzchołków.
     *
     * @return macierz krawędzi kandydackich
     */
    private boolean[][] buildCandidateEdgeMatrix() {
        boolean[][] edges = new boolean[vertexCount][vertexCount];

        for (int vertexId = 0; vertexId < vertexCount; vertexId++) {
            for (int nearestVertex : nearestVerticesByVertex[vertexId]) {
                // Krawędź kandydacka jest traktowana jako nieskierowana.
                // Wystarczy, że jeden z końców ma drugi koniec na swojej liście najbliższych.
                edges[vertexId][nearestVertex] = true;
                edges[nearestVertex][vertexId] = true;
            }
        }

        return edges;
    }

    /**
     * Buduje listy wierzchołków incydentnych z krawędziami kandydackimi.
     *
     * @return tablica list wierzchołków incydentnych
     */
    private int[][] buildIncidentVertices() {
        int[][] incidentVertices = new int[vertexCount][];

        for (int vertexId = 0; vertexId < vertexCount; vertexId++) {
            int incidentCount = 0;

            for (int otherVertex = 0; otherVertex < vertexCount; otherVertex++) {
                if (candidateEdge[vertexId][otherVertex]) {
                    incidentCount++;
                }
            }

            int[] incidentForVertex = new int[incidentCount];
            int index = 0;

            for (int otherVertex = 0; otherVertex < vertexCount; otherVertex++) {
                if (candidateEdge[vertexId][otherVertex]) {
                    // Lista incydentna może być dłuższa niż lista najbliższych,
                    // bo krawędź jest kandydacka także wtedy, gdy vertexId
                    // wystąpił na liście najbliższych drugiego wierzchołka.
                    incidentForVertex[index] = otherVertex;
                    index++;
                }
            }

            incidentVertices[vertexId] = incidentForVertex;
        }

        return incidentVertices;
    }

    /**
     * Buduje listę najbliższych wierzchołków dla jednego wierzchołka.
     *
     * @param instance instancja problemu
     * @param vertexId identyfikator wierzchołka
     * @return najbliższe wierzchołki
     */
    private int[] nearestVerticesFor(Instance instance, int vertexId) {
        int[] nearest = new int[candidateCount];
        int nearestSize = 0;

        for (int otherVertex = 0; otherVertex < vertexCount; otherVertex++) {
            if (otherVertex == vertexId) {
                continue;
            }

            int insertIndex = insertionIndex(instance, vertexId, otherVertex, nearest, nearestSize);

            if (insertIndex < candidateCount) {
                int lastIndex = Math.min(nearestSize, candidateCount - 1);

                // Utrzymujemy krótki posortowany prefiks zamiast sortować wszystkie wierzchołki.
                // Elementy od pozycji wstawienia są przesuwane o jedno miejsce w prawo.
                for (int index = lastIndex; index > insertIndex; index--) {
                    nearest[index] = nearest[index - 1];
                }

                nearest[insertIndex] = otherVertex;

                if (nearestSize < candidateCount) {
                    nearestSize++;
                }
            }
        }

        return nearest;
    }

    /**
     * Wyznacza pozycję wstawienia w posortowanej liście najbliższych wierzchołków.
     *
     * @param instance    instancja problemu
     * @param vertexId    wierzchołek, dla którego budowana jest lista
     * @param otherVertex sprawdzany wierzchołek
     * @param nearest     aktualna lista najbliższych wierzchołków
     * @param nearestSize aktualna liczba elementów listy
     * @return pozycja wstawienia
     */
    private int insertionIndex(
        Instance instance,
        int vertexId,
        int otherVertex,
        int[] nearest,
        int nearestSize
    ) {
        int distance = instance.distanceMatrix.distances[vertexId][otherVertex];

        for (int index = nearestSize - 1; index >= 0; index--) {
            int currentVertex = nearest[index];
            int currentDistance = instance.distanceMatrix.distances[vertexId][currentVertex];

            if (distance > currentDistance) {
                return index + 1;
            }

            // Przy równych odległościach niższy identyfikator daje deterministyczny porządek.
            if (distance == currentDistance && otherVertex > currentVertex) {
                return index + 1;
            }
        }

        return 0;
    }
}
