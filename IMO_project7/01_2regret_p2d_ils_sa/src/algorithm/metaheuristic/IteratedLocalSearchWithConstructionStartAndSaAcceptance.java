package algorithm.metaheuristic;

import algorithm.CycleImprover;
import algorithm.IterationCountingAlgorithm;
import algorithm.OptimizationAlgorithm;
import algorithm.SolutionObjective;
import algorithm.localsearch.Cycle;
import algorithm.metaheuristic.perturbation.IlsPerturbation;
import model.Instance;
import model.Solution;

import java.util.Random;

/**
 * Wariant ILS z podanym algorytmem startowym i akceptacją SA.
 */
public final class IteratedLocalSearchWithConstructionStartAndSaAcceptance implements OptimizationAlgorithm, IterationCountingAlgorithm, SaAcceptanceStatistics {
    private final String name;
    private final OptimizationAlgorithm startAlgorithm;
    private final CycleImprover localSearch;
    private final IlsPerturbation perturbation;
    private final long timeLimitNanos;
    private final Random random;
    private final double initialTemperature;
    private final double finalTemperature;
    private final CoolingSchedule coolingSchedule;

    private int lastIterationCount;
    private int acceptedBetterCount;
    private int acceptedWorseCount;
    private int rejectedWorseCount;
    private int bestFoundIteration;

    public IteratedLocalSearchWithConstructionStartAndSaAcceptance(
        String name,
        OptimizationAlgorithm startAlgorithm,
        CycleImprover localSearch,
        IlsPerturbation perturbation,
        long timeLimitNanos,
        long seed,
        double initialTemperature,
        double finalTemperature,
        CoolingSchedule coolingSchedule
    ) {
        this.name = name;
        this.startAlgorithm = startAlgorithm;
        this.localSearch = localSearch;
        this.perturbation = perturbation;
        this.timeLimitNanos = timeLimitNanos;
        this.random = new Random(seed);
        this.initialTemperature = initialTemperature;
        this.finalTemperature = finalTemperature;
        this.coolingSchedule = coolingSchedule;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Solution solve(Instance instance, int startVertexId) {
        lastIterationCount = 0;
        acceptedBetterCount = 0;
        acceptedWorseCount = 0;
        rejectedWorseCount = 0;
        bestFoundIteration = 0;

        long startTime = System.nanoTime();
        long endTime = startTime + timeLimitNanos;

        Solution start = startAlgorithm.solve(instance, startVertexId);
        Cycle currentCycle = new Cycle(start.cycle(), instance.size);
        localSearch.improve(instance, currentCycle);
        int currentObjective = SolutionObjective.calculate(instance, currentCycle);

        Cycle bestCycle = new Cycle(currentCycle);
        int bestObjective = currentObjective;

        while (System.nanoTime() < endTime) {
            long now = System.nanoTime();
            double temperature = temperature(now - startTime);

            Cycle candidateCycle = new Cycle(currentCycle);
            perturbation.perturb(instance, candidateCycle, random);
            localSearch.improve(instance, candidateCycle);
            int candidateObjective = SolutionObjective.calculate(instance, candidateCycle);
            int delta = candidateObjective - currentObjective;

            lastIterationCount++;

            boolean accepted = false;
            if (delta > 0) {
                acceptedBetterCount++;
                accepted = true;
            } else if (random.nextDouble() < Math.exp(delta / temperature)) {
                acceptedWorseCount++;
                accepted = true;
            } else {
                rejectedWorseCount++;
            }

            if (accepted) {
                currentCycle = candidateCycle;
                currentObjective = candidateObjective;

                if (currentObjective > bestObjective) {
                    bestCycle = new Cycle(currentCycle);
                    bestObjective = currentObjective;
                    bestFoundIteration = lastIterationCount;
                }
            }
        }

        return new Solution(
            start.instanceName(),
            start.startVertexId(),
            bestCycle.toList()
        );
    }

    private double temperature(long elapsedNanos) {
        double progress = (double) elapsedNanos / (double) timeLimitNanos;
        if (progress > 1.0) {
            progress = 1.0;
        }

        if (coolingSchedule == CoolingSchedule.LINEAR) {
            return initialTemperature + progress * (finalTemperature - initialTemperature);
        }

        return initialTemperature * Math.pow(finalTemperature / initialTemperature, progress);
    }

    @Override
    public int lastIterationCount() {
        return lastIterationCount;
    }

    @Override
    public int acceptedBetterCount() {
        return acceptedBetterCount;
    }

    @Override
    public int acceptedWorseCount() {
        return acceptedWorseCount;
    }

    @Override
    public int rejectedWorseCount() {
        return rejectedWorseCount;
    }

    @Override
    public int bestFoundIteration() {
        return bestFoundIteration;
    }
}
