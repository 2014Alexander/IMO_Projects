package app;

import algorithm.IterationCountingAlgorithm;
import algorithm.OptimizationAlgorithm;
import algorithm.construction.Randomized2RegretParamBestNWithPhaseTwoDeleteStart;
import algorithm.localsearch.SteepestLocalSearchWithCandidateMoves;
import algorithm.metaheuristic.CoolingSchedule;
import algorithm.metaheuristic.IteratedLocalSearchWithConstructionStartAndSaAcceptance;
import algorithm.metaheuristic.SaAcceptanceStatistics;
import algorithm.metaheuristic.perturbation.RandomSwapEdgesPerturbation;
import evaluation.SolutionEvaluator;
import experiment.core.RunConfig;
import experiment.core.RunPreparator;
import io.CsvInstanceReader;
import model.Instance;
import model.Solution;

import java.util.List;

public final class R2RegretUniversalSingleVariantRun {
    private static final long TIME_LIMIT_NANOS = 500_000_000L;
    private static final int TOP_K = 3;
    private static final double RANDOM_PROBABILITY = 0.30;
    private static final int TRIES_COUNT = 12;
    private static final CoolingSchedule COOLING = CoolingSchedule.GEOMETRIC;

    public static void main(String[] args) {
        if (args.length < 6) {
            throw new IllegalArgumentException("args: runsCount baseSeed instancePath swapEdges T0 Tmin");
        }
        int runsCount = Integer.parseInt(args[0]);
        long baseSeed = Long.parseLong(args[1]);
        String instancePath = args[2];
        int swapEdges = Integer.parseInt(args[3]);
        double t0 = Double.parseDouble(args[4]);
        double tmin = Double.parseDouble(args[5]);
        String name = String.format("ILS_R2REGRET_TOP3P30_BEST12_P2D_LS_START_SA_GEO_T0_%.0f_TMIN_%.0f_SWAP%d", t0, tmin, swapEdges);

        Instance instance = new CsvInstanceReader().read(instancePath);
        List<RunConfig> runs = RunPreparator.prepareRuns(instance, baseSeed, runsCount);
        System.out.println("instance,algorithm,runIndex,startVertexId,seed,objective,runtimeNanos,iterationCount,acceptedBetterCount,acceptedWorseCount,rejectedWorseCount,bestFoundIteration,topK,probability,triesCount,swapEdges,cooling,T0,Tmin");
        for (int runIndex = 0; runIndex < runs.size(); runIndex++) {
            RunConfig run = runs.get(runIndex);
            OptimizationAlgorithm algorithm = new IteratedLocalSearchWithConstructionStartAndSaAcceptance(
                name,
                new Randomized2RegretParamBestNWithPhaseTwoDeleteStart(name + "_START", TOP_K, RANDOM_PROBABILITY, TRIES_COUNT, run.runSeed(), new SteepestLocalSearchWithCandidateMoves()),
                new SteepestLocalSearchWithCandidateMoves(),
                new RandomSwapEdgesPerturbation(swapEdges),
                TIME_LIMIT_NANOS,
                run.runSeed(),
                t0,
                tmin,
                COOLING
            );
            long start = System.nanoTime();
            Solution solution = algorithm.solve(instance, run.startVertexId());
            long runtime = System.nanoTime() - start;
            int objective = new SolutionEvaluator().evaluate(instance, solution).objective();
            int iterationCount = ((IterationCountingAlgorithm) algorithm).lastIterationCount();
            SaAcceptanceStatistics stats = (SaAcceptanceStatistics) algorithm;
            System.out.println(instance.name + "," + algorithm.name() + "," + runIndex + "," + run.startVertexId() + "," + run.runSeed() + "," + objective + "," + runtime + "," + iterationCount + "," + stats.acceptedBetterCount() + "," + stats.acceptedWorseCount() + "," + stats.rejectedWorseCount() + "," + stats.bestFoundIteration() + "," + TOP_K + "," + RANDOM_PROBABILITY + "," + TRIES_COUNT + "," + swapEdges + "," + COOLING + "," + t0 + "," + tmin);
            System.out.flush();
        }
    }
}
