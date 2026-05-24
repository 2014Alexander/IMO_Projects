package algorithm.evolution.recombination;

import algorithm.evolution.EvolutionIndividual;
import algorithm.localsearch.Cycle;
import algorithm.similarity.SolutionFeatures;
import model.Instance;

import java.util.Arrays;
import java.util.Random;

/**
 * Operator 1: zachowuje wspolne czesci rodzicow.
 */
public final class CommonPartsRecombination implements RecombinationOperator {
    private final PathFragmentExtractor extractor = new PathFragmentExtractor();
    private final PathFragmentJoiner joiner = new PathFragmentJoiner();

    @Override
    public Cycle recombine(
        Instance instance,
        EvolutionIndividual firstParent,
        EvolutionIndividual secondParent,
        Random random
    ) {
        Cycle firstCycle = firstParent.cycle();
        SolutionFeatures firstFeatures = firstParent.features();
        SolutionFeatures secondFeatures = secondParent.features();

        boolean[] retainedEdges = retainedEdges(firstCycle, secondFeatures);
        PathFragment[] edgeFragments = extractor.extractFromRetainedEdges(firstCycle, retainedEdges);
        PathFragment[] fragments = appendIsolatedCommonVertices(
            instance,
            edgeFragments,
            firstFeatures,
            secondFeatures
        );

        return joiner.join(instance, fragments, random, true);
    }

    private static boolean[] retainedEdges(Cycle parent, SolutionFeatures otherFeatures) {
        int size = parent.size();
        boolean[] retainedEdges = new boolean[size];

        for (int position = 0; position < size; position++) {
            int from = parent.cycle[position];
            int to = parent.cycle[position + 1 == size ? 0 : position + 1];
            retainedEdges[position] = otherFeatures.hasEdge(from, to);
        }

        return retainedEdges;
    }

    private static PathFragment[] appendIsolatedCommonVertices(
        Instance instance,
        PathFragment[] edgeFragments,
        SolutionFeatures firstFeatures,
        SolutionFeatures secondFeatures
    ) {
        boolean[] usedInFragments = new boolean[instance.size];
        for (PathFragment fragment : edgeFragments) {
            fragment.markVertices(usedInFragments);
        }

        int isolatedCount = 0;
        for (int vertex = 0; vertex < instance.size; vertex++) {
            if (firstFeatures.hasVertex(vertex)
                    && secondFeatures.hasVertex(vertex)
                    && !usedInFragments[vertex]) {
                isolatedCount++;
            }
        }

        PathFragment[] fragments = Arrays.copyOf(edgeFragments, edgeFragments.length + isolatedCount);
        int nextIndex = edgeFragments.length;
        for (int vertex = 0; vertex < instance.size; vertex++) {
            if (firstFeatures.hasVertex(vertex)
                    && secondFeatures.hasVertex(vertex)
                    && !usedInFragments[vertex]) {
                fragments[nextIndex] = PathFragment.singleVertex(vertex);
                nextIndex++;
            }
        }

        return fragments;
    }
}
