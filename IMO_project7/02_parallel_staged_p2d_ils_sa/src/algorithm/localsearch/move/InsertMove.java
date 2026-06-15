package algorithm.localsearch.move;

import algorithm.localsearch.Cycle;

public final class InsertMove implements Move {
    private final int v;
    private final int i;

    public InsertMove(int v, int i) {
        this.v = v;
        this.i = i;
    }

    @Override
    public int delta(Cycle cycle, int[][] distanceMatrix, int[] profit) {
        int a = cycle.cycle[i];
        int b = cycle.cycle[cycle.nextIndex(i)];

        return profit[v]
                - distanceMatrix[a][v]
                - distanceMatrix[v][b]
                + distanceMatrix[a][b];
    }

    @Override
    public void apply(Cycle cycle) {
        cycle.insertAfter(i, v);
    }
}
