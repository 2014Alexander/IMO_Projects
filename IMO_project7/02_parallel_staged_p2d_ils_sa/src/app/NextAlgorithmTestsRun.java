package app;

import algorithm.IterationCountingAlgorithm;
import algorithm.OptimizationAlgorithm;
import algorithm.construction.CheapConsensusR2Top3P30Parameterized;
import algorithm.construction.ConsensusR2Top3P30LSExtractAll10Top3;
import algorithm.construction.Randomized2RegretParamBestNWithPhaseTwoDeleteStart;
import algorithm.localsearch.SteepestLocalSearchWithCandidateMoves;
import algorithm.metaheuristic.CoolingSchedule;
import algorithm.metaheuristic.IteratedLocalSearchWithConstructionStartAndSaAcceptance;
import algorithm.metaheuristic.IteratedLocalSearchWithFrequencyBackboneSaAcceptance;
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

public final class NextAlgorithmTestsRun {
    private static final long TIME_LIMIT_NANOS = 500_000_000L;
    private static final int DEFAULT_RUNS_COUNT = 10;
    private static final long DEFAULT_BASE_SEED = 20260613L;
    private static final double T0 = 300.0;
    private static final double TMIN = 10.0;
    private static final int SWAP_EDGES = 30;
    private static final double FIXED_BREAK_PROBABILITY = 0.20;
    private static final CoolingSchedule COOLING = CoolingSchedule.GEOMETRIC;

    private enum Kind {
        TWO_REGRET,
        R2_BEST12,
        CONSENSUS_R2_ALL10_TOP3,
        CHEAP_LSTOP5_START,
        CHEAP_LSTOP5_DYNAMIC_FIXED,
        CHEAP_LSTOP5_DYNAMIC_ADAPTIVE,
        CHEAP_LSTOP5_FREQ5_MIN3_FIXED,
        CHEAP_LSTOP5_FREQ5_MIN3_ADAPTIVE,
        CHEAP_LSTOP5_FREQ7_MIN4_FIXED,
        CHEAP_LSTOP3_DYNAMIC_FIXED,
        CHEAP_LSTOP4_DYNAMIC_FIXED,
        CHEAP_LSTOP5_OR_BEST_PARENT_DYNAMIC_FIXED
    }

    private record Variant(String name, Kind kind) {}

