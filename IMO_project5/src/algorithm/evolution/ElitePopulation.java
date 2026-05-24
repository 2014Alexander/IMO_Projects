package algorithm.evolution;

import algorithm.evolution.diversity.PopulationDiversityPolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Elitarna populacja stalego rozmiaru dla HAE.
 */
public final class ElitePopulation {
    private final int capacity;
    private final PopulationDiversityPolicy diversityPolicy;
    private final List<EvolutionIndividual> individuals;

    /**
     * Tworzy populacje elitarna o zadanej pojemnosci.
     */
    public ElitePopulation(
        int capacity,
        PopulationDiversityPolicy diversityPolicy
    ) {
        this.capacity = capacity;
        this.diversityPolicy = diversityPolicy;
        this.individuals = new ArrayList<>(capacity);
    }

    /**
     * Probuje dodac osobnika do populacji poczatkowej.
     */
    public boolean addInitial(EvolutionIndividual candidate) {
        if (individuals.size() == capacity) {
            return false;
        }
        if (!diversityPolicy.isDifferent(candidate, individuals)) {
            return false;
        }

        individuals.add(candidate);
        return true;
    }

    /**
     * Losuje dwoch roznych rodzicow z rozkladem rownomiernym.
     */
    public EvolutionIndividual[] selectTwoParents(Random random) {
        int firstIndex = random.nextInt(individuals.size());
        int secondIndex = random.nextInt(individuals.size() - 1);
        if (secondIndex >= firstIndex) {
            secondIndex++;
        }

        return new EvolutionIndividual[] {
            individuals.get(firstIndex),
            individuals.get(secondIndex)
        };
    }

    /**
     * Sprawdza, czy kandydat o danym objective ma szanse wejsc do populacji.
     */
    public boolean canBeatWorst(int objective) {
        return objective > worst().objective();
    }

    /**
     * Probuje zastapic najgorszego osobnika lepszym i dostatecznie roznym potomkiem.
     */
    public boolean tryReplaceWorst(EvolutionIndividual candidate) {
        if (!canBeatWorst(candidate.objective())) {
            return false;
        }
        if (!diversityPolicy.isDifferent(candidate, individuals)) {
            return false;
        }

        individuals.set(worstIndex(), candidate);
        return true;
    }

    /**
     * Zwraca najlepszego osobnika populacji.
     */
    public EvolutionIndividual best() {
        int bestIndex = 0;
        int bestObjective = individuals.get(0).objective();

        for (int index = 1; index < individuals.size(); index++) {
            int objective = individuals.get(index).objective();
            if (objective > bestObjective) {
                bestIndex = index;
                bestObjective = objective;
            }
        }

        return individuals.get(bestIndex);
    }

    /**
     * Zwraca najgorszego osobnika populacji.
     */
    public EvolutionIndividual worst() {
        return individuals.get(worstIndex());
    }

    /**
     * Zwraca aktualny rozmiar populacji.
     */
    public int size() {
        return individuals.size();
    }

    private int worstIndex() {
        int worstIndex = 0;
        int worstObjective = individuals.get(0).objective();

        for (int index = 1; index < individuals.size(); index++) {
            int objective = individuals.get(index).objective();
            if (objective < worstObjective) {
                worstIndex = index;
                worstObjective = objective;
            }
        }

        return worstIndex;
    }
}
