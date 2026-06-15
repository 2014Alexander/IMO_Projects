package app;

import algorithm.IterationCountingAlgorithm;
import algorithm.OptimizationAlgorithm;
import algorithm.construction.CheapConsensusR2Top3P30Parameterized;
import algorithm.construction.FastRandomized2RegretParamBestNWithPhaseTwoDeleteStart;
import algorithm.construction.Randomized2RegretParamBestNWithPhaseTwoDeleteStart;
import algorithm.localsearch.SteepestLocalSearchWithCandidateMoves;
import algorithm.metaheuristic.CoolingSchedule;
import algorithm.metaheuristic.IteratedLocalSearchWithConstructionStartAndSaAcceptance;
import algorithm.metaheuristic.IteratedLocalSearchWithFrequencyBackboneSaAcceptance;
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
 * Compares standard randomized 2-regret with fast randomized 2-regret inside
 * R2 best-start and cheap-consensus dynamic-backbone pipelines.
 */
public final class FastRandomized2RegretIntegrationTestsRun {
    private static final long TIME_LIMIT_NANOS = 500_000_000L;
    private static final int DEFAULT_RUNS_COUNT = 20;
    private static final long DEFAULT_BASE_SEED = 20260613L;
    private static final double T0 = 300.0;
    private static final double TMIN = 10.0;
    private static final int SWAP_EDGES = 30;
    private static final double FIXED_BREAK_PROBABILITY = 0.20;
    private static final CoolingSchedule COOLING = CoolingSchedule.GEOMETRIC;

    private enum Kind {
        R2_BEST12,
        FAST_R2_BEST12,
        CHEAP_LSTOP5_OR_BEST_PARENT_DYNAMIC_FIXED,
        CHEAP_FAST_R2_LSTOP5_OR_BEST_PARENT_DYNAMIC_FIXED
    }

    private record Variant(String name, Kind kind) {}

    public static void main(String[] args) {
        int runsCount = args.length >= 1 ? Integer.parseInt(args[0]) : DEFAULT_RUNS_COUNT;
        long baseSeed = args.length >= 2 ? Long.parseLong(args[1]) : DEFAULT_BASE_SEED;
        String variantFilter = args.length >= 3 && !args[2].endsWith(".csv") ? args[2] : null;
        int pathsOffset = variantFilter == null ? 2 : 3;
        String[] instancePaths = args.length > pathsOffset ? instancePaths(args, pathsOffset) : new String[] {"data/TSPA.csv", "data/TSPB.csv"};

        System.out.println("instance,algorithm,runIndex,startVertexId,seed,objective,runtimeNanos,iterationCount,acceptedBetterCount,acceptedWorseCount,rejectedWorseCount,bestFoundIteration,eliteUpdateCount,lastProtectedEdgesCount,minProtectedEdgesCount,maxProtectedEdgesCount,avgProtectedEdgesOnUpdate,swapEdges,cooling,T0,Tmin,breakProbability,archiveCapacity,minEdgeFrequency,useFastRandomized,lsCandidates,eliteSize,bestParentFallback");
        System.out.flush();

        for (String instancePath : instancePaths) {
            Instance instance = new CsvInstanceReader().read(instancePath);
            List<RunConfig> runs = RunPreparator.prepareRuns(instance, baseSeed, runsCount);
            for (Variant variant : variants()) {
                if (variantFilter != null && !variant.name().equals(variantFilter) && !variant.kind().name().equals(variantFilter)) {
                    continue;
                }
                runVariant(instance, runs, variant);
            }
        }
    }

    private static List<Variant> variants() {
        List<Variant> variants = new ArrayList<>();
        variants.add(new Variant("ILS_R2REGRET_TOP3P30_BEST12_P2D_LS_START_SA", Kind.R2_BEST12));
        variants.add(new Variant("ILS_FAST_R2REGRET_TOP3P30_BEST12_P2D_LS_START_SA", Kind.FAST_R2_BEST12));
        variants.add(new Variant("ILS_CHEAP_CONSENSUS_R2_TOP3P30_LSTOP5_OR_BEST_PARENT_DYNAMIC_FIXED", Kind.CHEAP_LSTOP5_OR_BEST_PARENT_DYNAMIC_FIXED));
        variants.add(new Variant("ILS_CHEAP_CONSENSUS_FAST_R2_TOP3P30_LSTOP5_OR_BEST_PARENT_DYNAMIC_FIXED", Kind.CHEAP_FAST_R2_LSTOP5_OR_BEST_PARENT_DYNAMIC_FIXED));
        return variants;
    }

