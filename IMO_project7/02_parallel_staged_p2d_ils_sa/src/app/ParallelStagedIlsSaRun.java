package app;

import algorithm.IterationCountingAlgorithm;
import algorithm.OptimizationAlgorithm;
import algorithm.metaheuristic.SaAcceptanceStatistics;
import evaluation.SolutionEvaluator;
import evaluation.SolutionMetrics;
import faircomparison.FairRunConfig;
import faircomparison.FairRunConfigCsv;
import faircomparison.FairRunPlan;
import faircomparison.ParallelStagedIlsSaDefinition;
import io.CsvInstanceReader;
import model.Instance;
import model.Solution;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Uczciwy test interleaved: dla każdego run-config uruchamiane są kolejno
 * warianty 1/2/4/8 wątków na tej samej instancji, startVertexId i runSeed.
 */
public final class ParallelStagedIlsSaRun {
    private static final double DEFAULT_TIME_LIMIT_SECONDS = 0.5;
    private static final long DEFAULT_BASE_SEED = 20260614L;
    private static final String DEFAULT_CONFIG_DIR = "configs";

    public static void main(String[] args) throws Exception {
        double timeLimitSeconds = args.length >= 1 ? Double.parseDouble(args[0]) : DEFAULT_TIME_LIMIT_SECONDS;
        int requestedRunsCount = args.length >= 2 ? Integer.parseInt(args[1]) : 200;
        long baseSeed = args.length >= 3 ? Long.parseLong(args[2]) : DEFAULT_BASE_SEED;
        String configDir = args.length >= 4 ? args[3] : DEFAULT_CONFIG_DIR;
        String[] instancePaths = args.length >= 5 ? instancePaths(args, 4) : new String[] {"data/TSPA.csv", "data/TSPB.csv"};

        long timeLimitNanos = Math.round(timeLimitSeconds * 1_000_000_000.0);
        List<ParallelStagedIlsSaDefinition> algorithms = ParallelStagedIlsSaDefinition.defaultAlgorithms();
        SolutionEvaluator evaluator = new SolutionEvaluator();

        printHeader();

        for (String instancePath : instancePaths) {
            Instance instance = new CsvInstanceReader().read(instancePath);
            List<FairRunConfig> configs = loadOrCreateConfigs(instance, baseSeed, requestedRunsCount, configDir);
            if (requestedRunsCount < configs.size()) {
                configs = configs.subList(0, requestedRunsCount);
            }

            for (FairRunConfig config : configs) {
                for (int algorithmIndex = 0; algorithmIndex < algorithms.size(); algorithmIndex++) {
                    ParallelStagedIlsSaDefinition definition = algorithms.get(algorithmIndex);
                    OptimizationAlgorithm algorithm = definition.factory().create(config.runSeed(), timeLimitNanos);

                    long start = System.nanoTime();
                    Solution solution = algorithm.solve(instance, config.startVertexId());
                    long runtimeNanos = System.nanoTime() - start;

                    SolutionMetrics metrics = evaluator.evaluate(instance, solution);
                    int iterations = algorithm instanceof IterationCountingAlgorithm counting
                            ? counting.lastIterationCount()
                            : -1;
                    int acceptedBetter = algorithm instanceof SaAcceptanceStatistics stats ? stats.acceptedBetterCount() : -1;
                    int acceptedWorse = algorithm instanceof SaAcceptanceStatistics stats ? stats.acceptedWorseCount() : -1;
                    int rejectedWorse = algorithm instanceof SaAcceptanceStatistics stats ? stats.rejectedWorseCount() : -1;
                    int bestFoundIteration = algorithm instanceof SaAcceptanceStatistics stats ? stats.bestFoundIteration() : -1;

                    System.out.println(instance.name
                            + "," + config.runIndex()
                            + "," + config.startVertexId()
                            + "," + config.runSeed()
                            + "," + algorithmIndex
                            + "," + definition.name()
                            + "," + metrics.objective()
                            + "," + metrics.profitSum()
                            + "," + metrics.tourLength()
                            + "," + runtimeNanos
                            + "," + iterations
                            + "," + acceptedBetter
                            + "," + acceptedWorse
                            + "," + rejectedWorse
                            + "," + bestFoundIteration
                            + "," + timeLimitSeconds);
                    System.out.flush();
                }
            }
        }
    }

    private static List<FairRunConfig> loadOrCreateConfigs(Instance instance, long baseSeed, int requestedRunsCount, String configDir) throws Exception {
        Path configPath = Path.of(configDir, instance.name + "_run_configs_" + instance.size + ".csv");
        if (Files.exists(configPath)) {
            return FairRunConfigCsv.read(configPath);
        }
        if (requestedRunsCount >= instance.size) {
            return FairRunPlan.allVertices(instance, baseSeed);
        }
        return FairRunPlan.firstNVertices(instance, baseSeed, requestedRunsCount);
    }

    private static void printHeader() {
        System.out.println("instance,runIndex,startVertexId,runSeed,algorithmIndex,algorithm,objective,profitSum,tourLength,runtimeNanos,iterationCount,acceptedBetterCount,acceptedWorseCount,rejectedWorseCount,bestFoundIteration,timeLimitSeconds");
    }

    private static String[] instancePaths(String[] args, int offset) {
        String[] paths = new String[args.length - offset];
        System.arraycopy(args, offset, paths, 0, paths.length);
        return paths;
    }
}
