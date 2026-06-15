package app;

import algorithm.IterationCountingAlgorithm;
import algorithm.OptimizationAlgorithm;
import algorithm.construction.ConsensusR2Top3P30LSExtractAll10Top3;
import algorithm.construction.ConsensusTop3LSExtractAll10Top3;
import algorithm.construction.R2Top3P30Best12PlusConsensusBestStart;
import algorithm.construction.R2Top3P30Best12PlusConsensusR2BestStart;
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

import java.util.ArrayList;
import java.util.List;

public final class ConsensusPipelineSaRun {
    private static final long TIME_LIMIT_NANOS = 500_000_000L;
    private static final int DEFAULT_RUNS_COUNT = 10;
    private static final long DEFAULT_BASE_SEED = 12345L;
    private static final double T0 = 300.0;
    private static final double TMIN = 10.0;
    private static final int SWAP_EDGES = 30;
    private static final CoolingSchedule COOLING = CoolingSchedule.GEOMETRIC;

    private enum Kind {
        TWO_REGRET,
        R2_BEST12,
        CONSENSUS_ORIG,
        CONSENSUS_R2_TOP3P30,
        R2_PLUS_CONSENSUS_ORIG,
        R2_PLUS_CONSENSUS_R2
    }

    private record Variant(String name, Kind kind) {}

    public static void main(String[] args) {
        int runsCount = args.length >= 1 ? Integer.parseInt(args[0]) : DEFAULT_RUNS_COUNT;
        long baseSeed = args.length >= 2 ? Long.parseLong(args[1]) : DEFAULT_BASE_SEED;
        String[] instancePaths = args.length >= 3 ? instancePaths(args) : new String[] {"data/TSPA.csv", "data/TSPB.csv"};

        System.out.println("instance,algorithm,runIndex,startVertexId,seed,objective,runtimeNanos,iterationCount,acceptedBetterCount,acceptedWorseCount,rejectedWorseCount,bestFoundIteration,swapEdges,cooling,T0,Tmin");
        System.out.flush();

        for (String instancePath : instancePaths) {
            Instance instance = new CsvInstanceReader().read(instancePath);
            List<RunConfig> runs = RunPreparator.prepareRuns(instance, baseSeed, runsCount);
            for (Variant variant : variants()) {
                runVariant(instance, runs, variant);
            }
        }
    }

    private static List<Variant> variants() {
        List<Variant> result = new ArrayList<>();
        result.add(new Variant("ILS_2REGRET_P2D_START_SA_GEO_T0_300_TMIN_10", Kind.TWO_REGRET));
        result.add(new Variant("ILS_R2REGRET_TOP3P30_BEST12_P2D_LS_START_SA_GEO_T0_300_TMIN_10", Kind.R2_BEST12));
        result.add(new Variant("ILS_CONSENSUS_TOP3_LSEXTRACT_ALL10_TOP3_START_SA_GEO_T0_300_TMIN_10", Kind.CONSENSUS_ORIG));
        result.add(new Variant("ILS_CONSENSUS_R2_TOP3P30_LSEXTRACT_ALL10_TOP3_START_SA_GEO_T0_300_TMIN_10", Kind.CONSENSUS_R2_TOP3P30));
        result.add(new Variant("ILS_R2_TOP3P30_BEST12_PLUS_CONSENSUS_BEST_START_SA_GEO_T0_300_TMIN_10", Kind.R2_PLUS_CONSENSUS_ORIG));
        return result;
    }

    private static void runVariant(Instance instance, List<RunConfig> runs, Variant variant) {
        for (int runIndex = 0; runIndex < runs.size(); runIndex++) {
            RunConfig run = runs.get(runIndex);
            OptimizationAlgorithm algorithm = createAlgorithm(variant, run.runSeed());
            long start = System.nanoTime();
            Solution solution = algorithm.solve(instance, run.startVertexId());
            long runtime = System.nanoTime() - start;
            int objective = new SolutionEvaluator().evaluate(instance, solution).objective();
            int iterationCount = ((IterationCountingAlgorithm) algorithm).lastIterationCount();
            SaAcceptanceStatistics stats = (SaAcceptanceStatistics) algorithm;
            System.out.println(instance.name + "," + algorithm.name() + "," + runIndex + "," + run.startVertexId() + "," + run.runSeed() + "," + objective + "," + runtime + "," + iterationCount + "," + stats.acceptedBetterCount() + "," + stats.acceptedWorseCount() + "," + stats.rejectedWorseCount() + "," + stats.bestFoundIteration() + "," + SWAP_EDGES + "," + COOLING + "," + T0 + "," + TMIN);
            System.out.flush();
        }
    }

    private static OptimizationAlgorithm createAlgorithm(Variant variant, long seed) {
        if (variant.kind() == Kind.TWO_REGRET) {
            return new IteratedLocalSearchWithTwoRegretStartAndSaAcceptance(
                    variant.name(),
                    new SteepestLocalSearchWithCandidateMoves(),
                    new RandomSwapEdgesPerturbation(SWAP_EDGES),
                    TIME_LIMIT_NANOS,
                    seed,
                    T0,
                    TMIN,
                    COOLING
            );
        }

        return new IteratedLocalSearchWithConstructionStartAndSaAcceptance(
                variant.name(),
                createStartAlgorithm(variant, seed),
                new SteepestLocalSearchWithCandidateMoves(),
                new RandomSwapEdgesPerturbation(SWAP_EDGES),
                TIME_LIMIT_NANOS,
                seed,
                T0,
                TMIN,
                COOLING
        );
    }

    private static OptimizationAlgorithm createStartAlgorithm(Variant variant, long seed) {
        return switch (variant.kind()) {
            case R2_BEST12 -> new Randomized2RegretParamBestNWithPhaseTwoDeleteStart(
                    variant.name() + "_START",
                    3,
                    0.30,
                    12,
                    seed,
                    new SteepestLocalSearchWithCandidateMoves()
            );
            case CONSENSUS_ORIG -> new ConsensusTop3LSExtractAll10Top3(seed);
            case CONSENSUS_R2_TOP3P30 -> new ConsensusR2Top3P30LSExtractAll10Top3(seed);
            case R2_PLUS_CONSENSUS_ORIG -> new R2Top3P30Best12PlusConsensusBestStart(
                    variant.name() + "_START",
                    seed,
                    new SteepestLocalSearchWithCandidateMoves()
            );
            case R2_PLUS_CONSENSUS_R2 -> new R2Top3P30Best12PlusConsensusR2BestStart(
                    variant.name() + "_START",
                    seed,
                    new SteepestLocalSearchWithCandidateMoves()
            );
            case TWO_REGRET -> throw new IllegalStateException("TWO_REGRET has separate ILS class");
        };
    }

    private static String[] instancePaths(String[] args) {
        String[] paths = new String[args.length - 2];
        System.arraycopy(args, 2, paths, 0, paths.length);
        return paths;
    }
}
