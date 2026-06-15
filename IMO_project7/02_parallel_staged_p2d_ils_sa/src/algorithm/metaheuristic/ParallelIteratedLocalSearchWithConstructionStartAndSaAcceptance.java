package algorithm.metaheuristic;

import algorithm.CycleImprover;
import algorithm.IterationCountingAlgorithm;
import algorithm.OptimizationAlgorithm;
import algorithm.SolutionObjective;
import algorithm.localsearch.Cycle;
import algorithm.metaheuristic.perturbation.IlsPerturbation;
import model.Instance;
import model.Solution;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

/**
 * Wielowątkowy ILS-SA: każdy wątek wykonuje niezależną trajektorię od tej samej
 * startowej vertices, a algorytm zwraca najlepsze rozwiązanie ze wszystkich wątków.
 */
public final class ParallelIteratedLocalSearchWithConstructionStartAndSaAcceptance
        implements OptimizationAlgorithm, IterationCountingAlgorithm, SaAcceptanceStatistics {
    private final String name;
    private final Supplier<OptimizationAlgorithm> startAlgorithmSupplier;
    private final Supplier<CycleImprover> localSearchSupplier;
    private final Supplier<IlsPerturbation> perturbationSupplier;
    private final long timeLimitNanos;
    private final long baseSeed;
    private final double initialTemperature;
    private final double finalTemperature;
    private final CoolingSchedule coolingSchedule;
    private final int threadCount;

    private int lastIterationCount;
    private int acceptedBetterCount;
    private int acceptedWorseCount;
    private int rejectedWorseCount;
    private int bestFoundIteration;

    public ParallelIteratedLocalSearchWithConstructionStartAndSaAcceptance(
            String name,
            Supplier<OptimizationAlgorithm> startAlgorithmSupplier,
            Supplier<CycleImprover> localSearchSupplier,
            Supplier<IlsPerturbation> perturbationSupplier,
            long timeLimitNanos,
            long baseSeed,
            double initialTemperature,
            double finalTemperature,
            CoolingSchedule coolingSchedule,
            int threadCount
    ) {
        this.name = name;
        this.startAlgorithmSupplier = startAlgorithmSupplier;
        this.localSearchSupplier = localSearchSupplier;
        this.perturbationSupplier = perturbationSupplier;
        this.timeLimitNanos = timeLimitNanos;
        this.baseSeed = baseSeed;
        this.initialTemperature = initialTemperature;
        this.finalTemperature = finalTemperature;
        this.coolingSchedule = coolingSchedule;
        this.threadCount = threadCount;
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

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<WorkerResult>> futures = new ArrayList<>();
        long globalStartTime = System.nanoTime();
        long globalEndTime = globalStartTime + timeLimitNanos;

        for (int threadIndex = 0; threadIndex < threadCount; threadIndex++) {
            long threadSeed = baseSeed + 100_000L * (threadIndex + 1);
            futures.add(executor.submit(new Worker(
                    instance,
                    startVertexId,
                    threadSeed,
                    globalStartTime,
                    globalEndTime,
                    startAlgorithmSupplier.get(),
                    localSearchSupplier.get(),
                    perturbationSupplier.get()
            )));
        }

        WorkerResult bestResult = null;
        try {
            for (Future<WorkerResult> future : futures) {
                WorkerResult result = future.get();
                lastIterationCount += result.iterationCount;
                acceptedBetterCount += result.acceptedBetterCount;
                acceptedWorseCount += result.acceptedWorseCount;
                rejectedWorseCount += result.rejectedWorseCount;
                if (bestResult == null || result.objective > bestResult.objective) {
                    bestResult = result;
                    bestFoundIteration = result.bestFoundIteration;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Parallel ILS-SA interrupted", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Parallel ILS-SA worker failed", e);
        } finally {
            executor.shutdownNow();
        }

        return new Solution(instance.name, startVertexId, bestResult.bestCycle.toList());
    }

    private double temperature(long elapsedNanos) {
        double progress = (double) elapsedNanos / (double) timeLimitNanos;
        if (progress > 1.0) {
            progress = 1.0;
        }
        if (progress < 0.0) {
            progress = 0.0;
        }

        if (coolingSchedule == CoolingSchedule.LINEAR) {
            return initialTemperature + progress * (finalTemperature - initialTemperature);
        }

        return initialTemperature * Math.pow(finalTemperature / initialTemperature, progress);
    }

    private final class Worker implements Callable<WorkerResult> {
        private final Instance instance;
        private final int startVertexId;
        private final long seed;
        private final long globalStartTime;
        private final long globalEndTime;
        private final OptimizationAlgorithm startAlgorithm;
        private final CycleImprover localSearch;
        private final IlsPerturbation perturbation;

        private Worker(
                Instance instance,
                int startVertexId,
                long seed,
                long globalStartTime,
                long globalEndTime,
                OptimizationAlgorithm startAlgorithm,
                CycleImprover localSearch,
                IlsPerturbation perturbation
        ) {
            this.instance = instance;
            this.startVertexId = startVertexId;
            this.seed = seed;
            this.globalStartTime = globalStartTime;
            this.globalEndTime = globalEndTime;
            this.startAlgorithm = startAlgorithm;
            this.localSearch = localSearch;
            this.perturbation = perturbation;
        }

        @Override
        public WorkerResult call() {
            Random random = new Random(seed);
            int iterationCount = 0;
            int acceptedBetter = 0;
            int acceptedWorse = 0;
            int rejectedWorse = 0;
            int localBestFoundIteration = 0;

            Solution start = startAlgorithm.solve(instance, startVertexId);
            Cycle currentCycle = new Cycle(start.cycle(), instance.size);
            localSearch.improve(instance, currentCycle);
            int currentObjective = SolutionObjective.calculate(instance, currentCycle);

            Cycle bestCycle = new Cycle(currentCycle);
            int bestObjective = currentObjective;

            while (System.nanoTime() < globalEndTime) {
                long now = System.nanoTime();
                double temperature = temperature(now - globalStartTime);

                Cycle candidateCycle = new Cycle(currentCycle);
                perturbation.perturb(instance, candidateCycle, random);
                localSearch.improve(instance, candidateCycle);
                int candidateObjective = SolutionObjective.calculate(instance, candidateCycle);
                int delta = candidateObjective - currentObjective;

                iterationCount++;

                boolean accepted = false;
                if (delta > 0) {
                    acceptedBetter++;
                    accepted = true;
                } else if (random.nextDouble() < Math.exp(delta / temperature)) {
                    acceptedWorse++;
                    accepted = true;
                } else {
                    rejectedWorse++;
                }

                if (accepted) {
                    currentCycle = candidateCycle;
                    currentObjective = candidateObjective;

                    if (currentObjective > bestObjective) {
                        bestCycle = new Cycle(currentCycle);
                        bestObjective = currentObjective;
                        localBestFoundIteration = iterationCount;
                    }
                }
            }

            return new WorkerResult(
                    bestCycle,
                    bestObjective,
                    iterationCount,
                    acceptedBetter,
                    acceptedWorse,
                    rejectedWorse,
                    localBestFoundIteration
            );
        }
    }

    private record WorkerResult(
            Cycle bestCycle,
            int objective,
            int iterationCount,
            int acceptedBetterCount,
            int acceptedWorseCount,
            int rejectedWorseCount,
            int bestFoundIteration
    ) {}

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
