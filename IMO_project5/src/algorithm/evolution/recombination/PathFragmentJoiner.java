package algorithm.evolution.recombination;

import algorithm.localsearch.Cycle;
import model.Instance;

import java.util.Random;

/**
 * Losowo sklada fragmenty w jeden czesciowy cykl.
 */
public final class PathFragmentJoiner {

    /**
     * Laczy fragmenty w losowej kolejnosc i opcjonalnie odwraca ich kierunek.
     */
    public Cycle join(
        Instance instance,
        PathFragment[] fragments,
        Random random,
        boolean allowReverse
    ) {
        Cycle cycle = new Cycle(instance.size);
        int[] order = shuffledOrder(fragments.length, random);

        for (int index : order) {
            boolean reversed = allowReverse && random.nextBoolean();
            fragments[index].appendTo(cycle, reversed);
        }

        return cycle;
    }

    private static int[] shuffledOrder(int size, Random random) {
        int[] order = new int[size];
        for (int index = 0; index < size; index++) {
            order[index] = index;
        }

        for (int index = size - 1; index > 0; index--) {
            int selected = random.nextInt(index + 1);
            int temporary = order[index];
            order[index] = order[selected];
            order[selected] = temporary;
        }

        return order;
    }
}
