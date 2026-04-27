package algorithm.localsearch;

import algorithm.OptimizationAlgorithm;
import algorithm.localsearch.movelist.CachedMove;
import algorithm.localsearch.movelist.MoveApplicability;
import algorithm.localsearch.movelist.MoveListBuilder;
import model.Instance;
import model.Solution;

import java.util.ArrayList;
import java.util.List;

/**
 * Lokalne przeszukiwanie w wersji stromej z listą LM w wersji alternatywnej.
 *
 * <p>Algorytm korzysta z zapamiętanych ruchów poprawiających przechowywanych
 * na liście uporządkowanej według delty od najlepszego ruchu do najsłabszego
 * ruchu poprawiającego.</p>
 */
public final class SteepestLocalSearchWithMoveList implements OptimizationAlgorithm {
    private final String name;
    private final OptimizationAlgorithm initialSolutionAlgorithm;

    /**
     * Tworzy algorytm lokalnego przeszukiwania z listą LM.
     *
     * @param name nazwa algorytmu używana w wynikach eksperymentu
     * @param initialSolutionAlgorithm algorytm tworzący rozwiązanie startowe
     */
    public SteepestLocalSearchWithMoveList(
        String name,
        OptimizationAlgorithm initialSolutionAlgorithm
    ) {
        this.name = name;
        this.initialSolutionAlgorithm = initialSolutionAlgorithm;
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
     * Uruchamia lokalne przeszukiwanie z listą LM dla podanej instancji.
     *
     * @param instance instancja problemu
     * @param startVertexId wierzchołek startowy używany przez algorytm rozwiązania początkowego
     * @return rozwiązanie otrzymane po zakończeniu lokalnego przeszukiwania
     */
    @Override
    public Solution solve(Instance instance, int startVertexId) {
        IndexedCycle cycle = buildInitialCycle(instance, startVertexId);
        MoveListBuilder moveListBuilder = new MoveListBuilder(instance);
        List<CachedMove> moveList = new ArrayList<>();

        solveByMoveList(cycle, moveListBuilder, moveList);

        return new Solution(instance.name, startVertexId, cycle.toCycle());
    }

    /**
     * Tworzy początkowy cykl indeksowany na podstawie algorytmu rozwiązania startowego.
     *
     * @param instance instancja problemu
     * @param startVertexId wierzchołek startowy
     * @return początkowy cykl indeksowany
     */
    private IndexedCycle buildInitialCycle(Instance instance, int startVertexId) {
        Solution initialSolution = initialSolutionAlgorithm.solve(instance, startVertexId);
        return new IndexedCycle(initialSolution.cycle(), instance.size);
    }

    /**
     * Wykonuje główną pętlę lokalnego przeszukiwania z listą LM.
     *
     * @param cycle bieżący cykl
     * @param moveListBuilder budowniczy listy LM
     * @param moveList lista LM
     */
    private void solveByMoveList(
        IndexedCycle cycle,
        MoveListBuilder moveListBuilder,
        List<CachedMove> moveList
    ) {
        moveListBuilder.fillMoveList(cycle, moveList);

        while (!moveList.isEmpty()) {
            findAndApplyMove(cycle, moveList);

            // W wariancie alternatywnym LM odbudowujemy dopiero wtedy,
            // gdy wyczerpiemy zapamiętane ruchy dla poprzedniego rozwiązania.
            if (moveList.isEmpty()) {
                moveListBuilder.fillMoveList(cycle, moveList);
            }
        }
    }

    /**
     * Szuka pierwszego aplikowalnego ruchu w LM i stosuje go do bieżącego cyklu.
     *
     * @param cycle bieżący cykl
     * @param moveList lista LM
     */
    private void findAndApplyMove(IndexedCycle cycle, List<CachedMove> moveList) {
        for (int index = 0; index < moveList.size(); index++) {
            CachedMove move = moveList.get(index);
            MoveApplicability applicability = move.applicability(cycle);

            if (applicability == MoveApplicability.APPLICABLE) {
                move.apply(cycle);
                // Ruchy lepsze od wykonanego zostały już sprawdzone i nie pasują
                // do bieżącego cyklu, a wykonany ruch nie powinien być użyty drugi raz.
                moveList.subList(0, index + 1).clear();
                return;
            }
        }

        // Żaden zapamiętany ruch nie pasuje już do bieżącego cyklu.
        // Pusta lista wymusi pełną ponowną ocenę ruchów aplikowalnych.
        moveList.clear();
    }
}