    public static void main(String[] args) {
        int runsCount = args.length >= 1 ? Integer.parseInt(args[0]) : DEFAULT_RUNS_COUNT;
        long baseSeed = args.length >= 2 ? Long.parseLong(args[1]) : DEFAULT_BASE_SEED;
        String[] instancePaths = args.length >= 3 ? instancePaths(args) : new String[] {"data/TSPA.csv", "data/TSPB.csv"};

        System.out.println("instance,algorithm,runIndex,startVertexId,seed,objective,runtimeNanos,iterationCount,acceptedBetterCount,acceptedWorseCount,rejectedWorseCount,bestFoundIteration,eliteUpdateCount,lastProtectedEdgesCount,minProtectedEdgesCount,maxProtectedEdgesCount,avgProtectedEdgesOnUpdate,swapEdges,cooling,T0,Tmin,breakProbability,adaptiveProtection,archiveCapacity,minEdgeFrequency,lsCandidates,eliteSize,bestParentFallback");
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
        List<Variant> variants = new ArrayList<>();
        variants.add(new Variant("ILS_2REGRET_P2D_START_SA", Kind.TWO_REGRET));
        variants.add(new Variant("ILS_R2REGRET_TOP3P30_BEST12_P2D_LS_START_SA", Kind.R2_BEST12));
        variants.add(new Variant("ILS_CONSENSUS_R2_TOP3P30_LSEXTRACT_ALL10_TOP3_START_SA", Kind.CONSENSUS_R2_ALL10_TOP3));
        variants.add(new Variant("ILS_CHEAP_CONSENSUS_R2_TOP3P30_P2DALL10_LSTOP5_TOP3_START_SA", Kind.CHEAP_LSTOP5_START));
        variants.add(new Variant("ILS_CHEAP_CONSENSUS_R2_TOP3P30_LSTOP5_DYNAMIC_BACKBONE_FIXED", Kind.CHEAP_LSTOP5_DYNAMIC_FIXED));
        variants.add(new Variant("ILS_CHEAP_CONSENSUS_R2_TOP3P30_LSTOP5_DYNAMIC_BACKBONE_ADAPTIVE", Kind.CHEAP_LSTOP5_DYNAMIC_ADAPTIVE));
        variants.add(new Variant("ILS_CHEAP_CONSENSUS_R2_TOP3P30_LSTOP5_FREQ_ARCHIVE5_MIN3_FIXED", Kind.CHEAP_LSTOP5_FREQ5_MIN3_FIXED));
        variants.add(new Variant("ILS_CHEAP_CONSENSUS_R2_TOP3P30_LSTOP5_FREQ_ARCHIVE5_MIN3_ADAPTIVE", Kind.CHEAP_LSTOP5_FREQ5_MIN3_ADAPTIVE));
        variants.add(new Variant("ILS_CHEAP_CONSENSUS_R2_TOP3P30_LSTOP5_FREQ_ARCHIVE7_MIN4_FIXED", Kind.CHEAP_LSTOP5_FREQ7_MIN4_FIXED));
        variants.add(new Variant("ILS_CHEAP_CONSENSUS_R2_TOP3P30_LSTOP3_DYNAMIC_BACKBONE_FIXED", Kind.CHEAP_LSTOP3_DYNAMIC_FIXED));
        variants.add(new Variant("ILS_CHEAP_CONSENSUS_R2_TOP3P30_LSTOP4_DYNAMIC_BACKBONE_FIXED", Kind.CHEAP_LSTOP4_DYNAMIC_FIXED));
        variants.add(new Variant("ILS_CHEAP_CONSENSUS_R2_TOP3P30_LSTOP5_OR_BEST_PARENT_DYNAMIC_FIXED", Kind.CHEAP_LSTOP5_OR_BEST_PARENT_DYNAMIC_FIXED));
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

            System.out.println(instance.name + "," + algorithm.name() + "," + runIndex + "," + run.startVertexId() + "," + run.runSeed() + "," + objective + "," + runtime + "," + iterationCount + "," + stats.acceptedBetterCount() + "," + stats.acceptedWorseCount() + "," + stats.rejectedWorseCount() + "," + stats.bestFoundIteration() + "," + eliteUpdateCount + "," + lastProtectedEdgesCount + "," + minProtectedEdgesCount + "," + maxProtectedEdgesCount + "," + avgProtectedEdgesOnUpdate + "," + SWAP_EDGES + "," + COOLING + "," + T0 + "," + TMIN + "," + config.breakProbability + "," + config.adaptiveProtection + "," + config.archiveCapacity + "," + config.minEdgeFrequency + "," + config.lsCandidates + "," + config.eliteSize + "," + config.bestParentFallback);
            System.out.flush();
        }
    }

    private static OptimizationAlgorithm createAlgorithm(Variant variant, VariantConfig config, long seed) {
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
                    config.adaptiveProtection
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
            case TWO_REGRET -> throw new IllegalStateException("TWO_REGRET has separate ILS class");
            case R2_BEST12 -> new Randomized2RegretParamBestNWithPhaseTwoDeleteStart(
                    name + "_START", 3, 0.30, 12, seed, new SteepestLocalSearchWithCandidateMoves());
            case CONSENSUS_R2_ALL10_TOP3 -> new ConsensusR2Top3P30LSExtractAll10Top3(seed);
            default -> new CheapConsensusR2Top3P30Parameterized(
                    seed,
                    config.lsCandidates,
                    config.eliteSize,
                    config.bestParentFallback
            );
        };
    }

    private static VariantConfig config(Kind kind) {
        return switch (kind) {
            case TWO_REGRET, R2_BEST12, CONSENSUS_R2_ALL10_TOP3, CHEAP_LSTOP5_START ->
                    new VariantConfig(5, 3, false, 0, 0, FIXED_BREAK_PROBABILITY, false);
            case CHEAP_LSTOP5_DYNAMIC_FIXED ->
                    new VariantConfig(5, 3, false, 3, 3, FIXED_BREAK_PROBABILITY, false);
            case CHEAP_LSTOP5_DYNAMIC_ADAPTIVE ->
                    new VariantConfig(5, 3, false, 3, 3, FIXED_BREAK_PROBABILITY, true);
            case CHEAP_LSTOP5_FREQ5_MIN3_FIXED ->
                    new VariantConfig(5, 3, false, 5, 3, FIXED_BREAK_PROBABILITY, false);
            case CHEAP_LSTOP5_FREQ5_MIN3_ADAPTIVE ->
                    new VariantConfig(5, 3, false, 5, 3, FIXED_BREAK_PROBABILITY, true);
            case CHEAP_LSTOP5_FREQ7_MIN4_FIXED ->
                    new VariantConfig(5, 3, false, 7, 4, FIXED_BREAK_PROBABILITY, false);
            case CHEAP_LSTOP3_DYNAMIC_FIXED ->
                    new VariantConfig(3, 3, false, 3, 3, FIXED_BREAK_PROBABILITY, false);
            case CHEAP_LSTOP4_DYNAMIC_FIXED ->
                    new VariantConfig(4, 3, false, 3, 3, FIXED_BREAK_PROBABILITY, false);
            case CHEAP_LSTOP5_OR_BEST_PARENT_DYNAMIC_FIXED ->
                    new VariantConfig(5, 3, true, 3, 3, FIXED_BREAK_PROBABILITY, false);
        };
    }

    private static String[] instancePaths(String[] args) {
        String[] paths = new String[args.length - 2];
        System.arraycopy(args, 2, paths, 0, paths.length);
        return paths;
    }

    private record VariantConfig(
            int lsCandidates,
            int eliteSize,
            boolean bestParentFallback,
            int archiveCapacity,
            int minEdgeFrequency,
            double breakProbability,
            boolean adaptiveProtection
    ) {}
}
