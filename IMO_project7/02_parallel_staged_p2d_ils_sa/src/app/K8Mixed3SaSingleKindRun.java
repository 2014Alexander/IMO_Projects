package app;

import algorithm.IterationCountingAlgorithm;
import algorithm.OptimizationAlgorithm;
import algorithm.construction.K8Mixed3WithPhaseTwoDeleteStart;
import algorithm.construction.K8Top3P20BestOf3WithPhaseTwoDeleteStart;
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

public final class K8Mixed3SaSingleKindRun {
    private static final long TIME_LIMIT_NANOS = 500_000_000L;
    private static final double T0 = 300.0;
    private static final double TMIN = 10.0;
    private static final CoolingSchedule COOLING = CoolingSchedule.GEOMETRIC;

    public static void main(String[] args) {
        String instancePath = args[0];
        String algorithmKind = args[1];
        int runsCount = Integer.parseInt(args[2]);
        long baseSeed = Long.parseLong(args[3]);
        boolean includeHeader = args.length < 5 || Boolean.parseBoolean(args[4]);

        Instance instance = new CsvInstanceReader().read(instancePath);
        List<RunConfig> runs = RunPreparator.prepareRuns(instance, baseSeed, runsCount);

        if (includeHeader) {
            System.out.println("instance,algorithm,runIndex,startVertexId,seed,objective,runtimeNanos,iterationCount,acceptedBetterCount,acceptedWorseCount,rejectedWorseCount,bestFoundIteration,cooling,T0,Tmin");
        }

        for (int runIndex = 0; runIndex < runs.size(); runIndex++) {
            RunConfig run = runs.get(runIndex);
            OptimizationAlgorithm algorithm = createAlgorithm(algorithmKind, run.runSeed());
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
                    + COOLING + "," + T0 + "," + TMIN
            );
            System.out.flush();
        }
    }

    private static OptimizationAlgorithm createAlgorithm(String algorithmKind, long seed) {
        if (algorithmKind.equals("top3_best3")) {
            return new IteratedLocalSearchWithConstructionStartAndSaAcceptance(
                "ILS_K8_TOP3P20_BEST3_SAME_START_P2D_SA_GEO_T0_300_TMIN_10",
                new K8Top3P20BestOf3WithPhaseTwoDeleteStart(seed, new SteepestLocalSearchWithCandidateMoves()),
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
            "ILS_K8_MIXED3_SAME_START_P2D_SA_GEO_T0_300_TMIN_10",
            new K8Mixed3WithPhaseTwoDeleteStart(seed, new SteepestLocalSearchWithCandidateMoves()),
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
