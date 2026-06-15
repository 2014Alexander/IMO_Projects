package app;

import algorithm.IterationCountingAlgorithm;
import algorithm.OptimizationAlgorithm;
import algorithm.construction.CheapConsensusR2Top3P30P2DAll10LSTop5Top3;
import algorithm.construction.ConsensusR2Top3P30LSExtractAll10Top3;
import algorithm.construction.Randomized2RegretParamBestNWithPhaseTwoDeleteStart;
import algorithm.localsearch.SteepestLocalSearchWithCandidateMoves;
import algorithm.metaheuristic.CoolingSchedule;
import algorithm.metaheuristic.IteratedLocalSearchWithConstructionStartAndSaAcceptance;
import algorithm.metaheuristic.IteratedLocalSearchWithDynamicEliteBackboneSaAcceptance;
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

public final class ConsensusDynamicBackboneSaRun {
    private static final long TIME_LIMIT_NANOS = 500_000_000L;
    private static final int DEFAULT_RUNS_COUNT = 10;
    private static final long DEFAULT_BASE_SEED = 12345L;
    private static final double T0 = 300.0;
    private static final double TMIN = 10.0;
    private static final int SWAP_EDGES = 30;
    private static final double PROTECTED_BREAK_PROBABILITY = 0.20;
    private static final CoolingSchedule COOLING = CoolingSchedule.GEOMETRIC;

    private enum Kind {
        TWO_REGRET,
        R2_BEST12,
        CONSENSUS_R2,
        CHEAP_CONSENSUS_R2,
        CONSENSUS_R2_DYNAMIC_BACKBONE,
        CHEAP_CONSENSUS_R2_DYNAMIC_BACKBONE
    }

    private record Variant(String name, Kind kind) {}

    public static void main(String[] args) {
        int runsCount = args.length >= 1 ? Integer.parseInt(args[0]) : DEFAULT_RUNS_COUNT;
        long baseSeed = args.length >= 2 ? Long.parseLong(args[1]) : DEFAULT_BASE_SEED;
        String[] instancePaths = args.length >= 3 ? instancePaths(args) : new String[] {"data/TSPA.csv", "data/TSPB.csv"};

        System.out.println("instance,algorithm,runIndex,startVertexId,seed,objective,runtimeNanos,iterationCount,acceptedBetterCount,acceptedWorseCount,rejectedWorseCount,bestFoundIteration,eliteUpdateCount,protectedEdgesCount,swapEdges,cooling,T0,Tmin,protectedBreakProbability");
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
        result.add(new Variant("ILS_CONSENSUS_R2_TOP3P30_LSEXTRACT_ALL10_TOP3_START_SA_GEO_T0_300_TMIN_10", Kind.CONSENSUS_R2));
        result.add(new Variant("ILS_CHEAP_CONSENSUS_R2_TOP3P30_P2DALL10_LSTOP5_TOP3_START_SA_GEO_T0_300_TMIN_10", Kind.CHEAP_CONSENSUS_R2));
        result.add(new Variant("ILS_CONSENSUS_R2_TOP3P30_DYNAMIC_ELITE_BACKBONE_OBJECTIVE_SA_GEO_T0_300_TMIN_10", Kind.CONSENSUS_R2_DYNAMIC_BACKBONE));
        result.add(new Variant("ILS_CHEAP_CONSENSUS_R2_TOP3P30_DYNAMIC_ELITE_BACKBONE_OBJECTIVE_SA_GEO_T0_300_TMIN_10", Kind.CHEAP_CONSENSUS_R2_DYNAMIC_BACKBONE));
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
            int eliteUpdateCount = 0;
            int protectedEdgesCount = 0;
            if (algorithm instanceof IteratedLocalSearchWithDynamicEliteBackboneSaAcceptance dynamic) {
                eliteUpdateCount = dynamic.eliteUpdateCount();
                protectedEdgesCount = dynamic.lastProtectedEdgesCount();
            }
            System.out.println(instance.name + "," + algorithm.name() + "," + runIndex + "," + run.startVertexId() + "," + run.runSeed() + "," + objective + "," + runtime + "," + iterationCount + "," + stats.acceptedBetterCount() + "," + stats.acceptedWorseCount() + "," + stats.rejectedWorseCount() + "," + stats.bestFoundIteration() + "," + eliteUpdateCount + "," + protectedEdgesCount + "," + SWAP_EDGES + "," + COOLING + "," + T0 + "," + TMIN + "," + PROTECTED_BREAK_PROBABILITY);
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

        if (variant.kind() == Kind.CONSENSUS_R2_DYNAMIC_BACKBONE
                || variant.kind() == Kind.CHEAP_CONSENSUS_R2_DYNAMIC_BACKBONE) {
            return new IteratedLocalSearchWithDynamicEliteBackboneSaAcceptance(
                    variant.name(),
                    createStartAlgorithm(variant, seed),
                    new SteepestLocalSearchWithCandidateMoves(),
                    TIME_LIMIT_NANOS,
                    seed,
                    T0,
                    TMIN,
                    COOLING,
                    SWAP_EDGES,
                    PROTECTED_BREAK_PROBABILITY
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
            case CONSENSUS_R2, CONSENSUS_R2_DYNAMIC_BACKBONE -> new ConsensusR2Top3P30LSExtractAll10Top3(seed);
            case CHEAP_CONSENSUS_R2, CHEAP_CONSENSUS_R2_DYNAMIC_BACKBONE -> new CheapConsensusR2Top3P30P2DAll10LSTop5Top3(seed);
            case TWO_REGRET -> throw new IllegalStateException("TWO_REGRET has separate ILS class");
        };
    }

    private static String[] instancePaths(String[] args) {
        String[] paths = new String[args.length - 2];
        System.arraycopy(args, 2, paths, 0, paths.length);
        return paths;
    }
}
