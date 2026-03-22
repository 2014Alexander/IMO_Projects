package model;

import java.util.List;
import java.util.Objects;

public record Solution(String instanceName, Integer startVertexId, List<Integer> cycle) {
    public Solution {
        Objects.requireNonNull(instanceName);
        Objects.requireNonNull(startVertexId);
        Objects.requireNonNull(cycle);

        cycle = List.copyOf(cycle);
    }
}