package faircomparison;

import model.Instance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Deterministic run-plan builder: one shuffled pass over all vertices, with one seed per run. */
public final class FairRunPlan {
    private FairRunPlan() {
    }

    public static List<FairRunConfig> allVertices(Instance instance, long baseSeed) {
        long instanceSeed = baseSeed ^ (long) instance.name.hashCode();
        Random master = new Random(instanceSeed);

        List<Integer> startVertexIds = new ArrayList<>(instance.size);
        for (int vertexId = 0; vertexId < instance.size; vertexId++) {
            startVertexIds.add(vertexId);
        }
        Collections.shuffle(startVertexIds, master);

        List<FairRunConfig> runs = new ArrayList<>(instance.size);
        for (int runIndex = 0; runIndex < instance.size; runIndex++) {
            runs.add(new FairRunConfig(runIndex, startVertexIds.get(runIndex), master.nextLong()));
        }
        return runs;
    }

    public static List<FairRunConfig> firstNVertices(Instance instance, long baseSeed, int runsCount) {
        List<FairRunConfig> all = allVertices(instance, baseSeed);
        if (runsCount >= all.size()) {
            return all;
        }
        return new ArrayList<>(all.subList(0, runsCount));
    }
}
