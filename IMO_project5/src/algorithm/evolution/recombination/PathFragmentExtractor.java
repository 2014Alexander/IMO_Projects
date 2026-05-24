package algorithm.evolution.recombination;

import algorithm.localsearch.Cycle;

import java.util.ArrayList;
import java.util.List;

/**
 * Wydziela fragmenty z zachowanych krawedzi.
 */
public final class PathFragmentExtractor {

    /**
     * Wydziela fragmenty z cyklu na podstawie tablicy zachowanych krawedzi.
     */
    public PathFragment[] extractFromRetainedEdges(Cycle cycle, boolean[] retainedEdges) {
        return extractFromRetainedEdges(cycle.cycle, cycle.size(), retainedEdges);
    }

    /**
     * Wydziela fragmenty z sekwencji wierzcholkow na podstawie zachowanych polaczen.
     */
    public PathFragment[] extractFromRetainedEdges(
        int[] vertices,
        int size,
        boolean[] retainedEdges
    ) {
        if (size < 2) {
            return new PathFragment[0];
        }

        int firstRemovedEdge = firstRemovedEdge(retainedEdges, size);
        if (firstRemovedEdge == -1) {
            int[] fragmentVertices = new int[size];
            System.arraycopy(vertices, 0, fragmentVertices, 0, size);
            return new PathFragment[] { new PathFragment(fragmentVertices) };
        }

        int[] orderedVertices = linearizeAfterRemovedEdge(vertices, size, firstRemovedEdge);
        boolean[] retainedBetween = retainedBetweenLinearVertices(retainedEdges, size, firstRemovedEdge);
        List<PathFragment> fragments = new ArrayList<>();

        int position = 0;
        while (position < size - 1) {
            if (!retainedBetween[position]) {
                position++;
                continue;
            }

            int start = position;
            position++;
            while (position < size - 1 && retainedBetween[position]) {
                position++;
            }

            int fragmentSize = position - start + 1;
            int[] fragmentVertices = new int[fragmentSize];
            System.arraycopy(orderedVertices, start, fragmentVertices, 0, fragmentSize);
            fragments.add(new PathFragment(fragmentVertices));
        }

        return fragments.toArray(new PathFragment[0]);
    }

    private static int firstRemovedEdge(boolean[] retainedEdges, int size) {
        for (int edge = 0; edge < size; edge++) {
            if (!retainedEdges[edge]) {
                return edge;
            }
        }

        return -1;
    }

    private static int[] linearizeAfterRemovedEdge(int[] vertices, int size, int removedEdge) {
        int[] orderedVertices = new int[size];
        int start = removedEdge + 1 == size ? 0 : removedEdge + 1;

        for (int offset = 0; offset < size; offset++) {
            int sourceIndex = start + offset;
            if (sourceIndex >= size) {
                sourceIndex -= size;
            }
            orderedVertices[offset] = vertices[sourceIndex];
        }

        return orderedVertices;
    }

    private static boolean[] retainedBetweenLinearVertices(
        boolean[] retainedEdges,
        int size,
        int removedEdge
    ) {
        boolean[] retainedBetween = new boolean[size - 1];
        int firstEdge = removedEdge + 1 == size ? 0 : removedEdge + 1;

        for (int offset = 0; offset < size - 1; offset++) {
            int sourceEdge = firstEdge + offset;
            if (sourceEdge >= size) {
                sourceEdge -= size;
            }
            retainedBetween[offset] = retainedEdges[sourceEdge];
        }

        return retainedBetween;
    }
}
