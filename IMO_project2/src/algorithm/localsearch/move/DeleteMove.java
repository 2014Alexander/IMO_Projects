package algorithm.localsearch.move;

import algorithm.localsearch.Cycle;

public final class DeleteMove implements Move {
    private final int i;

    public DeleteMove(int i) {
        this.i = i;
    }

    @Override
    public int delta(Cycle cycle, int[][] distanceMatrix, int[] profit) {
        int prev = cycle.prevIndex(i);
        int next = cycle.nextIndex(i);

        int a = cycle.cycle[prev];
        int v = cycle.cycle[i];
        int b = cycle.cycle[next];

        return -profit[v]
                - distanceMatrix[a][b]
                + distanceMatrix[a][v]
                + distanceMatrix[v][b];
    }

    @Override
    public void apply(Cycle cycle) {
        cycle.removeAt(i);
    }
}
