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

Poniżej zebrano pseudokody wszystkich najważniejszych elementów wykorzystanych w laboratorium 2.

> [!IMPORTANT]
> Oba rodzaje sąsiedztwa zawsze zawierają ruchy `InsertMove` i `DeleteMove`.
> Różni się tylko ruch wewnątrztrasowy: `SwapVerticesMove` albo `SwapEdgesMove`.

> [!TIP]
> Najwygodniej czyta się tę sekcję grupami: najpierw `Notacja` i `Move`, potem `Ruchy`, następnie `Sąsiedztwa`, `Rozwiązania startowe` i na końcu `Algorytmy`.

### Układ sekcji

| Grupa | Zawartość |
|---|---|
| Notacja i interfejs | `Notacja`, `Move` |
| Ruchy elementarne | `InsertMove`, `DeleteMove`, `SwapVerticesMove`, `SwapEdgesMove` |
| Sąsiedztwa | `Neighborhood_SwapVertices`, `Neighborhood_SwapEdges` |
| Rozwiązania startowe i heurystyki | `RandomSolution`, `GreedyCycle`, `PhaseTwoDelete`, `BestHeuristicSolution` |
| Algorytmy | `GreedyLocalSearch`, `SteepestLocalSearch`, `RandomWalk` |

### Notacja i interfejs ruchu

<details open>
<summary><strong>Notacja</strong></summary>

```text
Notacja używana we wszystkich pseudokodach

D      - macierz odległości, gdzie D[a][b] oznacza odległość między wierzchołkami a i b.
profit - tablica zysków, gdzie profit[v] oznacza zysk z odwiedzenia wierzchołka v.
cycle  - aktualny cykl zapisany jako uporządkowana lista wybranych wierzchołków.
|cycle| - liczba wierzchołków w aktualnym cyklu.
prev(i) - indeks poprzednika pozycji i w cyklu.
next(i) - indeks następnika pozycji i w cyklu.
Δ      - delta funkcji celu, czyli zmiana wartości:
         suma profitów wybranych wierzchołków minus długość cyklu.

W pseudokodach przez ruch rozumiemy obiekt posiadający dwie operacje:
- delta(cycle, D, profit)
- apply(cycle)

Oba rodzaje sąsiedztwa zawsze zawierają ruchy InsertMove i DeleteMove.
Różni się tylko ruch wewnątrztrasowy:
- Neighborhood_SwapVertices używa SwapVerticesMove,
- Neighborhood_SwapEdges używa SwapEdgesMove.
```

</details>

<details>
<summary><strong>Move</strong></summary>

```text
Move

Operacja delta(cycle, D, profit)
    Zwróć zmianę wartości funkcji celu po wykonaniu ruchu.

Operacja apply(cycle)
    Zmodyfikuj cycle zgodnie z definicją ruchu.
```

</details>

### Ruchy elementarne

<details>
<summary><strong>InsertMove</strong> — dodanie jednego wierzchołka do cyklu</summary>

```text
InsertMove(v, i)

Dane ruchu:
    v - wierzchołek, który ma zostać wstawiony do cyklu
    i - pozycja, po której wstawiamy v

InsertMove.delta(cycle, D, profit)
    a ← cycle[i]
    b ← cycle[next(i)]

    Δ ← profit[v] - D[a][v] - D[v][b] + D[a][b]

    zwróć Δ

InsertMove.apply(cycle)
    Wstaw v do cycle bezpośrednio po pozycji i.
```

</details>

<details>
<summary><strong>DeleteMove</strong> — usunięcie jednego wierzchołka z cyklu</summary>

```text
DeleteMove(i)

Dane ruchu:
    i - pozycja wierzchołka usuwanego z cyklu

DeleteMove.delta(cycle, D, profit)
    a ← cycle[prev(i)]
    v ← cycle[i]
    b ← cycle[next(i)]

    Δ ← -profit[v] - D[a][b] + D[a][v] + D[v][b]

    zwróć Δ

DeleteMove.apply(cycle)
    Usuń z cycle wierzchołek znajdujący się na pozycji i.
```

</details>

<details>
<summary><strong>SwapVerticesMove</strong> — zamiana dwóch wierzchołków w cyklu</summary>

```text
SwapVerticesMove(i, j)

Dane ruchu:
    i, j - pozycje dwóch wymienianych wierzchołków

SwapVerticesMove.delta(cycle, D, profit)
    jeżeli pozycje i oraz j są sąsiednie
        zwróć DeltaAdjacent(cycle, D, i, j)
    w przeciwnym wypadku
        zwróć DeltaNonAdjacent(cycle, D, i, j)

SwapVerticesMove.apply(cycle)
    Zamień miejscami elementy cycle[i] oraz cycle[j].

DeltaAdjacent(cycle, D, i, j)
    jeżeli next(i) = j
        left ← i
        right ← j
    w przeciwnym wypadku
        left ← j
        right ← i

    a ← cycle[prev(left)]
    vLeft ← cycle[left]
    vRight ← cycle[right]
    b ← cycle[next(right)]

    removed ← D[a][vLeft] + D[vRight][b]
    added   ← D[a][vRight] + D[vLeft][b]

    zwróć removed - added

DeltaNonAdjacent(cycle, D, i, j)
    a  ← cycle[prev(i)]
    vi ← cycle[i]
    b  ← cycle[next(i)]

    c  ← cycle[prev(j)]
    vj ← cycle[j]
    d  ← cycle[next(j)]

    removed ← D[a][vi] + D[vi][b] + D[c][vj] + D[vj][d]
    added   ← D[a][vj] + D[vj][b] + D[c][vi] + D[vi][d]

    zwróć removed - added
```

