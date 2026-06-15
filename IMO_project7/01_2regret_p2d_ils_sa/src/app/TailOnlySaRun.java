package app;

import algorithm.IterationCountingAlgorithm;
import algorithm.OptimizationAlgorithm;
import algorithm.construction.TailK25After85M5After80WithPhaseTwoDelete;
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

public final class TailOnlySaRun {
    public static void main(String[] args) {
        Instance instance = new CsvInstanceReader().read(args[0]);
        int runsCount = Integer.parseInt(args[1]);
        List<RunConfig> runs = RunPreparator.prepareRuns(instance, 12345L, runsCount);
        String algorithmName = "ILS_TAILK25_AFTER85_M5_AFTER80_P2D_START_SA_GEO_T0_300_TMIN_10";
        System.out.println("instance,algorithm,runIndex,startVertexId,seed,objective,runtimeNanos,iterationCount,acceptedBetterCount,acceptedWorseCount,rejectedWorseCount,bestFoundIteration,cooling,T0,Tmin");
        System.out.flush();
        for (int runIndex = 0; runIndex < runs.size(); runIndex++) {
            RunConfig run = runs.get(runIndex);
            OptimizationAlgorithm algorithm = new IteratedLocalSearchWithConstructionStartAndSaAcceptance(
                algorithmName,
                new TailK25After85M5After80WithPhaseTwoDelete(),
                new SteepestLocalSearchWithCandidateMoves(),
                new RandomSwapEdgesPerturbation(30),
                1_000_000_000L,
                run.runSeed(),
                300.0,
                10.0,
                CoolingSchedule.GEOMETRIC
            );
            long startTime = System.nanoTime();
            Solution solution = algorithm.solve(instance, run.startVertexId());
            long runtimeNanos = System.nanoTime() - startTime;
            int objective = new SolutionEvaluator().evaluate(instance, solution).objective();
            int iterationCount = ((IterationCountingAlgorithm) algorithm).lastIterationCount();
            SaAcceptanceStatistics stats = (SaAcceptanceStatistics) algorithm;
            System.out.println(instance.name + "," + algorithmName + "," + runIndex + "," + run.startVertexId() + "," + run.runSeed() + "," + objective + "," + runtimeNanos + "," + iterationCount + "," + stats.acceptedBetterCount() + "," + stats.acceptedWorseCount() + "," + stats.rejectedWorseCount() + "," + stats.bestFoundIteration() + ",GEOMETRIC,300.0,10.0");
            System.out.flush();
        }
    }
}
