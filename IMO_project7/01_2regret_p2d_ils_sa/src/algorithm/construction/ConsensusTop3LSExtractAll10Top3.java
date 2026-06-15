package algorithm.construction;

import algorithm.OptimizationAlgorithm;
import algorithm.SolutionObjective;
import algorithm.improvement.PhaseTwoDelete;
import model.Instance;
import model.Solution;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

/**
 * Consensus constructor based on ten randomized 2-regret runs.
 *
 * <p>Pipeline:</p>
 * <ol>
 *     <li>Run 10 x Randomized2RegretTop5P20 + ordinary PhaseTwoDelete.</li>
 *     <li>Apply local search to all 10 solutions.</li>
 *     <li>Sort improved solutions by objective and take top 3.</li>
 *     <li>Extract edges common to all top 3 solutions.</li>
 *     <li>Build ordered fragments in the order of the best top-3 solution.</li>
 *     <li>Protect internal fragment edges and complete the gaps by 2-regret repair.</li>
 *     <li>Run ordinary PhaseTwoDelete and final local search.</li>
 * </ol>
 */
public final class ConsensusTop3LSExtractAll10Top3 implements OptimizationAlgorithm {
    private static final int BASE_RUNS = 10;
    private static final int ELITE_SIZE = 3;
    private static final int LOCAL_SEARCH_MAX_ITERS = 100;

    private final Random random;
    private final PhaseTwoDelete phaseTwoDelete;

    public ConsensusTop3LSExtractAll10Top3() {
        this(new Random());
    }

    public ConsensusTop3LSExtractAll10Top3(long seed) {
        this(new Random(seed));
    }

    public ConsensusTop3LSExtractAll10Top3(Random random) {
        this.random = Objects.requireNonNull(random, "random");
        this.phaseTwoDelete = new PhaseTwoDelete();
    }

    @Override
    public String name() {
        return "ConsensusTop3_LSExtractAll10_Top3";
    }

    @Override
    public Solution solve(Instance instance, int startVertexId) {
        List<ScoredSolution> base = new ArrayList<>(BASE_RUNS);

        for (int run = 0; run < BASE_RUNS; run++) {
            Randomized2RegretTop5P20 construction = new Randomized2RegretTop5P20(new Random(random.nextLong()));
            Solution constructed = construction.solve(instance, startVertexId);
            Solution cleaned = phaseTwoDelete.improve(instance, constructed);
            Solution improved = localSearchBasic(instance, cleaned, LOCAL_SEARCH_MAX_ITERS);
            int objective = SolutionObjective.calculate(instance, improved);
            base.add(new ScoredSolution(improved, objective, run));
        }

        base.sort(Comparator
                .comparingInt(ScoredSolution::objective).reversed()
                .thenComparingInt(ScoredSolution::runIndex));

        List<List<Integer>> eliteCycles = new ArrayList<>(ELITE_SIZE);
        for (int i = 0; i < Math.min(ELITE_SIZE, base.size()); i++) {
            eliteCycles.add(base.get(i).solution().cycle());
        }

        List<Integer> reference = eliteCycles.get(0);
        Set<Long> commonEdges = commonEdgesAmong(eliteCycles);
        List<List<Integer>> fragments = orderedFragmentsFromReference(reference, commonEdges);

        List<Integer> repairedCycle = completeOrderedFragments(instance, startVertexId, fragments);
        Solution repaired = new Solution(instance.name, startVertexId, repairedCycle);
        Solution cleaned = phaseTwoDelete.improve(instance, repaired);
        return localSearchBasic(instance, cleaned, LOCAL_SEARCH_MAX_ITERS);
    }

    private static Set<Long> commonEdgesAmong(List<List<Integer>> cycles) {
        Set<Long> common = null;

        for (List<Integer> cycle : cycles) {
            Set<Long> edges = edgesOf(cycle);
            if (common == null) {
                common = edges;
            } else {
                common.retainAll(edges);
            }
        }

        return common == null ? Set.of() : common;
    }