</details>

<details>
<summary><strong>SwapEdgesMove</strong> — zamiana dwóch niesąsiednich krawędzi</summary>

```text
SwapEdgesMove(i, j)

Dane ruchu:
    i, j - pozycje wyznaczające krawędzie
           (cycle[i], cycle[next(i)]) oraz (cycle[j], cycle[next(j)])

Założenie:
    Rozważamy tylko pary niesąsiednich krawędzi.

SwapEdgesMove.delta(cycle, D, profit)
    a ← cycle[i]
    b ← cycle[next(i)]
    c ← cycle[j]
    d ← cycle[next(j)]

    removed ← D[a][b] + D[c][d]
    added   ← D[a][c] + D[b][d]

    zwróć removed - added

SwapEdgesMove.apply(cycle)
    left ← next(i)
    right ← j

    Odwróć kolejność fragmentu cycle od pozycji left do pozycji right.
```

</details>

### Sąsiedztwa

> [!NOTE]
> Każde sąsiedztwo łączy ruchy zmieniające zbiór wybranych wierzchołków z jednym typem ruchu wewnątrztrasowego.

<details>
<summary><strong>Neighborhood_SwapVertices</strong> — insert/delete + swap wierzchołków</summary>

```text
Neighborhood_SwapVertices(cycle, vertices)

moves ← pusta lista
selected ← tablica logiczna długości |vertices|, początkowo wszędzie false

Dla każdej pozycji p od 0 do |cycle| - 1
    selected[cycle[p]] ← true

Dla każdego wierzchołka vertex z vertices
    v ← vertex.id

    jeżeli selected[v] = false
        dla każdej pozycji i od 0 do |cycle| - 1
            Dodaj InsertMove(v, i) do moves

jeżeli |cycle| > 2
    dla każdej pozycji i od 0 do |cycle| - 1
        Dodaj DeleteMove(i) do moves

jeżeli |cycle| > 2
    dla każdej pozycji i od 0 do |cycle| - 2
        dla każdej pozycji j od i + 1 do |cycle| - 1
            Dodaj SwapVerticesMove(i, j) do moves

zwróć moves
```

</details>

<details>
<summary><strong>Neighborhood_SwapEdges</strong> — insert/delete + swap krawędzi</summary>

```text
Neighborhood_SwapEdges(cycle, vertices)

moves ← pusta lista
selected ← tablica logiczna długości |vertices|, początkowo wszędzie false

Dla każdej pozycji p od 0 do |cycle| - 1
    selected[cycle[p]] ← true

Dla każdego wierzchołka vertex z vertices
    v ← vertex.id

    jeżeli selected[v] = false
        dla każdej pozycji i od 0 do |cycle| - 1
            Dodaj InsertMove(v, i) do moves

jeżeli |cycle| > 2
    dla każdej pozycji i od 0 do |cycle| - 1
        Dodaj DeleteMove(i) do moves

Dla każdej pozycji i od 0 do |cycle| - 2
    dla każdej pozycji j od i + 1 do |cycle| - 1
        jeżeli next(i) = j lub next(j) = i
            Pomiń tę parę
        w przeciwnym wypadku
            Dodaj SwapEdgesMove(i, j) do moves

zwróć moves
```

</details>

### Rozwiązania startowe i heurystyki

<details>
<summary><strong>RandomSolution</strong> — losowe rozwiązanie startowe</summary>

```text
RandomSolution(start, vertices)

    // Losujemy liczbę wierzchołków, które wejdą do rozwiązania.
    k ← losowa liczba całkowita z przedziału [2, |vertices|]

    // Cykl zaczynamy od zadanego wierzchołka startowego.
    cycle ← [start]

    // Tworzymy zbiór wszystkich pozostałych wierzchołków.
    remaining ← wszystkie wierzchołki poza start

    selected_remaining ← losowo wybrane k - 1 różnych wierzchołków z remaining

    // Losujemy ich kolejność w cyklu.
    permute(selected_remaining)

    // Dodajemy je za wierzchołkiem startowym.
    dołącz selected_remaining na koniec cycle

    zwróć cycle
```

</details>

<details>
<summary><strong>GreedyCycle</strong> — konstrukcja rozwiązania heurystycznego</summary>

