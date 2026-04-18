# Phase II Delete

```text
PhaseTwoDelete(cycle, D, profit)

    // Faza II działa iteracyjnie.
    // W każdej iteracji sprawdzamy,
    // czy opłaca się usunąć któryś wierzchołek z aktualnego cyklu.

    // Nie schodzimy poniżej 2 wierzchołków,
    // ponieważ cykl musi nadal istnieć.
    dopóki liczba wierzchołków w cycle > 2:

        bestImprovement ← 0
        bestVertexIndex ← brak

        // Sprawdzamy każdy wierzchołek jako kandydata do usunięcia.
        dla każdego indeksu i w cycle:

            v ← cycle[i]

            // Szukamy sąsiadów wierzchołka v w aktualnym cyklu:
            // prev to poprzednik v,
            // next to następnik v.
            prev ← poprzedni wierzchołek v w cycle
            next ← następny wierzchołek v w cycle

            // Po usunięciu v znikają dwie krawędzie:
            // prev → v oraz v → next.
            removedEdgeCost ← D[prev][v] + D[v][next]

            // Po usunięciu v pojawia się jedna nowa krawędź:
            // prev → next.
            addedEdgeCost ← D[prev][next]

            // Usuwając v, tracimy także jego profit,
            lostProfit ← profit[v]

            // Poprawa funkcji celu po usunięciu v:
            // oszczędzamy koszt dwóch usuwanych krawędzi,
            // odejmujemy koszt nowej krawędzi,
            // odejmujemy utracony profit wierzchołka.
            improvement ← removedEdgeCost - addedEdgeCost - lostProfit

            // Zapamiętujemy najlepsze usunięcie w tej iteracji.
            jeżeli improvement > bestImprovement:
                bestImprovement ← improvement
                bestVertexIndex ← i

        // Jeśli znaleźliśmy usunięcie poprawiające rozwiązanie,
        // wykonujemy najlepszy taki ruch.
        jeżeli bestImprovement > 0:
            usuń wierzchołek o indeksie bestVertexIndex z cycle

        // Jeśli żadne usunięcie nie daje dodatniej poprawy,
        // kończymy fazę II.
        w przeciwnym razie:
            przerwij

    zwróć cycle
```
