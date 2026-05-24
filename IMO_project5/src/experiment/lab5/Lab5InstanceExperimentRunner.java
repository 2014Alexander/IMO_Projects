package experiment.lab5;

import algorithm.OptimizationAlgorithm;
import algorithm.similarity.AverageSimilarityToOthers;
import algorithm.similarity.CommonEdgesSimilarity;
import algorithm.similarity.CommonVerticesSimilarity;
import algorithm.similarity.SolutionFeatures;
import algorithm.similarity.SolutionFeaturesBuilder;
import evaluation.SolutionMetrics;
import experiment.core.RunConfig;
import experiment.core.RunPreparator;
import experiment.core.TestedAlgorithm;
import experiment.execution.AlgorithmExecutor;
import experiment.execution.ExecutionResult;
import experiment.lab4.Lab4Scenario;
import experiment.summary.AlgorithmExperimentSummary;
import experiment.summary.ExecutionResultsSummarizer;
import model.Instance;

import java.util.ArrayList;
import java.util.List;

public final class Lab5InstanceExperimentRunner {
    private final long baseSeed;
    private final Lab5Scenario scenario;
    private final AlgorithmExecutor algorithmExecutor;
    private final SolutionFeaturesBuilder featuresBuilder;
    private final CommonVerticesSimilarity commonVerticesSimilarity;
    private final CommonEdgesSimilarity commonEdgesSimilarity;
    private final AverageSimilarityToOthers averageSimilarityToOthers;

    public Lab5InstanceExperimentRunner(long baseSeed, Lab5Scenario scenario) {
        this.baseSeed = baseSeed;
        this.scenario = scenario;
        this.algorithmExecutor = new AlgorithmExecutor();
        this.featuresBuilder = new SolutionFeaturesBuilder();
        this.commonVerticesSimilarity = new CommonVerticesSimilarity();
        this.commonEdgesSimilarity = new CommonEdgesSimilarity();
        this.averageSimilarityToOthers = new AverageSimilarityToOthers();
    }

    public Lab5InstanceResult run(Instance instance) {
        SelectedBestSolution bestSolution = selectBestSolution(instance);
        printBestSolutionSelection(instance, bestSolution);
        List<RunConfig> runs = RunPreparator.prepareRuns(instance, baseSeed, scenario.localOptimaCount());
        List<EvaluatedRun> localOptima = runLocalOptima(instance, runs);
        printLocalOptimaGenerated(instance, localOptima.size());

        SolutionFeatures[] localFeatures = buildLocalFeatures(instance, localOptima);
        SolutionFeatures bestFeatures = featuresBuilder.build(instance, bestSolution.result().solution());

        double[] avgVerticesToOthers = averageSimilarityToOthers.calculate(localFeatures, commonVerticesSimilarity);
        double[] avgEdgesToOthers = averageSimilarityToOthers.calculate(localFeatures, commonEdgesSimilarity);

        List<Lab5Point> points = buildPoints(
            localOptima,
            localFeatures,
            bestFeatures,
            avgVerticesToOthers,
            avgEdgesToOthers
        );

        Lab5CorrelationSummary correlations = calculateCorrelations(points);
        printCompleted(instance);

        return new Lab5InstanceResult(
            instance.name,
            scenario.localOptimaCount(),
            scenario.localSearchName(),
            scenario.neighborhoodName(),
            new Lab5BestSolutionMetadata(
                bestSolution.algorithmName(),
                bestSolution.result().solutionMetrics().objective(),
                bestSolution.run().startVertexId(),
                bestSolution.run().runSeed(),
                bestSolution.selectionPoolAlgorithms()
            ),
            points,
            correlations
        );
    }

    private SelectedBestSolution selectBestSolution(Instance instance) {
        Lab4Scenario lab4Scenario = scenario.bestSolutionScenario();
        List<RunConfig> runs = RunPreparator.prepareRuns(instance, baseSeed, lab4Scenario.runsCount());

        TestedAlgorithm mslsAlgorithm = lab4Scenario.mslsAlgorithm();
        List<EvaluatedRun> mslsResults = runAlgorithmOnSharedRuns(instance, runs, mslsAlgorithm);
        AlgorithmExperimentSummary mslsSummary = ExecutionResultsSummarizer.summarize(extractResults(mslsResults));
        long timedAlgorithmsLimitNanos = Math.round(mslsSummary.runtimeStatistics().avgRuntimeNanos());

        EvaluatedRun best = bestOf(mslsResults);
        for (TestedAlgorithm testedAlgorithm : lab4Scenario.timedAlgorithms(timedAlgorithmsLimitNanos)) {
            EvaluatedRun candidate = bestOf(runAlgorithmOnSharedRuns(instance, runs, testedAlgorithm));
            if (candidate.result().solutionMetrics().objective() > best.result().solutionMetrics().objective()) {
                best = candidate;
            }
        }

        return new SelectedBestSolution(
            best.run(),
            best.algorithmName(),
            best.result(),
            scenario.bestSolutionSelectionPoolAlgorithms(timedAlgorithmsLimitNanos)
        );
    }

    private List<EvaluatedRun> runLocalOptima(Instance instance, List<RunConfig> runs) {
        return runAlgorithmOnSharedRuns(instance, runs, scenario.localOptimaAlgorithm());
    }

    private List<EvaluatedRun> runAlgorithmOnSharedRuns(
        Instance instance,
        List<RunConfig> runs,
        TestedAlgorithm testedAlgorithm
    ) {
        List<EvaluatedRun> results = new ArrayList<>(runs.size());

        for (RunConfig run : runs) {
            results.add(executeAlgorithm(instance, run, testedAlgorithm));
        }

        return results;
    }