```text
GreedyCycle(startVertexId, vertices, D, profit)

cycle ← lista zawierająca startVertexId

secondVertex ← wierzchołek różny od startVertexId,
               dla którego D[startVertexId][v] jest najmniejsze
Dodaj secondVertex do cycle

notUsed ← wszystkie wierzchołki różne od startVertexId i secondVertex

dopóki notUsed nie jest puste
    bestVertex ← brak
    bestInsertionPosition ← brak
    bestCost ← +∞

    dla każdego wierzchołka v z notUsed
        vertexBestPosition ← brak
        vertexBestCost ← +∞

        dla każdej krawędzi (a, b) bieżącego cycle
            increase ← D[a][v] + D[v][b] - D[a][b]
            cost ← increase - profit[v]

            jeżeli cost < vertexBestCost
                vertexBestCost ← cost
                vertexBestPosition ← pozycja bezpośrednio po a

        jeżeli vertexBestCost < bestCost
            bestCost ← vertexBestCost
            bestVertex ← v
            bestInsertionPosition ← vertexBestPosition

    Wstaw bestVertex do cycle na pozycji bestInsertionPosition
    Usuń bestVertex z notUsed

zwróć cycle
```

</details>

<details>
<summary><strong>PhaseTwoDelete</strong> — poprawa przez usuwanie nieopłacalnych wierzchołków</summary>

```text
PhaseTwoDelete(cycle, D, profit)

dopóki |cycle| > 2
    bestImprovement ← 0
    bestIndex ← brak

    dla każdej pozycji i od 0 do |cycle| - 1
        a ← cycle[prev(i)]
        v ← cycle[i]
        b ← cycle[next(i)]

        improvement ← D[a][v] + D[v][b] - D[a][b] - profit[v]

        jeżeli improvement > bestImprovement
            bestImprovement ← improvement
            bestIndex ← i

    jeżeli bestImprovement > 0
        Usuń z cycle wierzchołek na pozycji bestIndex
    w przeciwnym wypadku
        przerwij pętlę

zwróć cycle
```

</details>

<details>
<summary><strong>BestHeuristicSolution</strong> — najlepsze rozwiązanie startowe z lab1</summary>

```text
BestHeuristicSolution(startVertexId, vertices, D, profit)

cycle ← GreedyCycle(startVertexId, vertices, D, profit)
cycle ← PhaseTwoDelete(cycle, D, profit)

zwróć cycle
```

</details>

### Algorytmy

> [!IMPORTANT]
> W wersji `greedy` akceptowany jest pierwszy ruch z `Δ > 0`.
> W wersji `steepest` przeglądane są wszystkie ruchy i wybierany jest najlepszy.

<details>
<summary><strong>GreedyLocalSearch</strong> — pierwszy ruch poprawiający</summary>

```text
GreedyLocalSearch(initialSolutionAlgorithm, neighborhoodType, startVertexId, vertices, D, profit, random)

cycle ← initialSolutionAlgorithm(startVertexId, vertices, D, profit, random)

powtarzaj
    improved ← false

    jeżeli neighborhoodType = SWAP_VERTICES
        moves ← Neighborhood_SwapVertices(cycle, vertices)
    w przeciwnym wypadku
        moves ← Neighborhood_SwapEdges(cycle, vertices)

    Losowo przetasuj moves

    dla każdego move z moves
        Δ ← move.delta(cycle, D, profit)

        jeżeli Δ > 0
            move.apply(cycle)
            improved ← true
            przerwij pętlę

dopóki improved = true

zwróć cycle
```

</details>

<details>
<summary><strong>SteepestLocalSearch</strong> — najlepszy ruch poprawiający</summary>

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

</details>

<details>
<summary><strong>RandomWalk</strong> — losowe błądzenie z pamiętaniem najlepszego rozwiązania</summary>

```text
RandomWalk(initialSolutionAlgorithm, neighborhoodType, startVertexId, vertices, D, profit, timeLimit, random)

cycle ← initialSolutionAlgorithm(startVertexId, vertices, D, profit, random)
currentScore ← Evaluate(cycle, D, profit)

bestScore ← currentScore
bestCycle ← kopia cycle

endTime ← bieżący czas + timeLimit

Dopóki bieżący czas < endTime
    jeżeli neighborhoodType = SWAP_VERTICES
        moves ← Neighborhood_SwapVertices(cycle, vertices)
    w przeciwnym wypadku
        moves ← Neighborhood_SwapEdges(cycle, vertices)

    move ← losowo wybrany element z moves
    Δ ← move.delta(cycle, D, profit)

    move.apply(cycle)
    currentScore ← currentScore + Δ

    jeżeli currentScore > bestScore
        bestScore ← currentScore
        bestCycle ← kopia cycle

zwróć bestCycle

Evaluate(cycle, D, profit)
    score ← 0

    dla każdej pozycji i od 0 do |cycle| - 1
        score ← score + profit[cycle[i]]

    dla każdej pozycji i od 0 do |cycle| - 1
        score ← score - D[cycle[i]][cycle[next(i)]]

    zwróć score
```

</details>

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
