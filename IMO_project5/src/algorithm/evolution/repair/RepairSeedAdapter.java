package algorithm.evolution.repair;

import algorithm.evolution.EvolutionIndividual;
import algorithm.localsearch.Cycle;

/**
 * Tworzy minimalny seed dla repair poza semantyka samych operatorow rekombinacji.
 */
public final class RepairSeedAdapter {
    private static final int MINIMUM_REPAIR_SIZE = 2;

    /**
     * Uzupelnia partial cycle do minimalnego rozmiaru wymaganego przez repair.
     */
    public void adapt(Cycle cycle, EvolutionIndividual firstParent, EvolutionIndividual secondParent) {
        if (cycle.size() >= MINIMUM_REPAIR_SIZE) {
            return;
        }

        boolean[] used = new boolean[cycle.cycle.length];
        for (int position = 0; position < cycle.size(); position++) {
            used[cycle.cycle[position]] = true;
        }

        appendMissing(cycle, used, firstParent);
        if (cycle.size() < MINIMUM_REPAIR_SIZE) {
            appendMissing(cycle, used, secondParent);
        }
    }

    private static void appendMissing(Cycle target, boolean[] used, EvolutionIndividual source) {
        Cycle sourceCycle = source.cycle();

        for (int position = 0; position < sourceCycle.size() && target.size() < MINIMUM_REPAIR_SIZE; position++) {
            int vertex = sourceCycle.cycle[position];
            if (!used[vertex]) {
                target.append(vertex);
                used[vertex] = true;
            }
        }
    }
}
