# Zadanie 2. Local Search dla zmodyfikowanego problemu komiwojażera

Olga Krużyńska (151919)  
Aleksandr Tsekhanovskii (153929)
https://github.com/2014Alexander/IMO_Projects/tree/main/IMO_project2

## Opis zadania

Projekt dotyczy drugiego etapu badań nad wariantem problemu komiwojażera z zyskami. Dla każdej instancji dany jest zbiór wierzchołków, zysk odwiedzenia każdego wierzchołka oraz macierz odległości pomiędzy parami wierzchołków. Celem jest wyznaczenie takiego cyklu, aby maksymalizować wartość funkcji celu, rozumianą jako różnica pomiędzy sumą profitów odwiedzonych wierzchołków a długością zamkniętej trasy.

W odróżnieniu od klasycznego problemu komiwojażera nie trzeba odwiedzać wszystkich wierzchołków. Należy wybrać tylko taki podzbiór, którego odwiedzenie jest opłacalne z punktu widzenia całego rozwiązania.

W laboratorium 2 analizowano algorytmy local search. Każda metoda startuje od rozwiązania początkowego, a następnie próbuje poprawiać je przy użyciu ruchów lokalnych ocenianych przez zmianę funkcji celu. Dodatkowo jako punkt odniesienia uwzględniono random walk, który wykonuje losowe ruchy przez taki sam czas, jak średnio najwolniejszy wariant local search.

## Funkcja celu i ocena ruchów

Jakość rozwiązania oceniana jest na podstawie funkcji celu

f(C) = suma profitów odwiedzonych wierzchołków − długość zamkniętego cyklu.

W implementacji local search nie przeliczano całej funkcji celu od początku po każdym ruchu. Zamiast tego każdy ruch zwraca wartość Δ, czyli zmianę funkcji celu po jego wykonaniu.

Jeżeli Δ > 0, ruch poprawia rozwiązanie. Jeżeli Δ < 0, ruch je pogarsza. Taki sposób oceny pozwala szybko porównywać ruchy wstawienia, usunięcia, zamiany wierzchołków oraz zamiany krawędzi bez kosztownego przeliczania całego cyklu po każdej modyfikacji.

## Pseudokody algorytmów

Poniżej zebrano pseudokody najważniejszych elementów wykorzystanych w laboratorium 2.

### RandomSolution

```text
RandomSolution(startVertexId, vertices, D, profit, random)

    wybierz losowy rozmiar rozwiązania k z przedziału [2, |vertices|]
    cycle ← [startVertexId]
    remaining ← wszystkie wierzchołki poza startVertexId
    wybierz losowo k − 1 różnych wierzchołków z remaining
    losowo przetasuj ich kolejność
    dołącz je do cycle

    zwróć cycle
```

### GreedyCycle

```text
GreedyCycle(startVertexId, vertices, D, profit)

    cycle ← [startVertexId]
    second ← najbliższy sąsiad startVertexId
    dodaj second do cycle

    dopóki istnieją niewybrane wierzchołki
        dla każdego niewybranego wierzchołka
            wyznacz najlepsze miejsce wstawienia do aktualnego cyklu
            licz koszt wstawienia jako przyrost długości minus profit

        wybierz wierzchołek i miejsce o najlepszym koszcie
        wstaw wybrany wierzchołek do cycle

    zwróć cycle
```

### PhaseTwoDelete

```text
PhaseTwoDelete(cycle, D, profit)

    dopóki istnieje ruch delete o dodatniej delcie
        wybierz najlepszy ruch usunięcia
        usuń odpowiedni wierzchołek z cycle

    zwróć cycle
```

### BestHeuristicSolution

```text
BestHeuristicSolution(startVertexId, vertices, D, profit)

    cycle ← GreedyCycle(startVertexId, vertices, D, profit)
    cycle ← PhaseTwoDelete(cycle, D, profit)

    zwróć cycle
```

### GreedyLocalSearch

```text
GreedyLocalSearch(initialSolutionAlgorithm, neighborhoodType, startVertexId, vertices, D, profit, random)

    cycle ← initialSolutionAlgorithm(startVertexId, vertices, D, profit, random)

    powtarzaj
        improved ← false

        jeżeli neighborhoodType = SWAP_VERTICES
            moves ← Neighborhood_SwapVertices(cycle, vertices)
        w przeciwnym wypadku
            moves ← Neighborhood_SwapEdges(cycle, vertices)

        losowo przetasuj moves

        dla każdego move z moves
            Δ ← move.delta(cycle, D, profit)

            jeżeli Δ > 0
                move.apply(cycle)
                improved ← true
                przerwij pętlę

    dopóki improved = true

    zwróć cycle
```

### SteepestLocalSearch

