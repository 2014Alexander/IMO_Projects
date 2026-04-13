package model;
import algorithm.localsearch.Cycle;

import java.util.ArrayList;
import java.util.List;

public record Solution(String instanceName, int startVertexId, List<Integer> cycle) {

    public Solution {
        cycle = List.copyOf(cycle);
    }

    public Solution(String instanceName, int startVertexId, Cycle cycle) {
        this(instanceName, startVertexId, toList(cycle));
    }

    private static List<Integer> toList(Cycle cycle) {
        ArrayList<Integer> result = new ArrayList<>(cycle.size());
        for (int i = 0; i < cycle.size(); i++) {
            result.add(cycle.cycle[i]);
        }
        return result;
    }
}
