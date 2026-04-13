package algorithm.localsearch.move;

import algorithm.localsearch.Cycle;

public final class SwapEdgesMove implements Move {
    private final int i;
    private final int j;

    public SwapEdgesMove(int i, int j) {
        this.i = i;
        this.j = j;
    }

    @Override
    public int delta(Cycle cycle, int[][] distanceMatrix, int[] profit) {
        int nextI = cycle.nextIndex(i);
        int nextJ = cycle.nextIndex(j);

        int a = cycle.cycle[i];
        int b = cycle.cycle[nextI];
        int c = cycle.cycle[j];
        int d = cycle.cycle[nextJ];

        int removed = distanceMatrix[a][b]
                + distanceMatrix[c][d];

        int added = distanceMatrix[a][c]
                + distanceMatrix[b][d];

        return removed - added;
    }

    @Override
    public void apply(Cycle cycle) {
        int left = cycle.nextIndex(i);
        int right = j;

        cycle.reverseFragment(left, right);
    }
}
