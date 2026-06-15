package algorithm.localsearch;

import algorithm.localsearch.move.DeleteMove;
import algorithm.localsearch.move.InsertMove;
import algorithm.localsearch.move.Move;
import algorithm.localsearch.move.SwapEdgesMove;
import algorithm.localsearch.move.SwapVerticesMove;
import model.Vertex;

import java.util.ArrayList;
import java.util.List;

public final class Neighborhood {
    private final Cycle cycle;
    private final Vertex[] vertices;
    private int cycleSize;

    public Neighborhood(Cycle cycle, Vertex[] vertices) {
        this.cycle = cycle;
        this.vertices = vertices;
    }

    public List<Move> neighborhoodSwapVertices() {
        cycleSize = cycle.size();

        List<Move> moves = new ArrayList<>();

        generateInsertMoves(moves);
        generateDeleteMoves(moves);
        generateSwapVerticesMoves(moves);

        return moves;
    }

    public List<Move> neighborhoodSwapEdges() {
        cycleSize = cycle.size();

        List<Move> moves = new ArrayList<>();

        generateInsertMoves(moves);
        generateDeleteMoves(moves);
        generateSwapEdgesMoves(moves);

        return moves;
    }

    private void generateInsertMoves(List<Move> moves) {
        boolean[] selected = buildSelectedArray();

        for (Vertex vertex : vertices) {
            int v = vertex.id;

            if (!selected[v]) {
                for (int i = 0; i < cycleSize; i++) {
                    moves.add(new InsertMove(v, i));
                }
            }
        }
    }

    private void generateDeleteMoves(List<Move> moves) {
        if (cycleSize > 2) {
            for (int i = 0; i < cycleSize; i++) {
                moves.add(new DeleteMove(i));
            }
        }
    }

    private void generateSwapVerticesMoves(List<Move> moves) {
        if (cycleSize <= 2) {
            return;
        }

        for (int i = 0; i < cycleSize - 1; i++) {
            for (int j = i + 1; j < cycleSize; j++) {
                moves.add(new SwapVerticesMove(i, j));
            }
        }
    }

    private void generateSwapEdgesMoves(List<Move> moves) {
        for (int i = 0; i < cycleSize - 1; i++) {
            for (int j = i + 1; j < cycleSize; j++) {
                if (cycle.nextIndex(i) == j || cycle.nextIndex(j) == i) {
                    continue;
                }

                moves.add(new SwapEdgesMove(i, j));
            }
        }
    }

    private boolean[] buildSelectedArray() {
        boolean[] selected = new boolean[vertices.length];

        for (int i = 0; i < cycleSize; i++) {
            selected[cycle.cycle[i]] = true;
        }

        return selected;
    }
}
