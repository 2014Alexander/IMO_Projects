package algorithm.localsearch;

import algorithm.CycleImprover;
import algorithm.SolutionImprover;
import algorithm.localsearch.candidate.CandidateEdges;
import algorithm.localsearch.candidate.CandidateNeighborhood;
import algorithm.localsearch.move.Move;
import model.Instance;
import model.Solution;
import model.Vertex;

/**
 * Lokalne przeszukiwanie w wersji stromej z ruchami kandydackimi.
 *
 * <p>Algorytm generuje w każdej iteracji tylko ruchy wprowadzające
 * co najmniej jedną krawędź kandydacką i wybiera najlepszy ruch
 * poprawiający spośród tych ruchów.</p>
 */
public final class SteepestLocalSearchWithCandidateMoves implements SolutionImprover, CycleImprover {
    private final int candidateCount;

    private Instance preparedInstance;
    private CandidateEdges preparedCandidateEdges;
    private int[] preparedProfit;

    /**
     * Tworzy fazę lokalnego przeszukiwania z domyślną liczbą
     * najbliższych wierzchołków używaną do budowy krawędzi kandydackich.
     */
    public SteepestLocalSearchWithCandidateMoves() {
        this(CandidateEdges.DEFAULT_CANDIDATE_COUNT);
    }

    /**
     * Tworzy fazę lokalnego przeszukiwania z podaną liczbą
     * najbliższych wierzchołków używaną do budowy krawędzi kandydackich.
     *
     * @param candidateCount liczba najbliższych wierzchołków używana do budowy krawędzi kandydackich
     */
    public SteepestLocalSearchWithCandidateMoves(int candidateCount) {
        this.candidateCount = candidateCount;
    }

    /**
     * Uruchamia lokalne przeszukiwanie z ruchami kandydackimi dla gotowego rozwiązania.
     *
     * @param instance instancja problemu
     * @param solution rozwiązanie wejściowe
     * @return rozwiązanie otrzymane po zakończeniu lokalnego przeszukiwania
     */
    @Override
    public Solution improve(Instance instance, Solution solution) {
        Cycle cycle = new Cycle(solution.cycle(), instance.size);
        improve(instance, cycle);

        return new Solution(solution.instanceName(), solution.startVertexId(), cycle.toList());
    }

    @Override
    public void improve(Instance instance, Cycle cycle) {
        prepareFor(instance);

        CandidateNeighborhood candidateNeighborhood =
            new CandidateNeighborhood(cycle, instance.vertices, preparedCandidateEdges);

        searchWithCandidateMoves(
            cycle,
            candidateNeighborhood,
            instance.distanceMatrix.distances,
            preparedProfit
        );
    }

    /**
     * Przygotowuje dane pomocnicze zależne wyłącznie od instancji.
     * Dane są przechowywane tylko w obrębie tego obiektu lokalnego przeszukiwania,
     * więc nie są współdzielone między niezależnymi uruchomieniami algorytmów.
     *
     * @param instance instancja, dla której mają być przygotowane dane pomocnicze
     */
    private void prepareFor(Instance instance) {
        if (preparedInstance == instance) {
            return;
        }

        preparedInstance = instance;
        preparedCandidateEdges = new CandidateEdges(instance, candidateCount);
        preparedProfit = buildProfitArray(instance.vertices);
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
            bestMove = candidateNeighborhood.bestMoveInSwapEdgesNeighborhood(distanceMatrix, profit);

            if (bestMove != null) {
                bestMove.apply(cycle);
            }
        } while (bestMove != null);
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
