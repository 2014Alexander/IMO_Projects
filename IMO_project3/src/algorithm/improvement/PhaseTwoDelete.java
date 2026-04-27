package algorithm.improvement;

import model.Instance;
import model.Solution;
import model.Vertex;

import java.util.ArrayList;
import java.util.List;

public class PhaseTwoDelete {

    public Solution improve(Instance instance, Solution solution) {
        validateInputs(instance, solution);
        // Skróty do danych instancji:
        // D[i][j] - odległość między wierzchołkami i oraz j,
        // vertices[v].profit - zysk wierzchołka v.
        int[][] D = instance.distanceMatrix.distances;
        Vertex[] vertices = instance.vertices;

        // Tworzymy kopię cyklu, żeby nie modyfikować obiektu wejściowego.
        List<Integer> cycle = new ArrayList<>(solution.cycle());

        // Faza II działa iteracyjnie:
        // w każdej iteracji próbujemy znaleźć taki wierzchołek,
        // którego usunięcie da największą dodatnią poprawę funkcji celu.
        //
        // Nie schodzimy poniżej 2 wierzchołków, bo cykl musi nadal istnieć.
        while (cycle.size() > 2) {
            int bestImprovement = 0;
            int bestIndex = -1;

            // Sprawdzamy każdy wierzchołek aktualnego cyklu jako kandydata do usunięcia.
            for (int i = 0; i < cycle.size(); i++) {
                int v = cycle.get(i);

                // Poprzednik i następnik w cyklu.
                // Używamy indeksowania cyklicznego:
                // - dla pierwszego elementu poprzednikiem jest ostatni,
                // - dla ostatniego elementu następnikiem jest pierwszy.
                int prev = cycle.get((i - 1 + cycle.size()) % cycle.size());
                int next = cycle.get((i + 1) % cycle.size());

                // Po usunięciu v znikają dwie krawędzie:
                // prev -> v oraz v -> next.
                int removedEdgeCost = D[prev][v] + D[v][next];

                // Po usunięciu v pojawia się jedna nowa krawędź:
                // prev -> next.
                int addedEdgeCost = D[prev][next];

                // Usuwając wierzchołek v, tracimy także jego profit,
                // bo nie będzie już należał do rozwiązania.
                int lostProfit = vertices[v].profit;

                // Poprawa funkcji celu po usunięciu v:
                //
                // oszczędzamy koszt dwóch usuwanych krawędzi
                // minus
                // płacimy koszt nowej krawędzi
                // minus
                // tracimy profit usuwanego wierzchołka.
                //
                // Jeśli improvement > 0, to usunięcie v poprawia rozwiązanie.
                int improvement = removedEdgeCost - addedEdgeCost - lostProfit;

                // Zapamiętujemy najlepszy ruch z tej iteracji.
                if (improvement > bestImprovement) {
                    bestImprovement = improvement;
                    bestIndex = i;
                }
            }

            // Jeśli znaleźliśmy ruch poprawiający rozwiązanie,
            // usuwamy odpowiedni wierzchołek i przechodzimy do kolejnej iteracji.
            if (bestImprovement > 0) {
                cycle.remove(bestIndex);
            } else {
                // Jeśli żadne usunięcie nie daje dodatniej poprawy,
                // kończymy fazę II.
                break;
            }
        }

        // Zwracamy nowe rozwiązanie z poprawionym cyklem.
        return new Solution(
                solution.instanceName(),
                solution.startVertexId(),
                cycle
        );
    }

    private static void validateInputs(Instance instance, Solution solution) {
        if (instance == null) {
            throw new IllegalArgumentException("Instance nie może być null.");
        }
        if (solution == null) {
            throw new IllegalArgumentException("Solution nie może być null.");
        }
        if (solution.cycle() == null) {
            throw new IllegalArgumentException("Cycle nie może być null.");
        }
    }
}