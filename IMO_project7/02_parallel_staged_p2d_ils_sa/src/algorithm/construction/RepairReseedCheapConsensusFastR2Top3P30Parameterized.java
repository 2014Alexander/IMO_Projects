package algorithm.construction;

import algorithm.OptimizationAlgorithm;
import algorithm.SolutionObjective;
import algorithm.improvement.PhaseTwoDelete;
import model.Instance;
import model.Solution;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

/**
 * Experimental cheap-consensus variants around FAST_R2_TOP3P30.
 *
 * Variants:
 * - ALL_REPAIR_PASS: every base candidate receives a second randomized 2-regret repair pass after P2D.
 * - TOP3_REPAIR_PASS_ADD: only preliminary top3 get an additional repaired copy added to the pool.
 * - VERTEX_RESEED_TOP3: choose seed vertices from preliminary top3 by frequency/local contribution and run FAST_R2 from them.
 * - FRAGMENT_RESEED_BEST: choose the best common fragment of preliminary top3, close it as a seed cycle and repair it.
 */
public final class RepairReseedCheapConsensusFastR2Top3P30Parameterized implements OptimizationAlgorithm {
    private static final int BASE_RUNS = 10;
    private static final int LS_CANDIDATES = 5;
    private static final int ELITE_SIZE = 3;
    private static final int LOCAL_SEARCH_MAX_ITERS = 100;
    private static final int TOP_K = 3;
    private static final double EXPLORATION_PROBABILITY = 0.30;

    private final String name;
    private final Mode mode;
    private final Random random;
    private final PhaseTwoDelete phaseTwoDelete;
    private final int vertexReseedCount;

    public enum Mode {
        ALL_REPAIR_PASS,
        TOP3_REPAIR_PASS_ADD,
        VERTEX_RESEED_TOP3,
        FRAGMENT_RESEED_BEST
    }

    public RepairReseedCheapConsensusFastR2Top3P30Parameterized(String name, Mode mode, long seed) {
        this(name, mode, seed, 3);
    }

