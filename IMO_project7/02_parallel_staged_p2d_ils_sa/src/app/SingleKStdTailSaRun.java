package app;

import algorithm.IterationCountingAlgorithm;
import algorithm.OptimizationAlgorithm;
import algorithm.construction.K8M3StdWithPhaseTwoDelete;
import algorithm.construction.K12M3StdWithPhaseTwoDelete;
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

public final class SingleKStdTailSaRun {
    private static final long TIME_LIMIT_NANOS = 500_000_000L;
    private static final double T0 = 300.0;
    private static final double TMIN = 10.0;
    private static final CoolingSchedule COOLING = CoolingSchedule.GEOMETRIC;

    public static void main(String[] args) {
        String instancePath = args[0];
        String algorithmKind = args[1];
        int startVertexId = Integer.parseInt(args[2]);
        long seed = Long.parseLong(args[3]);
        Instance instance = new CsvInstanceReader().read(instancePath);
        OptimizationAlgorithm algorithm = createAlgorithm(algorithmKind, seed);

        long start = System.nanoTime();
        Solution solution = algorithm.solve(instance, startVertexId);
        long runtime = System.nanoTime() - start;

        int objective = new SolutionEvaluator().evaluate(instance, solution).objective();
        int iterations = ((IterationCountingAlgorithm) algorithm).lastIterationCount();
        SaAcceptanceStatistics stats = (SaAcceptanceStatistics) algorithm;

        System.out.println(instance.name + "," + algorithm.name() + "," + startVertexId + "," + seed + "," + objective + "," + runtime + "," + iterations + "," + stats.acceptedBetterCount() + "," + stats.acceptedWorseCount() + "," + stats.rejectedWorseCount() + "," + stats.bestFoundIteration());
    }

    private static OptimizationAlgorithm createAlgorithm(String algorithmKind, long seed) {
        if (algorithmKind.equals("k8")) {
            return new IteratedLocalSearchWithConstructionStartAndSaAcceptance(
                "ILS_K8_M3_STD_P2D_START_SA_GEO_T0_300_TMIN_10",
                new K8M3StdWithPhaseTwoDelete(),
                new SteepestLocalSearchWithCandidateMoves(),
                new RandomSwapEdgesPerturbation(30),
                TIME_LIMIT_NANOS,
                seed,
                T0,
                TMIN,
                COOLING
            );
        }

        if (algorithmKind.equals("k12")) {
            return new IteratedLocalSearchWithConstructionStartAndSaAcceptance(
                "ILS_K12_M3_STD_P2D_START_SA_GEO_T0_300_TMIN_10",
                new K12M3StdWithPhaseTwoDelete(),
                new SteepestLocalSearchWithCandidateMoves(),
                new RandomSwapEdgesPerturbation(30),
                TIME_LIMIT_NANOS,
                seed,
                T0,
                TMIN,
                COOLING
            );
        }

        return new IteratedLocalSearchWithTwoRegretStartAndSaAcceptance(
            "ILS_2REGRET_P2D_START_SA_GEO_T0_300_TMIN_10",
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
