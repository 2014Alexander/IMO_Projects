package algorithm.localsearch;

import algorithm.OptimizationAlgorithm;
import algorithm.localsearch.move.Move;
import model.Instance;
import model.Solution;
import model.Vertex;

import java.util.List;
import java.util.Random;

public final class RandomWalk implements OptimizationAlgorithm {

    private final String name;
    private final OptimizationAlgorithm initialSolutionAlgorithm;
    private final NeighborhoodType neighborhoodType;
    private final long timeLimitNanos;
    private final Random random;

    public RandomWalk(
        String name,
        OptimizationAlgorithm initialSolutionAlgorithm,
        NeighborhoodType neighborhoodType,
        long timeLimitNanos
    ) {
        this(name, initialSolutionAlgorithm, neighborhoodType, timeLimitNanos, new Random());
    }

    /*
     * Wersja seeded zachowuje pełną powtarzalność eksperymentu
     * także wtedy, gdy RandomWalk startuje z rozwiązania początkowego.
     */
    public RandomWalk(
        String name,
        OptimizationAlgorithm initialSolutionAlgorithm,
        NeighborhoodType neighborhoodType,
        long timeLimitNanos,
        long seed
    ) {
        this(name, initialSolutionAlgorithm, neighborhoodType, timeLimitNanos, new Random(seed));
    }

    private RandomWalk(
        String name,
        OptimizationAlgorithm initialSolutionAlgorithm,
        NeighborhoodType neighborhoodType,
        long timeLimitNanos,
        Random random
    ) {
        this.name = name;
        this.initialSolutionAlgorithm = initialSolutionAlgorithm;
        this.neighborhoodType = neighborhoodType;
        this.timeLimitNanos = timeLimitNanos;
        this.random = random;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Solution solve(Instance instance, int startVertexId) {
        /*
         * RandomWalk startuje z rozwiązania wygenerowanego
         * przez przekazany algorytm konstrukcyjny.
         */
        Cycle cycle = createStartCycle(instance, startVertexId);

        /*
         * Neighborhood pracuje bezpośrednio na obiekcie cycle.
         * Oznacza to, że po każdym apply(...) kolejne ruchy będą już generowane
         * dla aktualnego stanu rozwiązania.
         */
        Neighborhood neighborhood = new Neighborhood(cycle, instance.vertices);

        int[][] distanceMatrix = instance.distanceMatrix.distances;
        int[] profit = buildProfitArray(instance.vertices);

        /*
         * currentScore to wartość bieżącego rozwiązania,
         * bestScore to najlepsza wartość znaleziona w całym losowym błądzeniu,
         * bestCycle to kopia najlepszego dotąd cyklu.
         */
        int currentScore = evaluate(cycle, distanceMatrix, profit);
        int bestScore = currentScore;
        Cycle bestCycle = new Cycle(cycle);

        /*
         * RandomWalk działa przez zadany limit czasu.
         * W każdej iteracji wykonujemy jeden losowo wybrany ruch,
         * niezależnie od tego, czy poprawia rozwiązanie czy je pogarsza.
         */
        long endTime = System.nanoTime() + timeLimitNanos;

        while (System.nanoTime() < endTime) {
            /*
             * Dla aktualnego rozwiązania generujemy pełną listę ruchów
             * zgodnie z wybranym typem sąsiedztwa.
             */
            List<Move> moves = createMoves(neighborhood);

            /*
             * W random walk nie szukamy najlepszego ruchu.
             * Wybieramy jeden ruch całkowicie losowo.
             */
            Move move = moves.get(random.nextInt(moves.size()));

            /*
             * Najpierw liczymy deltę dla bieżącego rozwiązania,
             * potem wykonujemy ruch i aktualizujemy wartość funkcji celu.
             */
            int delta = move.delta(cycle, distanceMatrix, profit);
            move.apply(cycle);

            currentScore += delta;

            /*
             * Mimo że wykonujemy także ruchy pogarszające,
             * zapamiętujemy najlepsze rozwiązanie znalezione w całym przebiegu.
             */
            if (currentScore > bestScore) {
                bestScore = currentScore;
                bestCycle = new Cycle(cycle);
            }
        }

        return new Solution(instance.name, startVertexId, bestCycle);
    }

    private Cycle createStartCycle(Instance instance, int startVertexId) {
        /*
         * RandomWalk zaczyna od pełnego rozwiązania wygenerowanego
         * przez algorytm startowy, np. RandomSolution.
         *
         * Rozwiązanie z modelu Solution przepisujemy do wewnętrznej
         * reprezentacji Cycle używanej przez ruchy local search.
         */
        Solution initialSolution = initialSolutionAlgorithm.solve(instance, startVertexId);

        Cycle cycle = new Cycle(instance.size);
        for (int vertexId : initialSolution.cycle()) {
            cycle.append(vertexId);
        }

        return cycle;
    }

    private List<Move> createMoves(Neighborhood neighborhood) {
        /*
         * Oba warianty sąsiedztwa zawsze zawierają ruchy insert/delete.
         * Różni się tylko rodzaj ruchów wewnątrztrasowych:
         * - SWAP_VERTICES
         * - SWAP_EDGES
         */
        return neighborhoodType == NeighborhoodType.SWAP_VERTICES
            ? neighborhood.neighborhoodSwapVertices()
            : neighborhood.neighborhoodSwapEdges();
    }

    private static int[] buildProfitArray(Vertex[] vertices) {
        /*
         * Zamieniamy dane wierzchołków na prostą tablicę profit[id],
         * żeby ruchy mogły szybko odczytywać zysk dla danego vertexId.
         */
        int[] profit = new int[vertices.length];

        for (Vertex vertex : vertices) {
            profit[vertex.id] = vertex.profit;
        }

        return profit;
    }

    private static int evaluate(Cycle cycle, int[][] distanceMatrix, int[] profit) {
        /*
         * Funkcja celu:
         * suma profitów wybranych wierzchołków
         * minus długość zamkniętego cyklu.
         *
         * Używamy tego tylko do oceny rozwiązania startowego.
         * Potem wartość utrzymujemy już przy pomocy delt ruchów.
         */
        int score = 0;
        int cycleSize = cycle.size();

        for (int i = 0; i < cycleSize; i++) {
            score += profit[cycle.cycle[i]];
        }

        for (int i = 0; i < cycleSize; i++) {
            int next = cycle.nextIndex(i);
            score -= distanceMatrix[cycle.cycle[i]][cycle.cycle[next]];
        }

        return score;
    }
}