    private static Set<Long> edgesOf(List<Integer> cycle) {
        Set<Long> edges = new HashSet<>();
        int size = cycle.size();

        for (int i = 0; i < size; i++) {
            int a = cycle.get(i);
            int b = cycle.get((i + 1) % size);
            edges.add(edgeKey(a, b));
        }

        return edges;
    }

    private static List<List<Integer>> orderedFragmentsFromReference(List<Integer> reference, Set<Long> commonEdges) {
        int n = reference.size();
        List<List<Integer>> fragments = new ArrayList<>();

        if (n < 2 || commonEdges.isEmpty()) {
            return fragments;
        }

        boolean[] common = new boolean[n];
        boolean allCommon = true;
        for (int i = 0; i < n; i++) {
            int a = reference.get(i);
            int b = reference.get((i + 1) % n);
            common[i] = commonEdges.contains(edgeKey(a, b));
            allCommon &= common[i];
        }

        if (allCommon) {
            fragments.add(new ArrayList<>(reference));
            return fragments;
        }

        int start = 0;
        for (int i = 0; i < n; i++) {
            if (!common[i]) {
                start = (i + 1) % n;
                break;
            }
        }

        int steps = 0;
        while (steps < n) {
            int index = (start + steps) % n;
            if (!common[index]) {
                steps++;
                continue;
            }

            List<Integer> fragment = new ArrayList<>();
            fragment.add(reference.get(index));

            while (steps < n && common[(start + steps) % n]) {
                int edgeIndex = (start + steps) % n;
                fragment.add(reference.get((edgeIndex + 1) % n));
                steps++;
            }

            if (fragment.size() >= 2) {
                fragments.add(fragment);
            }
        }

        return fragments;
    }

    private static List<Integer> completeOrderedFragments(
            Instance instance,
            int startVertexId,
            List<List<Integer>> fragments
    ) {
        if (fragments.isEmpty()) {
            return new TwoRegretCost().solve(instance, startVertexId).cycle();
        }

        List<Integer> cycle = concatFragmentsReferenceOrder(fragments);
        boolean[] used = new boolean[instance.size];
        for (int vertex : cycle) {
            used[vertex] = true;
        }

        if (cycle.size() < 2) {
            cycle.clear();
            cycle.add(startVertexId);
            used = new boolean[instance.size];
            used[startVertexId] = true;
            int nearest = nearestUnusedVertex(instance, startVertexId, used);
            cycle.add(nearest);
            used[nearest] = true;
        }

        Set<Long> protectedEdges = protectedInternalEdges(fragments);
        List<Integer> unused = new ArrayList<>();
        for (int vertex = 0; vertex < instance.size; vertex++) {
            if (!used[vertex]) {
                unused.add(vertex);
            }
        }

        int[][] distances = instance.distanceMatrix.distances;
        int[] profits = profits(instance);

        while (!unused.isEmpty()) {
            Insertion selected = bestProtectedTwoRegretInsertion(
                    distances,
                    profits,
                    cycle,
                    unused,
                    protectedEdges
            );

            int vertex = unused.remove(selected.unusedIndex());
            cycle.add(selected.insertAfterPosition() + 1, vertex);
        }

        return cycle;
    }

    private static List<Integer> concatFragmentsReferenceOrder(List<List<Integer>> fragments) {
        List<Integer> cycle = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();

        for (List<Integer> fragment : fragments) {
            for (int vertex : fragment) {
                if (seen.add(vertex)) {
                    cycle.add(vertex);
                }
            }
        }

        return cycle;
    }

    private static Set<Long> protectedInternalEdges(List<List<Integer>> fragments) {
        Set<Long> protectedEdges = new HashSet<>();

        for (List<Integer> fragment : fragments) {
            for (int i = 0; i + 1 < fragment.size(); i++) {
                protectedEdges.add(edgeKey(fragment.get(i), fragment.get(i + 1)));
            }
        }

        return protectedEdges;
    }