```text
SteepestLocalSearch(initialSolutionAlgorithm, neighborhoodType, startVertexId, vertices, D, profit)

    cycle ← initialSolutionAlgorithm(startVertexId, vertices, D, profit)

    powtarzaj
        improved ← false
        bestMove ← brak
        bestDelta ← 0

        jeżeli neighborhoodType = SWAP_VERTICES
            moves ← Neighborhood_SwapVertices(cycle, vertices)
        w przeciwnym wypadku
            moves ← Neighborhood_SwapEdges(cycle, vertices)

        dla każdego move z moves
            Δ ← move.delta(cycle, D, profit)

            jeżeli Δ > bestDelta
                bestDelta ← Δ
                bestMove ← move

        jeżeli bestMove ≠ brak
            bestMove.apply(cycle)
            improved ← true

    dopóki improved = true

    zwróć cycle
```

### RandomWalk

```text
RandomWalk(initialSolutionAlgorithm, neighborhoodType, startVertexId, vertices, D, profit, timeLimit, random)

    cycle ← initialSolutionAlgorithm(startVertexId, vertices, D, profit, random)
    currentScore ← Evaluate(cycle, D, profit)

    bestScore ← currentScore
    bestCycle ← kopia cycle
    endTime ← bieżący czas + timeLimit

    dopóki bieżący czas < endTime
        jeżeli neighborhoodType = SWAP_VERTICES
            moves ← Neighborhood_SwapVertices(cycle, vertices)
        w przeciwnym wypadku
            moves ← Neighborhood_SwapEdges(cycle, vertices)

        move ← losowo wybrany ruch
        Δ ← move.delta(cycle, D, profit)
        move.apply(cycle)
        currentScore ← currentScore + Δ

        jeżeli currentScore > bestScore
            bestScore ← currentScore
            bestCycle ← kopia cycle

    zwróć bestCycle
```

## Sposób przeprowadzenia eksperymentu

Eksperyment przeprowadzono dla dwóch instancji testowych: TSPA oraz TSPB. Dla każdej instancji wykonano 100 uruchomień każdego algorytmu. W każdym uruchomieniu używano tego samego zestawu wierzchołków startowych oraz seedów losowych dla wszystkich porównywanych metod, aby zachować sprawiedliwe warunki porównania.

Porównano osiem wariantów local search. Rozpatrywano dwa sposoby przeszukiwania sąsiedztwa: podejście zachłanne oraz podejście strome. Dla każdego z nich użyto dwóch rodzajów rozwiązania początkowego: startu losowego oraz startu opartego na najlepszej heurystyce z poprzedniego laboratorium. Dodatkowo badano dwa typy sąsiedztwa: zamianę wierzchołków oraz zamianę krawędzi. Jako punkt odniesienia uwzględniono także rozwiązanie bazowe z poprzedniego laboratorium oraz dwie wersje random walk.

Dla każdego algorytmu zebrano statystyki zbiorcze: średnią, minimum i maksimum wartości funkcji celu oraz średni, minimalny i maksymalny czas działania. W przypadku random walk limit czasu ustawiono jako średni czas najwolniejszej wersji local search, zgodnie z założeniem laboratorium.

## Wyniki zbiorcze

Na kolejnych stronach zamieszczono dwie tabele zbiorcze. Pierwsza przedstawia średnie, minimalne i maksymalne wartości funkcji celu dla wszystkich badanych metod. Druga pokazuje średnie, minimalne i maksymalne czasy działania w milisekundach.

## Wyniki i interpretacja

### Instancja TSPA

Dla instancji TSPA najlepszy średni wynik spośród wszystkich badanych metod local search uzyskał wariant stromy z losowym startem i sąsiedztwem opartym na zamianie krawędzi. Średnia wartość funkcji celu wyniosła 5734,21, a najlepszy pojedynczy wynik 7640. Bardzo zbliżone rezultaty osiągnął wariant zachłanny z losowym startem i tym samym typem sąsiedztwa, dla którego średnia wyniosła 5546,54, a maksimum 6960.

Wyraźnie widać, że dla TSPA kluczowe znaczenie ma rodzaj sąsiedztwa. Wersje wykorzystujące zamianę krawędzi są zdecydowanie lepsze od wersji opartych na zamianie wierzchołków. Szczególnie słabo wypadają warianty z losowym startem i zamianą wierzchołków, dla których średnie wartości funkcji celu są ujemne. Oznacza to, że przy słabym rozwiązaniu początkowym oraz mniej korzystnym sąsiedztwie local search nie potrafi skutecznie wydostać się z bardzo słabych obszarów przestrzeni rozwiązań.

Warianty startujące od najlepszego rozwiązania początkowego dają wyniki bardziej stabilne i dodatnie, ale na tej instancji nie przewyższają najlepszych metod z losowym startem i zamianą krawędzi. Pokazuje to, że dobre rozwiązanie początkowe nie zawsze jest ważniejsze od zdolności sąsiedztwa do wykonywania silnych przekształceń struktury cyklu.

Metoda bazowa z poprzedniego laboratorium osiąga średnią 3686,00, czyli wynik wyraźnie gorszy od najlepszych wersji local search, ale jednocześnie lepszy od najsłabszych wariantów wykorzystujących zamianę wierzchołków.

