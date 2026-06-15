package algorithm.metaheuristic.perturbation;

import algorithm.localsearch.Cycle;
import model.Instance;

import java.util.Random;

public final class RandomSwapEdgesPerturbation implements IlsPerturbation {
    public static final int DEFAULT_MOVES_COUNT = 30;

    private final int movesCount;

    /**
     * Tworzy perturbację z domyślną liczbą losowych ruchów SWAP_EDGES.
     */
    public RandomSwapEdgesPerturbation() {
        this(DEFAULT_MOVES_COUNT);
    }

    /**
     * Tworzy perturbację z jawnie podaną liczbą losowych ruchów SWAP_EDGES.
     *
     * @param movesCount liczba losowych odwróceń fragmentów cyklu
     */
    public RandomSwapEdgesPerturbation(int movesCount) {
        this.movesCount = movesCount;
    }

    @Override
    public void perturb(Instance instance, Cycle cycle, Random random) {
        for (int move = 0; move < movesCount; move++) {
            applyRandomSwapEdges(cycle, random);
        }
    }

    private static void applyRandomSwapEdges(Cycle cycle, Random random) {
        if (cycle.size() < 4) {
            return;
        }

        int firstPosition;
        int secondPosition;

        do {
            firstPosition = random.nextInt(cycle.size());
            secondPosition = random.nextInt(cycle.size());
        } while (areAdjacentEdges(cycle, firstPosition, secondPosition));

        int leftPosition = Math.min(firstPosition, secondPosition);
        int rightPosition = Math.max(firstPosition, secondPosition);

        cycle.reverseFragment(leftPosition + 1, rightPosition);
    }

    private static boolean areAdjacentEdges(Cycle cycle, int firstPosition, int secondPosition) {
        return firstPosition == secondPosition
            || cycle.areAdjacentPositions(firstPosition, secondPosition);
    }
}