    private EvaluatedRun executeAlgorithm(
        Instance instance,
        RunConfig run,
        TestedAlgorithm testedAlgorithm
    ) {
        OptimizationAlgorithm algorithm = testedAlgorithm.create(run);
        ExecutionResult result = algorithmExecutor.execute(
            instance,
            run.startVertexId(),
            testedAlgorithm.name(),
            algorithm
        );

        return new EvaluatedRun(run, testedAlgorithm.name(), result);
    }

    private SolutionFeatures[] buildLocalFeatures(Instance instance, List<EvaluatedRun> localOptima) {
        SolutionFeatures[] features = new SolutionFeatures[localOptima.size()];

        for (int index = 0; index < localOptima.size(); index++) {
            features[index] = featuresBuilder.build(instance, localOptima.get(index).result().solution());
        }

        return features;
    }

    private List<Lab5Point> buildPoints(
        List<EvaluatedRun> localOptima,
        SolutionFeatures[] localFeatures,
        SolutionFeatures bestFeatures,
        double[] avgVerticesToOthers,
        double[] avgEdgesToOthers
    ) {
        List<Lab5Point> points = new ArrayList<>(localOptima.size());

        for (int index = 0; index < localOptima.size(); index++) {
            EvaluatedRun evaluatedRun = localOptima.get(index);
            SolutionMetrics metrics = evaluatedRun.result().solutionMetrics();

            points.add(new Lab5Point(
                index,
                evaluatedRun.run().startVertexId(),
                evaluatedRun.run().runSeed(),
                metrics.objective(),
                commonVerticesSimilarity.calculate(localFeatures[index], bestFeatures),
                commonEdgesSimilarity.calculate(localFeatures[index], bestFeatures),
                avgVerticesToOthers[index],
                avgEdgesToOthers[index]
            ));
        }

        return List.copyOf(points);
    }

    private static Lab5CorrelationSummary calculateCorrelations(List<Lab5Point> points) {
        double[] objectives = new double[points.size()];
        double[] verticesToBest = new double[points.size()];
        double[] edgesToBest = new double[points.size()];
        double[] avgVerticesToOthers = new double[points.size()];
        double[] avgEdgesToOthers = new double[points.size()];

        for (int index = 0; index < points.size(); index++) {
            Lab5Point point = points.get(index);
            objectives[index] = point.objective();
            verticesToBest[index] = point.similarityVerticesToBest();
            edgesToBest[index] = point.similarityEdgesToBest();
            avgVerticesToOthers[index] = point.avgSimilarityVerticesToOthers();
            avgEdgesToOthers[index] = point.avgSimilarityEdgesToOthers();
        }

        return new Lab5CorrelationSummary(
            pearsonCorrelation(objectives, verticesToBest),
            pearsonCorrelation(objectives, edgesToBest),
            pearsonCorrelation(objectives, avgVerticesToOthers),
            pearsonCorrelation(objectives, avgEdgesToOthers)
        );
    }

    private static double pearsonCorrelation(double[] first, double[] second) {
        if (first.length != second.length || first.length == 0) {
            throw new IllegalArgumentException("Arrays must have the same positive length");
        }

        double firstMean = mean(first);
        double secondMean = mean(second);
        double numerator = 0.0;
        double firstSquares = 0.0;
        double secondSquares = 0.0;

        for (int index = 0; index < first.length; index++) {
            double firstCentered = first[index] - firstMean;
            double secondCentered = second[index] - secondMean;
            numerator += firstCentered * secondCentered;
            firstSquares += firstCentered * firstCentered;
            secondSquares += secondCentered * secondCentered;
        }

        double denominator = Math.sqrt(firstSquares * secondSquares);
        if (denominator == 0.0) {
            return Double.NaN;
        }

        return numerator / denominator;
    }

    private static double mean(double[] values) {
        double sum = 0.0;
        for (double value : values) {
            sum += value;
        }
        return sum / values.length;
    }

    private static List<ExecutionResult> extractResults(List<EvaluatedRun> runs) {
        List<ExecutionResult> results = new ArrayList<>(runs.size());
        for (EvaluatedRun run : runs) {
            results.add(run.result());
        }
        return List.copyOf(results);
    }

    private static EvaluatedRun bestOf(List<EvaluatedRun> runs) {
        EvaluatedRun best = runs.get(0);
        for (int index = 1; index < runs.size(); index++) {
            EvaluatedRun candidate = runs.get(index);
            if (candidate.result().solutionMetrics().objective() > best.result().solutionMetrics().objective()) {
                best = candidate;
            }
        }
        return best;
    }

    private static void printBestSolutionSelection(Instance instance, SelectedBestSolution bestSolution) {
        System.out.println(
            "Wybrano rozwiazanie B dla instancji "
                + instance.name
                + ": "
                + bestSolution.algorithmName()
                + " objective="
                + bestSolution.result().solutionMetrics().objective()
        );
    }

    private static void printLocalOptimaGenerated(Instance instance, int count) {
        System.out.println(
            "Wygenerowano local optima dla instancji "
                + instance.name
                + ": "
                + count
        );
    }

    private static void printCompleted(Instance instance) {
        System.out.println(
            "Zakonczono eksperyment lab5 dla instancji "
                + instance.name
        );
    }

    private record EvaluatedRun(RunConfig run, String algorithmName, ExecutionResult result) {
    }

    private record SelectedBestSolution(
        RunConfig run,
        String algorithmName,
        ExecutionResult result,
        List<String> selectionPoolAlgorithms
    ) {
    }
}