Obie wersje random walk wypadają bardzo słabo. Średnie wyniki są skrajnie ujemne i zdecydowanie gorsze od wszystkich metod local search. Oznacza to, że samo losowe przemieszczanie się po sąsiedztwie, nawet z zapamiętywaniem najlepszego rozwiązania, nie stanowi skutecznej metody optymalizacji dla tej instancji.

### Instancja TSPB

Dla instancji TSPB najlepszy średni wynik uzyskał wariant zachłanny z losowym startem i zamianą krawędzi. Średnia wartość funkcji celu wyniosła 17216,29, a maksimum 19217. Bardzo dobre wyniki uzyskały również wariant stromy z losowym startem i tym samym typem sąsiedztwa oraz wariant zachłanny startujący od najlepszego rozwiązania początkowego.

Podobnie jak dla TSPA, najważniejsze okazało się sąsiedztwo oparte na zamianie krawędzi. Warianty wykorzystujące zamianę wierzchołków są zauważalnie słabsze. Szczególnie słabo wypada wersja zachłanna z losowym startem i zamianą wierzchołków, dla której średnia wartość funkcji celu pozostaje ujemna. Wersje startujące od najlepszego rozwiązania początkowego są bardziej stabilne, ale nadal przegrywają z najlepszymi wariantami wykorzystującymi zamianę krawędzi.

Warto odnotować także dobry wynik metody bazowej z poprzedniego laboratorium, która osiągnęła średnią 14970,01. Oznacza to, że najlepsza heurystyka konstrukcyjna nadal stanowi mocny punkt odniesienia. Mimo to najlepsze warianty local search potrafią ją wyraźnie poprawić.

Także dla TSPB random walk okazuje się bardzo słabym punktem odniesienia. Średnie wartości funkcji celu są zdecydowanie gorsze od wszystkich metod local search, co potwierdza, że losowe ruchy nie prowadzą tutaj do konkurencyjnych rozwiązań.

### Analiza czasu działania

Porównanie czasów działania pokazuje wyraźny kompromis pomiędzy jakością rozwiązania a kosztem obliczeń. Dla obu instancji najwolniejsze są warianty zachłanne z losowym startem. Ich średni czas działania wynosi około 428–450 ms dla TSPA i 377–433 ms dla TSPB. Podobny rząd wielkości mają obie wersje random walk, co wynika bezpośrednio z celowego ograniczenia czasu ich działania do poziomu najwolniejszej wersji local search.

Znacznie szybsze są warianty startujące od najlepszego rozwiązania początkowego. Dla TSPA ich średnie czasy wynoszą około 31–37 ms, a dla TSPB około 24–37 ms. W praktyce oznacza to, że dobre rozwiązanie początkowe skraca liczbę potrzebnych ulepszeń i pozwala szybciej dojść do lokalnego optimum.

Na instancji TSPA szczególnie interesujący jest wariant stromy z losowym startem i zamianą krawędzi, ponieważ daje najlepszą średnią wartość funkcji celu, a jednocześnie działa wyraźnie szybciej od odpowiedniego wariantu zachłannego. Pokazuje to, że dla tego typu sąsiedztwa pełny przegląd ruchów i wybór najlepszego może być zarówno skuteczny, jak i praktycznie opłacalny.

## Wnioski

Najważniejszy wniosek z eksperymentu jest taki, że o jakości rozwiązania w laboratorium 2 w największym stopniu decyduje rodzaj sąsiedztwa. W obu instancjach warianty wykorzystujące zamianę krawędzi są zdecydowanie lepsze od wariantów opartych na zamianie wierzchołków. Oznacza to, że przekształcenia zmieniające strukturę połączeń w cyklu są znacznie skuteczniejsze niż samo przestawianie odwiedzanych wierzchołków.

Drugim ważnym wnioskiem jest znaczenie rozwiązania początkowego. Start od najlepszego rozwiązania daje wyniki bardziej stabilne oraz wyraźnie skraca czas działania, ale nie zawsze prowadzi do najlepszego średniego wyniku. W obu instancjach najlepsze rezultaty osiągnęły warianty z losowym startem i zamianą krawędzi, co pokazuje, że mocne sąsiedztwo potrafi dobrze wykorzystać nawet mniej uporządkowane rozwiązanie początkowe.

Random walk okazał się bardzo słabym punktem odniesienia. Mimo takiego samego limitu czasu jak najwolniejsza wersja local search nie potrafi znaleźć rozwiązań konkurencyjnych. Potwierdza to, że sama eksploracja losowa bez mechanizmu systematycznej poprawy nie wystarcza w tym zadaniu.

Jeżeli wskazać najlepsze metody osobno dla obu instancji, to dla TSPA najlepszy okazał się wariant stromy z losowym startem i zamianą krawędzi, natomiast dla TSPB wariant zachłanny z losowym startem i zamianą krawędzi. Jeżeli natomiast uwzględnić jednocześnie jakość i czas działania, bardzo mocnymi kompromisami pozostają warianty startujące od najlepszego rozwiązania i korzystające z zamiany krawędzi, ponieważ są szybkie i jednocześnie dają wysokie oraz stabilne wyniki.
