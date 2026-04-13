package experiment.run;

import model.Instance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class RunPreparator {

    private RunPreparator() {
    }

    public static List<RunConfig> prepareRuns(
            Instance instance,
            long baseSeed,
            int runsCount
    ) {
        long instanceSeed = createInstanceSeed(instance, baseSeed);
        Random master = new Random(instanceSeed);

        List<Integer> startVertexIds = new ArrayList<>(instance.size);
        for (int v = 0; v < instance.size; v++) {
            startVertexIds.add(v);
        }

        Collections.shuffle(startVertexIds, master);

        List<RunConfig> runs = new ArrayList<>(runsCount);
        for (int i = 0; i < runsCount; i++) {
            int startVertexId = startVertexIds.get(i);
            long runSeed = master.nextLong();
            runs.add(new RunConfig(startVertexId, runSeed));
        }

        return runs;
    }

    private static long createInstanceSeed(Instance instance, long baseSeed) {
        return baseSeed ^ (long) instance.name.hashCode();
    }
}
