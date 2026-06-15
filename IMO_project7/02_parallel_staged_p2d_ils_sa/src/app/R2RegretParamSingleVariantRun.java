package app;

import algorithm.IterationCountingAlgorithm;
import algorithm.OptimizationAlgorithm;
import algorithm.construction.Randomized2RegretParamBestNWithPhaseTwoDeleteStart;
import algorithm.localsearch.SteepestLocalSearchWithCandidateMoves;
import algorithm.metaheuristic.CoolingSchedule;
import algorithm.metaheuristic.IteratedLocalSearchWithConstructionStartAndSaAcceptance;
import algorithm.metaheuristic.IteratedLocalSearchWithTwoRegretStartAndSaAcceptance;
import algorithm.metaheuristic.SaAcceptanceStatistics;
import algorithm.metaheuristic.perturbation.RandomSwapEdgesPerturbation;
import evaluation.SolutionEvaluator;
import experiment.core.RunConfig;
import experiment.core.RunPreparator;
import io.CsvInstanceReader;
import model.Instance;
import model.Solution;

import java.util.List;

public final class R2RegretParamSingleVariantRun {
    private static final long TIME_LIMIT_NANOS = 500_000_000L;
    private static final double T0 = 300.0;
    private static final double TMIN = 10.0;
    private static final CoolingSchedule COOLING = CoolingSchedule.GEOMETRIC;

    public static void main(String[] args) {
        String instancePath = args[0];
        String name = args[1];
        int topK = Integer.parseInt(args[2]);
        double probability = Double.parseDouble(args[3]);
        int triesCount = Integer.parseInt(args[4]);
        int runsCount = Integer.parseInt(args[5]);
        long baseSeed = Long.parseLong(args[6]);
        boolean baseline = Boolean.parseBoolean(args[7]);

        Instance instance = new CsvInstanceReader().read(instancePath);
        List<RunConfig> runs = RunPreparator.prepareRuns(instance, baseSeed, runsCount);

        for (int runIndex = 0; runIndex < runs.size(); runIndex++) {
            RunConfig run = runs.get(runIndex);
            OptimizationAlgorithm algorithm = createAlgorithm(name, topK, probability, triesCount, run.runSeed(), baseline);
            long start = System.nanoTime();
            Solution solution = algorithm.solve(instance, run.startVertexId());
            long runtime = System.nanoTime() - start;
            int objective = new SolutionEvaluator().evaluate(instance, solution).objective();
            int iterationCount = ((IterationCountingAlgorithm) algorithm).lastIterationCount();
            SaAcceptanceStatistics stats = (SaAcceptanceStatistics) algorithm;

            System.out.println(
                instance.name + "," + algorithm.name() + "," + runIndex + ","
                    + run.startVertexId() + "," + run.runSeed() + ","
                    + objective + "," + runtime + "," + iterationCount + ","
                    + stats.acceptedBetterCount() + "," + stats.acceptedWorseCount() + ","
                    + stats.rejectedWorseCount() + "," + stats.bestFoundIteration() + ","
                    + topK + "," + probability + "," + triesCount + ","
                    + COOLING + "," + T0 + "," + TMIN
            );
            System.out.flush();
        }
    }

    private static OptimizationAlgorithm createAlgorithm(String name, int topK, double probability, int triesCount, long seed, boolean baseline) {
        if (baseline) {
            return new IteratedLocalSearchWithTwoRegretStartAndSaAcceptance(
                name,
                new SteepestLocalSearchWithCandidateMoves(),
                new RandomSwapEdgesPerturbation(30),
                TIME_LIMIT_NANOS,
                seed,
                T0,
                TMIN,
                COOLING
            );
        }

        return new IteratedLocalSearchWithConstructionStartAndSaAcceptance(
            name,
            new Randomized2RegretParamBestNWithPhaseTwoDeleteStart(
                name + "_START",
                topK,
                probability,
                triesCount,
                seed,
                new SteepestLocalSearchWithCandidateMoves()
            ),
            new SteepestLocalSearchWithCandidateMoves(),
            new RandomSwapEdgesPerturbation(30),
            TIME_LIMIT_NANOS,
            seed,
            T0,
            TMIN,
            COOLING
        );
    }
}
