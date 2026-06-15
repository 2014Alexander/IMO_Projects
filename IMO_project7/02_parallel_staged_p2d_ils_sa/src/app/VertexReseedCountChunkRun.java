package app;

import algorithm.IterationCountingAlgorithm;
import algorithm.OptimizationAlgorithm;
import algorithm.construction.CheapConsensusR2Top3P30Parameterized;
import algorithm.construction.RepairReseedCheapConsensusFastR2Top3P30Parameterized;
import algorithm.localsearch.SteepestLocalSearchWithCandidateMoves;
import algorithm.metaheuristic.CoolingSchedule;
import algorithm.metaheuristic.IteratedLocalSearchWithFrequencyBackboneSaAcceptance;
import algorithm.metaheuristic.SaAcceptanceStatistics;
import evaluation.SolutionEvaluator;
import experiment.core.RunConfig;
import experiment.core.RunPreparator;
import io.CsvInstanceReader;
import model.Instance;
import model.Solution;

import java.util.List;

public final class VertexReseedCountChunkRun {
    private static final long TIME_LIMIT_NANOS = 500_000_000L;
    private static final double T0 = 300.0;
    private static final double TMIN = 10.0;
    private static final int SWAP_EDGES = 30;
    private static final double BREAK_PROBABILITY = 0.20;
    private static final CoolingSchedule COOLING = CoolingSchedule.GEOMETRIC;
    private static final int ARCHIVE_CAPACITY = 3;
    private static final int MIN_EDGE_FREQUENCY = 3;

    private record Variant(String name, int vertexReseedCount) {
        boolean baseline() { return vertexReseedCount == 0; }
    }

    public static void main(String[] args) {
        if (args.length < 5) {
            throw new IllegalArgumentException("Usage: VertexReseedCountChunkRun <vertexReseedCount:0|2|3|4|5> <offset> <count> <baseSeed> <paths...>");
        }
        int vertexReseedCount = Integer.parseInt(args[0]);
        int offset = Integer.parseInt(args[1]);
        int count = Integer.parseInt(args[2]);
        long baseSeed = Long.parseLong(args[3]);
        String[] instancePaths = new String[args.length - 4];
        System.arraycopy(args, 4, instancePaths, 0, instancePaths.length);
        Variant variant = variant(vertexReseedCount);

        System.out.println("instance,algorithm,runIndex,startVertexId,seed,objective,runtimeNanos,iterationCount,acceptedBetterCount,acceptedWorseCount,rejectedWorseCount,bestFoundIteration,eliteUpdateCount,lastProtectedEdgesCount,minProtectedEdgesCount,maxProtectedEdgesCount,avgProtectedEdgesOnUpdate,swapEdges,cooling,T0,Tmin,breakProbability,archiveCapacity,minEdgeFrequency,vertexReseedCount,variantKind");
        for (String instancePath : instancePaths) {
            Instance instance = new CsvInstanceReader().read(instancePath);
            List<RunConfig> runs = RunPreparator.prepareRuns(instance, baseSeed, offset + count);
            for (int runIndex = offset; runIndex < offset + count; runIndex++) {
                RunConfig run = runs.get(runIndex);
                runVariant(instance, run, runIndex, variant);
            }
        }
    }

    private static Variant variant(int count) {
        if (count == 0) {
            return new Variant("ILS_CHEAP_CONSENSUS_FAST_R2_TOP3P30_LSTOP5_OR_BEST_PARENT_DYNAMIC_FIXED", 0);
        }
        return new Variant("ILS_CHEAP_CONSENSUS_FAST_R2_TOP3P30_VERTEX_RESEED_TOP" + count + "_LSTOP5_OR_BEST_PARENT_DYNAMIC_FIXED", count);
    }

    private static void runVariant(Instance instance, RunConfig run, int runIndex, Variant variant) {
        OptimizationAlgorithm algorithm = createAlgorithm(variant, run.runSeed());
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
        String kind = variant.baseline() ? "BASELINE" : "VERTEX_RESEED_TOP" + variant.vertexReseedCount();
        System.out.println(instance.name + "," + algorithm.name() + "," + runIndex + "," + run.startVertexId() + "," + run.runSeed() + "," + objective + "," + runtime + "," + iterationCount + "," + stats.acceptedBetterCount() + "," + stats.acceptedWorseCount() + "," + stats.rejectedWorseCount() + "," + stats.bestFoundIteration() + "," + eliteUpdateCount + "," + lastProtectedEdgesCount + "," + minProtectedEdgesCount + "," + maxProtectedEdgesCount + "," + avgProtectedEdgesOnUpdate + "," + SWAP_EDGES + "," + COOLING + "," + T0 + "," + TMIN + "," + BREAK_PROBABILITY + "," + ARCHIVE_CAPACITY + "," + MIN_EDGE_FREQUENCY + "," + variant.vertexReseedCount() + "," + kind);
    }

    private static OptimizationAlgorithm createAlgorithm(Variant variant, long seed) {
        return new IteratedLocalSearchWithFrequencyBackboneSaAcceptance(
                variant.name(),
                startAlgorithm(variant, seed),
                new SteepestLocalSearchWithCandidateMoves(),
                TIME_LIMIT_NANOS,
                seed,
                T0,
                TMIN,
                COOLING,
                SWAP_EDGES,
                ARCHIVE_CAPACITY,
                MIN_EDGE_FREQUENCY,
                BREAK_PROBABILITY,
                false
        );
    }

    private static OptimizationAlgorithm startAlgorithm(Variant variant, long seed) {
        if (variant.baseline()) {
            return new CheapConsensusR2Top3P30Parameterized(seed, 5, 3, true, true);
        }
        return new RepairReseedCheapConsensusFastR2Top3P30Parameterized(
                variant.name() + "_START",
                RepairReseedCheapConsensusFastR2Top3P30Parameterized.Mode.VERTEX_RESEED_TOP3,
                seed,
                variant.vertexReseedCount()
        );
    }
}
