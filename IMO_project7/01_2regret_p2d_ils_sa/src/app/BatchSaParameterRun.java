package app;

import algorithm.IterationCountingAlgorithm;
import algorithm.OptimizationAlgorithm;
import algorithm.localsearch.SteepestLocalSearchWithCandidateMoves;
import algorithm.metaheuristic.CoolingSchedule;
import algorithm.metaheuristic.IteratedLocalSearch;
import algorithm.metaheuristic.IteratedLocalSearchWithTwoRegretStart;
import algorithm.metaheuristic.IteratedLocalSearchWithTwoRegretStartAndSaAcceptance;
import algorithm.metaheuristic.SaAcceptanceStatistics;
import algorithm.metaheuristic.perturbation.RandomSwapEdgesPerturbation;
import evaluation.SolutionEvaluator;
import experiment.core.RunConfig;
import experiment.core.RunPreparator;
import io.CsvInstanceReader;
import model.Instance;
import model.Solution;

import java.util.ArrayList;
import java.util.List;

/**
 * Uruchamia test parametrow akceptacji SA dla wariantu ILS z konstrukcyjnym startem 2-regret + druga faza.
 */
public final class BatchSaParameterRun {
    private record Variant(String name, boolean sa, CoolingSchedule coolingSchedule, double initialTemperature, double finalTemperature) {}

    public static void main(String[] args) {
        long timeLimitNanos = Long.parseLong(args[0]);
        int runsCount = Integer.parseInt(args[1]);
        long baseSeed = Long.parseLong(args[2]);

        List<Variant> variants = variants();
        System.out.println("instance,algorithm,runIndex,startVertexId,seed,objective,runtimeNanos,iterationCount,acceptedBetterCount,acceptedWorseCount,rejectedWorseCount,bestFoundIteration,cooling,T0,Tmin");

        for (int instanceArgument = 3; instanceArgument < args.length; instanceArgument++) {
            String instancePath = args[instanceArgument];
            Instance instance = new CsvInstanceReader().read(instancePath);
            List<RunConfig> runs = RunPreparator.prepareRuns(instance, baseSeed, runsCount);

            for (Variant variant : variants) {
                for (int runIndex = 0; runIndex < runs.size(); runIndex++) {
                    RunConfig run = runs.get(runIndex);
                    OptimizationAlgorithm algorithm = createAlgorithm(variant, timeLimitNanos, run.runSeed());

                    long startTime = System.nanoTime();
                    Solution solution = algorithm.solve(instance, run.startVertexId());
                    long runtimeNanos = System.nanoTime() - startTime;

                    int objective = new SolutionEvaluator().evaluate(instance, solution).objective();
                    int iterationCount = ((IterationCountingAlgorithm) algorithm).lastIterationCount();

                    int acceptedBetterCount = 0;
                    int acceptedWorseCount = 0;
                    int rejectedWorseCount = 0;
                    int bestFoundIteration = 0;

                    if (algorithm instanceof SaAcceptanceStatistics saStatistics) {
                        acceptedBetterCount = saStatistics.acceptedBetterCount();
                        acceptedWorseCount = saStatistics.acceptedWorseCount();
                        rejectedWorseCount = saStatistics.rejectedWorseCount();
                        bestFoundIteration = saStatistics.bestFoundIteration();
                    }

                    System.out.println(
                        instance.name + "," + variant.name + "," + runIndex + ","
                            + run.startVertexId() + "," + run.runSeed() + ","
                            + objective + "," + runtimeNanos + "," + iterationCount + ","
                            + acceptedBetterCount + "," + acceptedWorseCount + "," + rejectedWorseCount + "," + bestFoundIteration + ","
                            + variant.coolingSchedule + "," + variant.initialTemperature + "," + variant.finalTemperature
                    );
                }
            }
        }
    }

    private static List<Variant> variants() {
        List<Variant> variants = new ArrayList<>();
        variants.add(new Variant("ILS_RANDOM_START", false, CoolingSchedule.GEOMETRIC, 0.0, 0.0));
        variants.add(new Variant("ILS_2REGRET_P2D_START", false, CoolingSchedule.GEOMETRIC, 0.0, 0.0));

        double[] t0Values = {100.0, 200.0, 300.0, 500.0, 800.0};
        double[] tminValues = {1.0, 5.0, 10.0};

        for (double t0 : t0Values) {
            for (double tmin : tminValues) {
                variants.add(new Variant(
                    "ILS_2REGRET_P2D_START_SA_GEO_T0_" + intText(t0) + "_TMIN_" + intText(tmin),
                    true,
                    CoolingSchedule.GEOMETRIC,
                    t0,
                    tmin
                ));
            }
        }

        double[] linearT0Values = {200.0, 300.0, 500.0};
        double[] linearTminValues = {1.0, 5.0};
        for (double t0 : linearT0Values) {
            for (double tmin : linearTminValues) {
                variants.add(new Variant(
                    "ILS_2REGRET_P2D_START_SA_LINEAR_T0_" + intText(t0) + "_TMIN_" + intText(tmin),
                    true,
                    CoolingSchedule.LINEAR,
                    t0,
                    tmin
                ));
            }
        }

        return variants;
    }

    private static String intText(double value) {
        return Integer.toString((int) value);
    }

    private static OptimizationAlgorithm createAlgorithm(Variant variant, long timeLimitNanos, long seed) {
        if ("ILS_RANDOM_START".equals(variant.name)) {
            return new IteratedLocalSearch(
                variant.name,
                new SteepestLocalSearchWithCandidateMoves(),
                new RandomSwapEdgesPerturbation(),
                timeLimitNanos,
                seed
            );
        }

        if ("ILS_2REGRET_P2D_START".equals(variant.name)) {
            return new IteratedLocalSearchWithTwoRegretStart(
                variant.name,
                new SteepestLocalSearchWithCandidateMoves(),
                new RandomSwapEdgesPerturbation(),
                timeLimitNanos,
                seed
            );
        }

        return new IteratedLocalSearchWithTwoRegretStartAndSaAcceptance(
            variant.name,
            new SteepestLocalSearchWithCandidateMoves(),
            new RandomSwapEdgesPerturbation(),
            timeLimitNanos,
            seed,
            variant.initialTemperature,
            variant.finalTemperature,
            variant.coolingSchedule
        );
    }
}
