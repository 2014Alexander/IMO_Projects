package algorithm.similarity;

import algorithm.localsearch.Cycle;
import model.Solution;

import java.util.Arrays;
import java.util.List;

/**
 * Kanoniczna sygnatura cyklu odporna na punkt startu i kierunek przejscia.
 */
public final class CanonicalCycleSignature {
    private final int[] vertices;

    private CanonicalCycleSignature(int[] vertices) {
        this.vertices = vertices;
    }

    /**
     * Buduje sygnature z `Solution`.
     */
    public static CanonicalCycleSignature from(Solution solution) {
        List<Integer> cycle = solution.cycle();
        int[] vertices = new int[cycle.size()];

        for (int index = 0; index < vertices.length; index++) {
            vertices[index] = cycle.get(index);
        }

        return from(vertices, vertices.length);
    }

    /**
     * Buduje sygnature z `Cycle`.
     */
    public static CanonicalCycleSignature from(Cycle cycle) {
        return from(cycle.cycle, cycle.size());
    }

    private static CanonicalCycleSignature from(int[] cycle, int cycleSize) {
        int[] canonical = new int[cycleSize];
        if (cycleSize == 0) {
            return new CanonicalCycleSignature(canonical);
        }

        int bestStart = 0;
        boolean bestReversed = false;

        for (int start = 0; start < cycleSize; start++) {
            if (compareRotation(cycle, cycleSize, start, false, bestStart, bestReversed) < 0) {
                bestStart = start;
                bestReversed = false;
            }
            if (compareRotation(cycle, cycleSize, start, true, bestStart, bestReversed) < 0) {
                bestStart = start;
                bestReversed = true;
            }
        }

        for (int offset = 0; offset < cycleSize; offset++) {
            canonical[offset] = valueAt(cycle, cycleSize, bestStart, bestReversed, offset);
        }

        return new CanonicalCycleSignature(canonical);
    }

    private static int compareRotation(
        int[] cycle,
        int cycleSize,
        int candidateStart,
        boolean candidateReversed,
        int bestStart,
        boolean bestReversed
    ) {
        for (int offset = 0; offset < cycleSize; offset++) {
            int candidate = valueAt(cycle, cycleSize, candidateStart, candidateReversed, offset);
            int best = valueAt(cycle, cycleSize, bestStart, bestReversed, offset);

            if (candidate != best) {
                return Integer.compare(candidate, best);
            }
        }

        return 0;
    }

    private static int valueAt(
        int[] cycle,
        int cycleSize,
        int start,
        boolean reversed,
        int offset
    ) {
        int index = reversed
            ? start - offset
            : start + offset;

        index %= cycleSize;
        if (index < 0) {
            index += cycleSize;
        }

        return cycle[index];
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof CanonicalCycleSignature other)) {
            return false;
        }
        return Arrays.equals(vertices, other.vertices);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(vertices);
    }
}
