package algorithm.localsearch.move;

import algorithm.localsearch.Cycle;

public final class SwapVerticesMove implements Move {
    private final int i;
    private final int j;

    public SwapVerticesMove(int i, int j) {
        this.i = i;
        this.j = j;
    }

    @Override
    public int delta(Cycle cycle, int[][] distanceMatrix, int[] profit) {
        return cycle.areAdjacentPositions(i, j)
                ? deltaAdjacent(cycle, distanceMatrix)
                : deltaNonAdjacent(cycle, distanceMatrix);
    }

    @Override
    public void apply(Cycle cycle) {
        cycle.swapPositions(i, j);
    }

    private int deltaAdjacent(Cycle cycle, int[][] distanceMatrix) {
        boolean iBeforeJ = cycle.nextIndex(i) == j;
        int left = iBeforeJ ? i : j;
        int right = iBeforeJ ? j : i;

        int prevLeft = cycle.prevIndex(left);
        int nextRight = cycle.nextIndex(right);

        int[] order = cycle.cycle;

        int a = order[prevLeft];
        int vLeft = order[left];
        int vRight = order[right];
        int b = order[nextRight];

        int removed = distanceMatrix[a][vLeft]
                + distanceMatrix[vRight][b];

        int added = distanceMatrix[a][vRight]
                + distanceMatrix[vLeft][b];

        return removed - added;
    }

    private int deltaNonAdjacent(Cycle cycle, int[][] distanceMatrix) {
        int prevI = cycle.prevIndex(i);
        int nextI = cycle.nextIndex(i);
        int prevJ = cycle.prevIndex(j);
        int nextJ = cycle.nextIndex(j);

        int[] order = cycle.cycle;

        int a = order[prevI];
        int vi = order[i];
        int b = order[nextI];

        int c = order[prevJ];
        int vj = order[j];
        int d = order[nextJ];

        int removed = distanceMatrix[a][vi]
                + distanceMatrix[vi][b]
                + distanceMatrix[c][vj]
                + distanceMatrix[vj][d];

        int added = distanceMatrix[a][vj]
                + distanceMatrix[vj][b]
                + distanceMatrix[c][vi]
                + distanceMatrix[vi][d];

        return removed - added;
    }
}
