package algorithm.localsearch.move;

import algorithm.localsearch.Cycle;

public interface Move {
    int delta(Cycle cycle, int[][] distanceMatrix, int[] profit);

    void apply(Cycle cycle);
}