    private static Insertion bestProtectedTwoRegretInsertion(
            int[][] distances,
            int[] profits,
            List<Integer> cycle,
            List<Integer> unused,
            Set<Long> protectedEdges
    ) {
        boolean hasUnprotectedEdge = hasUnprotectedEdge(cycle, protectedEdges);

        int selectedUnusedIndex = -1;
        int selectedInsertAfter = -1;
        int selectedRegret = Integer.MIN_VALUE;
        int selectedBestCost = Integer.MAX_VALUE;
        int selectedVertex = Integer.MAX_VALUE;

        for (int unusedIndex = 0; unusedIndex < unused.size(); unusedIndex++) {
            int vertex = unused.get(unusedIndex);
            int bestCost = Integer.MAX_VALUE;
            int secondBestCost = Integer.MAX_VALUE;
            int bestInsertAfter = -1;

            for (int position = 0; position < cycle.size(); position++) {
                int a = cycle.get(position);
                int b = cycle.get((position + 1) % cycle.size());

                if (hasUnprotectedEdge && protectedEdges.contains(edgeKey(a, b))) {
                    continue;
                }

                int increaseLength = distances[a][vertex] + distances[vertex][b] - distances[a][b];
                int cost = increaseLength - profits[vertex];

                if (cost < bestCost) {
                    secondBestCost = bestCost;
                    bestCost = cost;
                    bestInsertAfter = position;
                } else if (cost < secondBestCost) {
                    secondBestCost = cost;
                }
            }

            int regret = secondBestCost - bestCost;
            if (regret > selectedRegret
                    || (regret == selectedRegret && bestCost < selectedBestCost)
                    || (regret == selectedRegret && bestCost == selectedBestCost && vertex < selectedVertex)) {
                selectedUnusedIndex = unusedIndex;
                selectedInsertAfter = bestInsertAfter;
                selectedRegret = regret;
                selectedBestCost = bestCost;
                selectedVertex = vertex;
            }
        }

        return new Insertion(selectedUnusedIndex, selectedInsertAfter);
    }

    private static boolean hasUnprotectedEdge(List<Integer> cycle, Set<Long> protectedEdges) {
        for (int i = 0; i < cycle.size(); i++) {
            int a = cycle.get(i);
            int b = cycle.get((i + 1) % cycle.size());
            if (!protectedEdges.contains(edgeKey(a, b))) {
                return true;
            }
        }
        return false;
    }

    private static Solution localSearchBasic(Instance instance, Solution solution, int maxIterations) {
        List<Integer> cycle = new ArrayList<>(solution.cycle());
        localSearchBasic(instance, cycle, maxIterations);
        return new Solution(solution.instanceName(), solution.startVertexId(), cycle);
    }

    private static void localSearchBasic(Instance instance, List<Integer> cycle, int maxIterations) {
        int[][] distances = instance.distanceMatrix.distances;
        int[] profits = profits(instance);
        boolean[] selected = new boolean[instance.size];
        for (int vertex : cycle) {
            selected[vertex] = true;
        }

        for (int iteration = 0; iteration < maxIterations; iteration++) {
            Move bestMove = findBestLocalSearchMove(distances, profits, cycle, selected);
            if (bestMove == null || bestMove.gain() <= 0) {
                return;
            }
            applyMove(cycle, selected, bestMove);
        }
    }