    public RepairReseedCheapConsensusFastR2Top3P30Parameterized(String name, Mode mode, long seed, int vertexReseedCount) {
        this.name = Objects.requireNonNull(name, "name");
        this.mode = Objects.requireNonNull(mode, "mode");
        this.random = new Random(seed);
        this.phaseTwoDelete = new PhaseTwoDelete();
        if (vertexReseedCount < 0) {
            throw new IllegalArgumentException("vertexReseedCount must be non-negative");
        }
        this.vertexReseedCount = vertexReseedCount;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Solution solve(Instance instance, int startVertexId) {
        List<ScoredSolution> base = new ArrayList<>(BASE_RUNS + 4);

        for (int run = 0; run < BASE_RUNS; run++) {
            OptimizationAlgorithm construction = new FastRandomized2RegretParameterizedWithPhaseTwoDelete(
                    name + "_FAST_R2_try" + run,
                    TOP_K,
                    EXPLORATION_PROBABILITY,
                    new Random(random.nextLong())
            );
            Solution candidate = construction.solve(instance, startVertexId);
            if (mode == Mode.ALL_REPAIR_PASS) {
                candidate = repairPassWithPhaseTwoDelete(instance, candidate, new Random(random.nextLong()));
            }
            base.add(new ScoredSolution(candidate, SolutionObjective.calculate(instance, candidate), run));
        }

        base.sort(Comparator
                .comparingInt(ScoredSolution::objective).reversed()
                .thenComparingInt(ScoredSolution::runIndex));

        switch (mode) {
            case TOP3_REPAIR_PASS_ADD -> addRepairPassForTop3(instance, base);
            case VERTEX_RESEED_TOP3 -> addVertexReseedCandidates(instance, startVertexId, base);
            case FRAGMENT_RESEED_BEST -> addFragmentReseedCandidate(instance, startVertexId, base);
            case ALL_REPAIR_PASS -> {
                // Already applied to every base candidate above.
            }
        }

        base.sort(Comparator
                .comparingInt(ScoredSolution::objective).reversed()
                .thenComparingInt(ScoredSolution::runIndex));

        List<ScoredSolution> improved = new ArrayList<>(LS_CANDIDATES);
        for (int i = 0; i < Math.min(LS_CANDIDATES, base.size()); i++) {
            ScoredSolution item = base.get(i);
            Solution lsSolution = localSearchBasic(instance, item.solution(), LOCAL_SEARCH_MAX_ITERS);
            improved.add(new ScoredSolution(lsSolution, SolutionObjective.calculate(instance, lsSolution), item.runIndex()));
        }

        improved.sort(Comparator
                .comparingInt(ScoredSolution::objective).reversed()
                .thenComparingInt(ScoredSolution::runIndex));

        List<List<Integer>> eliteCycles = new ArrayList<>(ELITE_SIZE);
        for (int i = 0; i < Math.min(ELITE_SIZE, improved.size()); i++) {
            eliteCycles.add(improved.get(i).solution().cycle());
        }

        if (eliteCycles.isEmpty()) {
            return new FastRandomized2RegretParameterizedWithPhaseTwoDelete(
                    name + "_fallback", TOP_K, EXPLORATION_PROBABILITY, new Random(random.nextLong())
            ).solve(instance, startVertexId);
        }

        List<Integer> reference = eliteCycles.get(0);
        Set<Long> commonEdges = commonEdgesAmong(eliteCycles);
        List<List<Integer>> fragments = orderedFragmentsFromReference(reference, commonEdges);

        List<Integer> repairedCycle = completeOrderedFragments(instance, startVertexId, fragments);
        Solution repaired = new Solution(instance.name, startVertexId, repairedCycle);
        Solution cleaned = phaseTwoDelete.improve(instance, repaired);
        Solution finalSolution = localSearchBasic(instance, cleaned, LOCAL_SEARCH_MAX_ITERS);

        // Best parent fallback, as in current baseline.
        Solution bestParent = improved.get(0).solution();
        int bestParentObjective = SolutionObjective.calculate(instance, bestParent);
        int finalObjective = SolutionObjective.calculate(instance, finalSolution);
        if (bestParentObjective > finalObjective) {
            return bestParent;
        }

        return finalSolution;
    }

    private void addRepairPassForTop3(Instance instance, List<ScoredSolution> base) {
        int limit = Math.min(3, base.size());
        for (int i = 0; i < limit; i++) {
            Solution repaired = repairPassWithPhaseTwoDelete(instance, base.get(i).solution(), new Random(random.nextLong()));
            base.add(new ScoredSolution(repaired, SolutionObjective.calculate(instance, repaired), 1000 + i));
        }
    }

    private void addVertexReseedCandidates(Instance instance, int startVertexId, List<ScoredSolution> base) {
        List<List<Integer>> topCycles = topCycles(base, 3);
        List<Integer> seeds = bestSeedVertices(instance, topCycles, vertexReseedCount);
        int extraIndex = 0;
        for (int seedVertex : seeds) {
            OptimizationAlgorithm construction = new FastRandomized2RegretParameterizedWithPhaseTwoDelete(
                    name + "_vertex_reseed_" + seedVertex,
                    TOP_K,
                    EXPLORATION_PROBABILITY,
                    new Random(random.nextLong())
            );
            Solution reseeded = construction.solve(instance, seedVertex);
            // Preserve original startVertex in metadata, so the run remains comparable.
            reseeded = new Solution(instance.name, startVertexId, reseeded.cycle());
            base.add(new ScoredSolution(reseeded, SolutionObjective.calculate(instance, reseeded), 2000 + extraIndex));
            extraIndex++;
        }
    }

    private void addFragmentReseedCandidate(Instance instance, int startVertexId, List<ScoredSolution> base) {
        List<List<Integer>> topCycles = topCycles(base, 3);
        if (topCycles.size() < 2) {
            return;
        }
        Set<Long> commonEdges = commonEdgesAmong(topCycles);
        List<List<Integer>> fragments = orderedFragmentsFromReference(topCycles.get(0), commonEdges);
        List<Integer> bestFragment = bestFragment(instance, fragments);
        if (bestFragment.isEmpty()) {
            return;
        }
        Solution seed = new Solution(instance.name, startVertexId, bestFragment);
        Solution repaired = repairPassWithPhaseTwoDelete(instance, seed, new Random(random.nextLong()));
        base.add(new ScoredSolution(repaired, SolutionObjective.calculate(instance, repaired), 3000));
    }

    private static List<List<Integer>> topCycles(List<ScoredSolution> scored, int count) {
        List<List<Integer>> top = new ArrayList<>(Math.min(count, scored.size()));
        for (int i = 0; i < Math.min(count, scored.size()); i++) {
            top.add(scored.get(i).solution().cycle());
        }
        return top;
    }

    private Solution repairPassWithPhaseTwoDelete(Instance instance, Solution seedSolution, Random repairRandom) {
        List<Integer> cycle = new ArrayList<>(seedSolution.cycle());
        boolean[] used = new boolean[instance.size];
        List<Integer> cleanCycle = new ArrayList<>(cycle.size());
        for (int vertex : cycle) {
            if (!used[vertex]) {
                used[vertex] = true;
                cleanCycle.add(vertex);
            }
        }
        cycle = cleanCycle;

        if (cycle.size() < 2) {
            int start = cycle.isEmpty() ? seedSolution.startVertexId() : cycle.get(0);
            used = new boolean[instance.size];
            cycle.clear();
            cycle.add(start);
            used[start] = true;
            int nearest = nearestUnusedVertex(instance, start, used);
            if (nearest >= 0) {
                cycle.add(nearest);
                used[nearest] = true;
            }
        }

        List<Integer> unused = new ArrayList<>(instance.size - cycle.size());
        for (int vertex = 0; vertex < instance.size; vertex++) {
            if (!used[vertex]) {
                unused.add(vertex);
            }
        }

        int[][] distances = instance.distanceMatrix.distances;
        int[] profits = profits(instance);

        while (!unused.isEmpty()) {
            List<RepairCandidate> candidates = new ArrayList<>(unused.size());
            for (int unusedIndex = 0; unusedIndex < unused.size(); unusedIndex++) {
                int vertex = unused.get(unusedIndex);
                int bestCost = Integer.MAX_VALUE;
                int secondBestCost = Integer.MAX_VALUE;
                int bestPosition = -1;

                for (int position = 0; position < cycle.size(); position++) {
                    int a = cycle.get(position);
                    int b = cycle.get((position + 1) % cycle.size());
                    int cost = distances[a][vertex] + distances[vertex][b] - distances[a][b] - profits[vertex];
                    if (cost < bestCost) {
                        secondBestCost = bestCost;
                        bestCost = cost;
                        bestPosition = position;
                    } else if (cost < secondBestCost) {
                        secondBestCost = cost;
                    }
                }

                int regret = secondBestCost - bestCost;
                candidates.add(new RepairCandidate(unusedIndex, vertex, bestPosition, regret, bestCost));
            }

            candidates.sort(Comparator
                    .comparingInt(RepairCandidate::regret).reversed()
                    .thenComparingInt(RepairCandidate::bestCost)
                    .thenComparingInt(RepairCandidate::vertex));

            int availableExplorationCandidates = Math.min(TOP_K, candidates.size()) - 1;
            int selectedRank = 0;
            if (availableExplorationCandidates > 0 && repairRandom.nextDouble() < EXPLORATION_PROBABILITY) {
                selectedRank = 1 + repairRandom.nextInt(availableExplorationCandidates);
            }

            RepairCandidate selected = candidates.get(selectedRank);
            int vertex = unused.remove(selected.unusedIndex());
            cycle.add(selected.insertAfterPosition() + 1, vertex);
        }

        Solution full = new Solution(instance.name, seedSolution.startVertexId(), cycle);
        return phaseTwoDelete.improve(instance, full);
    }

    private static List<Integer> bestSeedVertices(Instance instance, List<List<Integer>> cycles, int count) {
        if (cycles.isEmpty()) {
            return List.of();
        }

        int[][] distances = instance.distanceMatrix.distances;
        int[] profits = profits(instance);
        Map<Integer, VertexScore> scores = new HashMap<>();

        for (List<Integer> cycle : cycles) {
            int size = cycle.size();
            for (int position = 0; position < size; position++) {
                int vertex = cycle.get(position);
                int prev = cycle.get(position == 0 ? size - 1 : position - 1);
                int next = cycle.get(position + 1 == size ? 0 : position + 1);
                int localContribution = profits[vertex]
                        - (distances[prev][vertex] + distances[vertex][next] - distances[prev][next]);
                scores.computeIfAbsent(vertex, ignored -> new VertexScore(vertex)).accept(localContribution);
            }
        }

        return scores.values().stream()
                .sorted(Comparator
                        .comparingLong(VertexScore::score).reversed()
                        .thenComparingInt(VertexScore::vertex))
                .limit(count)
                .map(VertexScore::vertex)
                .toList();
    }

    private static List<Integer> bestFragment(Instance instance, List<List<Integer>> fragments) {
        if (fragments.isEmpty()) {
            return List.of();
        }
        int[][] distances = instance.distanceMatrix.distances;
        int[] profits = profits(instance);

        List<Integer> best = List.of();
        int bestScore = Integer.MIN_VALUE;
        for (List<Integer> fragment : fragments) {
            if (fragment.size() < 2) {
                continue;
            }
            int score = 0;
            for (int vertex : fragment) {
                score += profits[vertex];
            }
            for (int i = 0; i + 1 < fragment.size(); i++) {
                score -= distances[fragment.get(i)][fragment.get(i + 1)];
            }
            score -= distances[fragment.get(0)][fragment.get(fragment.size() - 1)];
            score += fragment.size();

            if (score > bestScore || (score == bestScore && fragment.size() > best.size())) {
                bestScore = score;
                best = fragment;
            }
        }
        return best;
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

    private static List<Integer> completeOrderedFragments(Instance instance, int startVertexId, List<List<Integer>> fragments) {
        if (fragments.isEmpty()) {
            return new FastRandomized2RegretParameterizedWithPhaseTwoDelete(
                    "fallback_fast_r2", TOP_K, EXPLORATION_PROBABILITY, new Random(startVertexId + 17L)
            ).solve(instance, startVertexId).cycle();
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
            Insertion selected = bestProtectedTwoRegretInsertion(distances, profits, cycle, unused, protectedEdges);
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

    private static Insertion bestProtectedTwoRegretInsertion(int[][] distances, int[] profits, List<Integer> cycle, List<Integer> unused, Set<Long> protectedEdges) {
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
                int cost = distances[a][vertex] + distances[vertex][b] - distances[a][b] - profits[vertex];
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

    private static Move findBestLocalSearchMove(int[][] distances, int[] profits, List<Integer> cycle, boolean[] selected) {
        int size = cycle.size();
        Move bestMove = null;
        int bestGain = 0;

        if (size > 2) {
            for (int position = 0; position < size; position++) {
                int vertex = cycle.get(position);
                int previous = cycle.get(position == 0 ? size - 1 : position - 1);
                int next = cycle.get(position + 1 == size ? 0 : position + 1);
                int gain = distances[previous][vertex] + distances[vertex][next] - distances[previous][next] - profits[vertex];
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

    private record ScoredSolution(Solution solution, int objective, int runIndex) {}
    private record RepairCandidate(int unusedIndex, int vertex, int insertAfterPosition, int regret, int bestCost) {}
    private record Insertion(int unusedIndex, int insertAfterPosition) {}

    private static final class VertexScore {
        private final int vertex;
        private int frequency;
        private int bestLocalContribution = Integer.MIN_VALUE;

        VertexScore(int vertex) {
            this.vertex = vertex;
        }

        void accept(int localContribution) {
            frequency++;
            bestLocalContribution = Math.max(bestLocalContribution, localContribution);
        }

        int vertex() {
            return vertex;
        }

        long score() {
            return frequency * 1_000_000L + bestLocalContribution;
        }
    }

    private enum MoveType { DELETE, INSERT, REPLACE, TWO_OPT }

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
