package algorithm.metaheuristic;

public interface SaAcceptanceStatistics {
    int acceptedBetterCount();
    int acceptedWorseCount();
    int rejectedWorseCount();
    int bestFoundIteration();
}
