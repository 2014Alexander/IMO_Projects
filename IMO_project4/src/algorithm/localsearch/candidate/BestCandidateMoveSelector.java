package algorithm.localsearch.candidate;

import algorithm.localsearch.Cycle;
import algorithm.localsearch.move.DeleteMove;
import algorithm.localsearch.move.InsertMove;
import algorithm.localsearch.move.Move;
import algorithm.localsearch.move.SwapEdgesMove;

final class BestCandidateMoveSelector {
    private final Cycle cycle;
    private final int[][] distanceMatrix;
    private final int[] profit;

    private Move bestMove;
    private int bestDelta;

    BestCandidateMoveSelector(Cycle cycle, int[][] distanceMatrix, int[] profit) {
        this.cycle = cycle;
        this.distanceMatrix = distanceMatrix;
        this.profit = profit;
        this.bestMove = null;
        this.bestDelta = 0;
    }

    Move bestMove() {
        return bestMove;
    }

    void considerInsert(int insertedVertex, int insertPosition) {
        int firstVertex = cycle.cycle[insertPosition];
        int secondVertex = cycle.cycle[cycle.nextIndex(insertPosition)];

        int delta = profit[insertedVertex]
            - distanceMatrix[firstVertex][insertedVertex]
            - distanceMatrix[insertedVertex][secondVertex]
            + distanceMatrix[firstVertex][secondVertex];

        if (delta > bestDelta) {
            bestDelta = delta;
            bestMove = new InsertMove(insertedVertex, insertPosition);
        }
    }

    void considerDelete(int position) {
        int previousPosition = cycle.prevIndex(position);
        int nextPosition = cycle.nextIndex(position);

        int previousVertex = cycle.cycle[previousPosition];
        int deletedVertex = cycle.cycle[position];
        int nextVertex = cycle.cycle[nextPosition];

        int delta = -profit[deletedVertex]
            - distanceMatrix[previousVertex][nextVertex]
            + distanceMatrix[previousVertex][deletedVertex]
            + distanceMatrix[deletedVertex][nextVertex];

        if (delta > bestDelta) {
            bestDelta = delta;
            bestMove = new DeleteMove(position);
        }
    }

    void considerSwapEdges(int firstPosition, int secondPosition) {
        int nextFirstPosition = cycle.nextIndex(firstPosition);
        int nextSecondPosition = cycle.nextIndex(secondPosition);

        int firstStart = cycle.cycle[firstPosition];
        int firstEnd = cycle.cycle[nextFirstPosition];
        int secondStart = cycle.cycle[secondPosition];
        int secondEnd = cycle.cycle[nextSecondPosition];

        int delta = distanceMatrix[firstStart][firstEnd]
            + distanceMatrix[secondStart][secondEnd]
            - distanceMatrix[firstStart][secondStart]
            - distanceMatrix[firstEnd][secondEnd];

        if (delta > bestDelta) {
            bestDelta = delta;
            bestMove = new SwapEdgesMove(firstPosition, secondPosition);
        }
    }
}
