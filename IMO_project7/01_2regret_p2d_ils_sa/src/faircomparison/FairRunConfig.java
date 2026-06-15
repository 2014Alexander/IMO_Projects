package faircomparison;

/** One independent run configuration shared by every tested algorithm in the same run slot. */
public record FairRunConfig(int runIndex, int startVertexId, long runSeed) {
}
