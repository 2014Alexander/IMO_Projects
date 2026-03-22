package experiment.run;

import algorithm.construction.singlephase.RandomSolution;
import algorithm.construction.singlephase.TwoRegretCost;
import algorithm.construction.singlephase.WeightedTwoRegretCost;
import algorithm.construction.twophase.GreedyCycle;
import algorithm.construction.twophase.GreedyCycleWithoutProfit;
import algorithm.construction.twophase.NearestNeighbor;
import algorithm.construction.twophase.NearestNeighborWithoutProfit;
import algorithm.improvement.PhaseTwoDelete;
import experiment.summary.SinglePhaseExperimentSummary;
import experiment.summary.TwoPhaseExperimentSummary;
import experiment.build.SinglePhaseExperimentSummaryBuilder;
import experiment.build.TwoPhaseExperimentSummaryBuilder;
import experiment.summary.InstanceExperimentResult;
import model.Instance;

import java.util.ArrayList;
import java.util.List;

public final class InstanceExperimentRunner {

    private final SinglePhaseExperimentSummaryBuilder singlePhaseSummaryBuilder;
    private final TwoPhaseExperimentSummaryBuilder twoPhaseSummaryBuilder;
    private final PhaseTwoDelete phaseTwoDelete;

    public InstanceExperimentRunner() {
        this.singlePhaseSummaryBuilder = new SinglePhaseExperimentSummaryBuilder();
        this.twoPhaseSummaryBuilder = new TwoPhaseExperimentSummaryBuilder();
        this.phaseTwoDelete = new PhaseTwoDelete();
    }

    public InstanceExperimentResult run(Instance instance) {
        if (instance == null) {
            throw new IllegalArgumentException("instance cannot be null");
        }

        List<SinglePhaseExperimentSummary> singlePhaseSummaries =
                buildSinglePhaseSummaries(instance);

        List<TwoPhaseExperimentSummary> twoPhaseSummaries =
                buildTwoPhaseSummaries(instance);

        return new InstanceExperimentResult(
                instance.name,
                singlePhaseSummaries,
                twoPhaseSummaries
        );
    }

    private List<SinglePhaseExperimentSummary> buildSinglePhaseSummaries(Instance instance) {
        List<SinglePhaseExperimentSummary> summaries = new ArrayList<>();

        summaries.add(
                singlePhaseSummaryBuilder.build(instance, "Random", new RandomSolution())
        );
        summaries.add(
                singlePhaseSummaryBuilder.build(instance, "2-Regret", new TwoRegretCost())
        );
        summaries.add(
                singlePhaseSummaryBuilder.build(instance, "W. 2-Regret", new WeightedTwoRegretCost())
        );

        return summaries;
    }

    private List<TwoPhaseExperimentSummary> buildTwoPhaseSummaries(Instance instance) {
        List<TwoPhaseExperimentSummary> summaries = new ArrayList<>();

        summaries.add(
                twoPhaseSummaryBuilder.build(
                        instance,
                        "NN no prof.",
                        new NearestNeighborWithoutProfit(),
                        phaseTwoDelete
                )
        );
        summaries.add(
                twoPhaseSummaryBuilder.build(
                        instance,
                        "NN + profit",
                        new NearestNeighbor(),
                        phaseTwoDelete
                )
        );
        summaries.add(
                twoPhaseSummaryBuilder.build(
                        instance,
                        "GC no prof.",
                        new GreedyCycleWithoutProfit(),
                        phaseTwoDelete
                )
        );
        summaries.add(
                twoPhaseSummaryBuilder.build(
                        instance,
                        "GC + profit",
                        new GreedyCycle(),
                        phaseTwoDelete
                )
        );

        return summaries;
    }
}