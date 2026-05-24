package algorithm.similarity;

import algorithm.localsearch.Cycle;
import model.Instance;
import model.Solution;

import java.util.Arrays;
import java.util.List;

/**
 * Buduje strukturalne cechy rozwiazania z reprezentacji uzywanych w projekcie.
 */
public final class SolutionFeaturesBuilder {

    /**
     * Buduje cechy rozwiazania zapisanego jako `Solution`.
     */
    public SolutionFeatures build(Instance instance, Solution solution) {
        return build(instance, solution.cycle());
    }

    /**
     * Buduje cechy rozwiazania zapisanego jako `Cycle`.
     */
    public SolutionFeatures build(Instance instance, Cycle cycle) {
        boolean[] selectedVertices = new boolean[instance.size];
        int selectedCount = 0;

        for (int position = 0; position < cycle.size(); position++) {
            int vertex = cycle.cycle[position];
            if (!selectedVertices[vertex]) {
                selectedVertices[vertex] = true;
                selectedCount++;
            }
        }

        long[] edges = buildEdges(cycle.cycle, cycle.size());
        return new SolutionFeatures(instance.size, selectedVertices, selectedCount, edges);
    }

    private SolutionFeatures build(Instance instance, List<Integer> cycle) {
        boolean[] selectedVertices = new boolean[instance.size];
        int selectedCount = 0;

        for (int position = 0; position < cycle.size(); position++) {
            int vertex = cycle.get(position);
            if (!selectedVertices[vertex]) {
                selectedVertices[vertex] = true;
                selectedCount++;
            }
        }

        long[] edges = buildEdges(cycle);
        return new SolutionFeatures(instance.size, selectedVertices, selectedCount, edges);
    }

    private static long[] buildEdges(List<Integer> cycle) {
        int cycleSize = cycle.size();
        long[] edges = new long[cycleSize];

        for (int position = 0; position < cycleSize; position++) {
            int from = cycle.get(position);
            int to = cycle.get(position + 1 == cycleSize ? 0 : position + 1);
            edges[position] = SolutionFeatures.edgeKey(from, to);
        }

        return sortAndUnique(edges);
    }

    private static long[] buildEdges(int[] cycle, int cycleSize) {
        long[] edges = new long[cycleSize];

        for (int position = 0; position < cycleSize; position++) {
            int from = cycle[position];
            int to = cycle[position + 1 == cycleSize ? 0 : position + 1];
            edges[position] = SolutionFeatures.edgeKey(from, to);
        }

        return sortAndUnique(edges);
    }

    private static long[] sortAndUnique(long[] edges) {
        Arrays.sort(edges);

        int uniqueSize = 0;
        for (long edge : edges) {
            if (uniqueSize == 0 || edges[uniqueSize - 1] != edge) {
                edges[uniqueSize] = edge;
                uniqueSize++;
            }
        }

        return Arrays.copyOf(edges, uniqueSize);
    }
}
