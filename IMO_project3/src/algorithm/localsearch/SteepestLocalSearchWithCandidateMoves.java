package algorithm.localsearch;

import algorithm.OptimizationAlgorithm;
import algorithm.localsearch.candidate.CandidateEdges;
import algorithm.localsearch.candidate.CandidateNeighborhood;
import algorithm.localsearch.move.Move;
import model.Instance;
import model.Solution;
import model.Vertex;

import java.util.List;

/**
 * Lokalne przeszukiwanie w wersji stromej z ruchami kandydackimi.
 *
 * <p>Algorytm generuje w każdej iteracji tylko ruchy wprowadzające
 * co najmniej jedną krawędź kandydacką i wybiera najlepszy ruch
 * poprawiający spośród tych ruchów.</p>
 */
public final class SteepestLocalSearchWithCandidateMoves implements OptimizationAlgorithm {
    private final String name;
    private final OptimizationAlgorithm initialSolutionAlgorithm;
    private final int candidateCount;

    /**
     * Tworzy algorytm lokalnego przeszukiwania z domyślną liczbą
     * najbliższych wierzchołków używaną do budowy krawędzi kandydackich.
     *
     * @param name                     nazwa algorytmu używana w wynikach eksperymentu
     * @param initialSolutionAlgorithm algorytm tworzący rozwiązanie startowe
     */
    public SteepestLocalSearchWithCandidateMoves(
        String name,
        OptimizationAlgorithm initialSolutionAlgorithm
    ) {
        this(name, initialSolutionAlgorithm, CandidateEdges.DEFAULT_CANDIDATE_COUNT);
    }

    /**
     * Tworzy algorytm lokalnego przeszukiwania z podaną liczbą
     * najbliższych wierzchołków używaną do budowy krawędzi kandydackich.
     *
     * @param name                     nazwa algorytmu używana w wynikach eksperymentu
     * @param initialSolutionAlgorithm algorytm tworzący rozwiązanie startowe
     * @param candidateCount           liczba najbliższych wierzchołków używana do budowy krawędzi kandydackich
     */
    public SteepestLocalSearchWithCandidateMoves(
        String name,
        OptimizationAlgorithm initialSolutionAlgorithm,
        int candidateCount
    ) {
        this.name = name;
        this.initialSolutionAlgorithm = initialSolutionAlgorithm;
        this.candidateCount = candidateCount;
    }

    /**
     * Zwraca nazwę algorytmu.
     *
     * @return nazwa algorytmu
     */
    @Override
    public String name() {
        return name;
    }

    /**
     * Uruchamia lokalne przeszukiwanie z ruchami kandydackimi.
     *
     * @param instance      instancja problemu
     * @param startVertexId wierzchołek startowy przekazywany do algorytmu startowego
     * @return rozwiązanie otrzymane po zakończeniu lokalnego przeszukiwania
     */
    @Override
    public Solution solve(Instance instance, int startVertexId) {
        Cycle cycle = buildInitialCycle(instance, startVertexId);
        CandidateEdges candidateEdges = new CandidateEdges(instance, candidateCount);
        CandidateNeighborhood candidateNeighborhood =
            new CandidateNeighborhood(cycle, instance.vertices, candidateEdges);

        int[][] distanceMatrix = instance.distanceMatrix.distances;
        int[] profit = buildProfitArray(instance.vertices);

        searchWithCandidateMoves(cycle, candidateNeighborhood, distanceMatrix, profit);

        return new Solution(instance.name, startVertexId, cycle);
    }

    /**
     * Tworzy początkowy cykl na podstawie algorytmu rozwiązania startowego.
     *
     * @param instance      instancja problemu
     * @param startVertexId wierzchołek startowy
     * @return początkowy cykl
     */
    private Cycle buildInitialCycle(Instance instance, int startVertexId) {
        Solution initialSolution = initialSolutionAlgorithm.solve(instance, startVertexId);
        return new Cycle(initialSolution.cycle(), instance.size);
    }

    /**
     * Wykonuje główną pętlę lokalnego przeszukiwania z ruchami kandydackimi.
     *
     * @param cycle                 bieżący cykl
     * @param candidateNeighborhood generator sąsiedztwa kandydackiego
     * @param distanceMatrix        macierz odległości
     * @param profit                zyski wierzchołków
     */
    private void searchWithCandidateMoves(
        Cycle cycle,
        CandidateNeighborhood candidateNeighborhood,
        int[][] distanceMatrix,
        int[] profit
    ) {
        Move bestMove;

        do {
            List<Move> moves = candidateNeighborhood.neighborhoodSwapEdges();
            bestMove = findBestMove(moves, cycle, distanceMatrix, profit);

            if (bestMove != null) {
                bestMove.apply(cycle);
            }
        } while (bestMove != null);
    }

    /**
     * Znajduje najlepszy ruch poprawiający w podanej liście ruchów.
     *
     * @param moves          lista ruchów kandydackich
     * @param cycle          bieżący cykl
     * @param distanceMatrix macierz odległości
     * @param profit         zyski wierzchołków
     * @return najlepszy ruch poprawiający albo null, jeżeli taki ruch nie istnieje
     */
    private Move findBestMove(
        List<Move> moves,
        Cycle cycle,
        int[][] distanceMatrix,
        int[] profit
    ) {
        Move bestMove = null;
        int bestDelta = 0;

        for (Move move : moves) {
            int delta = move.delta(cycle, distanceMatrix, profit);

            if (delta > bestDelta) {
                bestDelta = delta;
                bestMove = move;
            }
        }

        return bestMove;
    }

    /**
     * Buduje tablicę zysków indeksowaną identyfikatorem wierzchołka.
     *
     * @param vertices wszystkie wierzchołki instancji
     * @return tablica zysków wierzchołków
     */
    private static int[] buildProfitArray(Vertex[] vertices) {
        int[] profit = new int[vertices.length];

        for (Vertex vertex : vertices) {
            profit[vertex.id] = vertex.profit;
        }

        return profit;
    }
}