    private static Move findBestLocalSearchMove(
            int[][] distances,
            int[] profits,
            List<Integer> cycle,
            boolean[] selected
    ) {
        int size = cycle.size();
        Move bestMove = null;
        int bestGain = 0;

        if (size > 2) {
            for (int position = 0; position < size; position++) {
                int vertex = cycle.get(position);
                int previous = cycle.get(position == 0 ? size - 1 : position - 1);
                int next = cycle.get(position + 1 == size ? 0 : position + 1);
                int gain = distances[previous][vertex] + distances[vertex][next]
                        - distances[previous][next] - profits[vertex];
                if (gain > bestGain) {
                    bestGain = gain;
                    bestMove = Move.delete(gain, position);
                }
            }
        }

        for (int vertex = 0; vertex < selected.length; vertex++) {
            if (selected[vertex]) {
                continue;
            }

            for (int edgePosition = 0; edgePosition < size; edgePosition++) {
                int a = cycle.get(edgePosition);
                int b = cycle.get((edgePosition + 1) % size);
                int gain = profits[vertex] - (distances[a][vertex] + distances[vertex][b] - distances[a][b]);
                if (gain > bestGain) {
                    bestGain = gain;
                    bestMove = Move.insert(gain, vertex, edgePosition);
                }
            }

            for (int position = 0; position < size; position++) {
                int oldVertex = cycle.get(position);
                int previous = cycle.get(position == 0 ? size - 1 : position - 1);
                int next = cycle.get(position + 1 == size ? 0 : position + 1);
                int gain = profits[vertex] - profits[oldVertex]
                        - (distances[previous][vertex] + distances[vertex][next]
                        - distances[previous][oldVertex] - distances[oldVertex][next]);
                if (gain > bestGain) {
                    bestGain = gain;
                    bestMove = Move.replace(gain, vertex, position);
                }
            }
        }

        if (size >= 4) {
            for (int i = 0; i < size; i++) {
                int iNext = (i + 1) % size;
                for (int j = i + 2; j < size; j++) {
                    if (i == 0 && j == size - 1) {
                        continue;
                    }
                    int jNext = (j + 1) % size;
                    int a = cycle.get(i);
                    int b = cycle.get(iNext);
                    int c = cycle.get(j);
                    int d = cycle.get(jNext);
                    int gain = distances[a][b] + distances[c][d] - distances[a][c] - distances[b][d];
                    if (gain > bestGain) {
                        bestGain = gain;
                        bestMove = Move.twoOpt(gain, i, j);
                    }
                }
            }
        }

        return bestMove;
    }

    private static void applyMove(List<Integer> cycle, boolean[] selected, Move move) {
        switch (move.type()) {
            case DELETE -> {
                int vertex = cycle.remove(move.position());
                selected[vertex] = false;
            }
            case INSERT -> {
                cycle.add(move.position() + 1, move.vertex());
                selected[move.vertex()] = true;
            }
            case REPLACE -> {
                int oldVertex = cycle.set(move.position(), move.vertex());
                selected[oldVertex] = false;
                selected[move.vertex()] = true;
            }
            case TWO_OPT -> reverse(cycle, move.left() + 1, move.right());
        }
    }

    private static void reverse(List<Integer> cycle, int left, int right) {
        while (left < right) {
            int tmp = cycle.get(left);
            cycle.set(left, cycle.get(right));
            cycle.set(right, tmp);
            left++;
            right--;
        }
    }

    private static int nearestUnusedVertex(Instance instance, int startVertex, boolean[] used) {
        int bestVertex = -1;
        int bestDistance = Integer.MAX_VALUE;

        for (int vertex = 0; vertex < instance.size; vertex++) {
            if (!used[vertex] && instance.distanceMatrix.distances[startVertex][vertex] < bestDistance) {
                bestDistance = instance.distanceMatrix.distances[startVertex][vertex];
                bestVertex = vertex;
            }
        }

        return bestVertex;
    }

    private static int[] profits(Instance instance) {
        int[] profits = new int[instance.size];
        for (int vertex = 0; vertex < instance.size; vertex++) {
            profits[vertex] = instance.vertices[vertex].profit;
        }
        return profits;
    }

    private static long edgeKey(int a, int b) {
        int u = Math.min(a, b);
        int v = Math.max(a, b);
        return ((long) u << 32) | (v & 0xffffffffL);
    }

    private record ScoredSolution(Solution solution, int objective, int runIndex) {
    }

    private record Insertion(int unusedIndex, int insertAfterPosition) {
    }

    private enum MoveType {
        DELETE,
        INSERT,
        REPLACE,
        TWO_OPT
    }

    private record Move(MoveType type, int gain, int vertex, int position, int left, int right) {
        static Move delete(int gain, int position) {
            return new Move(MoveType.DELETE, gain, -1, position, -1, -1);
        }

        static Move insert(int gain, int vertex, int edgePosition) {
            return new Move(MoveType.INSERT, gain, vertex, edgePosition, -1, -1);
        }

        static Move replace(int gain, int vertex, int position) {
            return new Move(MoveType.REPLACE, gain, vertex, position, -1, -1);
        }

        static Move twoOpt(int gain, int left, int right) {
            return new Move(MoveType.TWO_OPT, gain, -1, -1, left, right);
        }
    }
}
