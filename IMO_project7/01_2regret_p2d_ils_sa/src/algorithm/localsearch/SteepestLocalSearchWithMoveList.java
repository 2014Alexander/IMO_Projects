package algorithm.localsearch;

import algorithm.SolutionImprover;
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
public final class SteepestLocalSearchWithMoveList implements SolutionImprover {

    /**
     * Uruchamia lokalne przeszukiwanie z listą LM dla gotowego rozwiązania.
     *
     * @param instance instancja problemu
     * @param solution rozwiązanie wejściowe
     * @return rozwiązanie otrzymane po zakończeniu lokalnego przeszukiwania
     */
    @Override
    public Solution improve(Instance instance, Solution solution) {
        IndexedCycle cycle = new IndexedCycle(solution.cycle(), instance.size);
        MoveListBuilder moveListBuilder = new MoveListBuilder(instance);
        List<CachedMove> moveList = new ArrayList<>();

        solveByMoveList(cycle, moveListBuilder, moveList);

        return new Solution(solution.instanceName(), solution.startVertexId(), cycle.toCycle().toList());
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
