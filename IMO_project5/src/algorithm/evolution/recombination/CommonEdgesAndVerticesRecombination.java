package algorithm.evolution.recombination;

import algorithm.evolution.EvolutionIndividual;
import algorithm.localsearch.Cycle;
import algorithm.similarity.SolutionFeatures;
import model.Instance;

import java.util.Random;

/**
 * Operator 2: filtruje rodzica bazowego po wspolnych wierzcholkach i krawedziach.
 */
public final class CommonEdgesAndVerticesRecombination implements RecombinationOperator {
    private final PathFragmentExtractor extractor = new PathFragmentExtractor();
    private final PathFragmentJoiner joiner = new PathFragmentJoiner();

    @Override
    public Cycle recombine(
        Instance instance,
        EvolutionIndividual firstParent,
        EvolutionIndividual secondParent,
        Random random
    ) {
        EvolutionIndividual baseParent = random.nextBoolean() ? firstParent : secondParent;
        EvolutionIndividual filterParent = baseParent == firstParent ? secondParent : firstParent;
        Cycle baseCycle = baseParent.cycle();

        SolutionFeatures filterFeatures = filterParent.features();
        int[] filteredVertices = new int[baseCycle.size()];
        int filteredSize = filterByVertices(baseCycle, filterFeatures, filteredVertices);

        if (filteredSize < 2) {
            Cycle child = new Cycle(instance.size);
            appendFilteredVertices(child, filteredVertices, filteredSize);
            return child;
        }

        boolean[] retainedEdges = retainedEdges(filteredVertices, filteredSize, filterFeatures);
        PathFragment[] fragments = extractor.extractFromRetainedEdges(
            filteredVertices,
            filteredSize,
            retainedEdges
        );

        return joiner.join(instance, fragments, random, true);
    }

    private static int filterByVertices(
        Cycle baseParent,
        SolutionFeatures filterFeatures,
        int[] filteredVertices
    ) {
        int filteredSize = 0;

        for (int position = 0; position < baseParent.size(); position++) {
            int vertex = baseParent.cycle[position];
            if (filterFeatures.hasVertex(vertex)) {
                filteredVertices[filteredSize] = vertex;
                filteredSize++;
            }
        }

        return filteredSize;
    }

    private static boolean[] retainedEdges(
        int[] vertices,
        int size,
        SolutionFeatures filterFeatures
    ) {
        boolean[] retainedEdges = new boolean[size];

        for (int position = 0; position < size; position++) {
            int from = vertices[position];
            int to = vertices[position + 1 == size ? 0 : position + 1];
            retainedEdges[position] = filterFeatures.hasEdge(from, to);
        }

        return retainedEdges;
    }

    private static void appendFilteredVertices(Cycle child, int[] filteredVertices, int filteredSize) {
        for (int position = 0; position < filteredSize; position++) {
            child.append(filteredVertices[position]);
        }
    }
}
