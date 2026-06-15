package app;

import algorithm.IterationCountingAlgorithm;
import algorithm.OptimizationAlgorithm;
import algorithm.construction.TailK25After85M5After80WithPhaseTwoDelete;
import algorithm.localsearch.SteepestLocalSearchWithCandidateMoves;
import algorithm.metaheuristic.CoolingSchedule;
import algorithm.metaheuristic.IteratedLocalSearchWithConstructionStartAndSaAcceptance;
import algorithm.metaheuristic.IteratedLocalSearchWithTwoRegretStartAndSaAcceptance;
import algorithm.metaheuristic.SaAcceptanceStatistics;
import algorithm.metaheuristic.perturbation.RandomSwapEdgesPerturbation;
import evaluation.SolutionEvaluator;
import io.CsvInstanceReader;
import model.Instance;
import model.Solution;

public final class SingleTailStartSaRun {
    public static void main(String[] args) {
        String instancePath = args[0];
        String algorithmKind = args[1];
        int startVertexId = Integer.parseInt(args[2]);
        long seed = Long.parseLong(args[3]);
        long timeLimitNanos = 1_000_000_000L;
        Instance instance = new CsvInstanceReader().read(instancePath);
        String algorithmName = algorithmKind.equals("tail")
            ? "ILS_TAILK25_AFTER85_M5_AFTER80_P2D_START_SA_GEO_T0_300_TMIN_10"
            : "ILS_2REGRET_P2D_START_SA_GEO_T0_300_TMIN_10";
        OptimizationAlgorithm algorithm;
        if (algorithmKind.equals("tail")) {
            algorithm = new IteratedLocalSearchWithConstructionStartAndSaAcceptance(
                algorithmName,
                new TailK25After85M5After80WithPhaseTwoDelete(),
                new SteepestLocalSearchWithCandidateMoves(),
                new RandomSwapEdgesPerturbation(30),
                timeLimitNanos,
                seed,
                300.0,
                10.0,
                CoolingSchedule.GEOMETRIC
            );
        } else {
            algorithm = new IteratedLocalSearchWithTwoRegretStartAndSaAcceptance(
                algorithmName,
                new SteepestLocalSearchWithCandidateMoves(),
                new RandomSwapEdgesPerturbation(30),
                timeLimitNanos,
                seed,
                300.0,
                10.0,
                CoolingSchedule.GEOMETRIC
            );
        }
        long start = System.nanoTime();
        Solution solution = algorithm.solve(instance, startVertexId);
        long runtime = System.nanoTime() - start;
        int objective = new SolutionEvaluator().evaluate(instance, solution).objective();
        int iterations = ((IterationCountingAlgorithm) algorithm).lastIterationCount();
        SaAcceptanceStatistics stats = (SaAcceptanceStatistics) algorithm;
        System.out.println(instance.name + "," + algorithmName + "," + startVertexId + "," + seed + "," + objective + "," + runtime + "," + iterations + "," + stats.acceptedBetterCount() + "," + stats.acceptedWorseCount() + "," + stats.rejectedWorseCount() + "," + stats.bestFoundIteration());
    }
}
