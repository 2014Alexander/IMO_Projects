package model;

import java.util.List;

public record Solution(String instanceName, int startVertexId, List<Integer> cycle) {

    public Solution {
        cycle = List.copyOf(cycle);
    }
}