    private static void runVariant(Instance instance, List<RunConfig> runs, Variant variant) {
        for (int runIndex = 0; runIndex < runs.size(); runIndex++) {
            RunConfig run = runs.get(runIndex);
            VariantConfig config = config(variant.kind());
            OptimizationAlgorithm algorithm = createAlgorithm(variant, config, run.runSeed());
            long start = System.nanoTime();
            Solution solution = algorithm.solve(instance, run.startVertexId());
            long runtime = System.nanoTime() - start;
            int objective = new SolutionEvaluator().evaluate(instance, solution).objective();
            int iterationCount = ((IterationCountingAlgorithm) algorithm).lastIterationCount();
            SaAcceptanceStatistics stats = (SaAcceptanceStatistics) algorithm;

            int eliteUpdateCount = 0;
            int lastProtectedEdgesCount = 0;
            int minProtectedEdgesCount = 0;
            int maxProtectedEdgesCount = 0;
            double avgProtectedEdgesOnUpdate = 0.0;
            if (algorithm instanceof IteratedLocalSearchWithFrequencyBackboneSaAcceptance frequency) {
                eliteUpdateCount = frequency.eliteUpdateCount();
                lastProtectedEdgesCount = frequency.lastProtectedEdgesCount();
                minProtectedEdgesCount = frequency.minProtectedEdgesCount();
                maxProtectedEdgesCount = frequency.maxProtectedEdgesCount();
                avgProtectedEdgesOnUpdate = frequency.avgProtectedEdgesOnUpdate();
            }

            System.out.println(instance.name + "," + algorithm.name() + "," + runIndex + "," + run.startVertexId() + "," + run.runSeed() + "," + objective + "," + runtime + "," + iterationCount + "," + stats.acceptedBetterCount() + "," + stats.acceptedWorseCount() + "," + stats.rejectedWorseCount() + "," + stats.bestFoundIteration() + "," + eliteUpdateCount + "," + lastProtectedEdgesCount + "," + minProtectedEdgesCount + "," + maxProtectedEdgesCount + "," + avgProtectedEdgesOnUpdate + "," + SWAP_EDGES + "," + COOLING + "," + T0 + "," + TMIN + "," + config.breakProbability + "," + config.archiveCapacity + "," + config.minEdgeFrequency + "," + config.useFastRandomized + "," + config.lsCandidates + "," + config.eliteSize + "," + config.bestParentFallback);
            System.out.flush();
        }
    }

    private static OptimizationAlgorithm createAlgorithm(Variant variant, VariantConfig config, long seed) {
        if (config.archiveCapacity > 0) {
            return new IteratedLocalSearchWithFrequencyBackboneSaAcceptance(
                    variant.name(),
                    startAlgorithm(variant.kind(), variant.name(), seed, config),
                    new SteepestLocalSearchWithCandidateMoves(),
                    TIME_LIMIT_NANOS,
                    seed,
                    T0,
                    TMIN,
                    COOLING,
                    SWAP_EDGES,
                    config.archiveCapacity,
                    config.minEdgeFrequency,
                    config.breakProbability,
                    false
            );
        }

        return new IteratedLocalSearchWithConstructionStartAndSaAcceptance(
                variant.name(),
                startAlgorithm(variant.kind(), variant.name(), seed, config),
                new SteepestLocalSearchWithCandidateMoves(),
                new RandomSwapEdgesPerturbation(SWAP_EDGES),
                TIME_LIMIT_NANOS,
                seed,
                T0,
                TMIN,
                COOLING
        );
    }

    private static OptimizationAlgorithm startAlgorithm(Kind kind, String name, long seed, VariantConfig config) {
        return switch (kind) {
            case R2_BEST12 -> new Randomized2RegretParamBestNWithPhaseTwoDeleteStart(
                    name + "_START", 3, 0.30, 12, seed, new SteepestLocalSearchWithCandidateMoves());
            case FAST_R2_BEST12 -> new FastRandomized2RegretParamBestNWithPhaseTwoDeleteStart(
                    name + "_START", 3, 0.30, 12, seed, new SteepestLocalSearchWithCandidateMoves());
            case CHEAP_LSTOP5_OR_BEST_PARENT_DYNAMIC_FIXED -> new CheapConsensusR2Top3P30Parameterized(
                    seed, config.lsCandidates, config.eliteSize, config.bestParentFallback, false);
            case CHEAP_FAST_R2_LSTOP5_OR_BEST_PARENT_DYNAMIC_FIXED -> new CheapConsensusR2Top3P30Parameterized(
                    seed, config.lsCandidates, config.eliteSize, config.bestParentFallback, true);
        };
    }

    private static VariantConfig config(Kind kind) {
        return switch (kind) {
            case R2_BEST12 -> new VariantConfig(false, 0, 0, 0, 0, false, FIXED_BREAK_PROBABILITY);
            case FAST_R2_BEST12 -> new VariantConfig(true, 0, 0, 0, 0, false, FIXED_BREAK_PROBABILITY);
            case CHEAP_LSTOP5_OR_BEST_PARENT_DYNAMIC_FIXED -> new VariantConfig(false, 5, 3, 3, 3, true, FIXED_BREAK_PROBABILITY);
            case CHEAP_FAST_R2_LSTOP5_OR_BEST_PARENT_DYNAMIC_FIXED -> new VariantConfig(true, 5, 3, 3, 3, true, FIXED_BREAK_PROBABILITY);
        };
    }

    private static String[] instancePaths(String[] args, int offset) {
        String[] paths = new String[args.length - offset];
        System.arraycopy(args, offset, paths, 0, paths.length);
        return paths;
    }

    private record VariantConfig(
            boolean useFastRandomized,
            int lsCandidates,
            int eliteSize,
            int archiveCapacity,
            int minEdgeFrequency,
            boolean bestParentFallback,
            double breakProbability
    ) {}
}
