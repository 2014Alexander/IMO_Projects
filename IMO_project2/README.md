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

### Notacja

<pre>
<strong>Notacja używana we wszystkich pseudokodach</strong>

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
</pre>

### Move

<pre>
<strong>Move</strong>

<strong>Operacja</strong> delta(cycle, D, profit)
    <strong>Zwróć</strong> zmianę wartości funkcji celu po wykonaniu ruchu.

<strong>Operacja</strong> apply(cycle)
    <strong>Zmodyfikuj</strong> cycle zgodnie z definicją ruchu.
</pre>

### InsertMove

<pre>
<strong>InsertMove(v, i)</strong>

<strong>Dane ruchu:</strong>
    v - wierzchołek, który ma zostać wstawiony do cyklu
    i - pozycja, po której wstawiamy v

InsertMove.delta(cycle, D, profit)
    a ← cycle[i]
    b ← cycle[next(i)]

    Δ ← profit[v] - D[a][v] - D[v][b] + D[a][b]

    <strong>zwróć</strong> Δ

InsertMove.apply(cycle)
    <strong>Wstaw</strong> v do cycle bezpośrednio po pozycji i.
</pre>

### DeleteMove

<pre>
<strong>DeleteMove(i)</strong>

<strong>Dane ruchu:</strong>
    i - pozycja wierzchołka usuwanego z cyklu

DeleteMove.delta(cycle, D, profit)
    a ← cycle[prev(i)]
    v ← cycle[i]
    b ← cycle[next(i)]

    Δ ← -profit[v] - D[a][b] + D[a][v] + D[v][b]

    <strong>zwróć</strong> Δ

DeleteMove.apply(cycle)
    <strong>Usuń</strong> z cycle wierzchołek znajdujący się na pozycji i.
</pre>

### SwapVerticesMove

<pre>
<strong>SwapVerticesMove(i, j)</strong>

<strong>Dane ruchu:</strong>
    i, j - pozycje dwóch wymienianych wierzchołków

SwapVerticesMove.delta(cycle, D, profit)
    <strong>jeżeli</strong> pozycje i oraz j są sąsiednie
        <strong>zwróć</strong> DeltaAdjacent(cycle, D, i, j)
    <strong>w przeciwnym wypadku</strong>
        <strong>zwróć</strong> DeltaNonAdjacent(cycle, D, i, j)

SwapVerticesMove.apply(cycle)
    Zamień miejscami elementy cycle[i] oraz cycle[j].

DeltaAdjacent(cycle, D, i, j)
    <strong>jeżeli</strong> next(i) = j
        left ← i
        right ← j
    <strong>w przeciwnym wypadku</strong>
        left ← j
        right ← i

    a ← cycle[prev(left)]
    vLeft ← cycle[left]
    vRight ← cycle[right]
    b ← cycle[next(right)]

    removed ← D[a][vLeft] + D[vRight][b]
    added   ← D[a][vRight] + D[vLeft][b]

    <strong>zwróć</strong> removed - added

DeltaNonAdjacent(cycle, D, i, j)
    a  ← cycle[prev(i)]
    vi ← cycle[i]
    b  ← cycle[next(i)]

    c  ← cycle[prev(j)]
    vj ← cycle[j]
    d  ← cycle[next(j)]

    removed ← D[a][vi] + D[vi][b] + D[c][vj] + D[vj][d]
    added   ← D[a][vj] + D[vj][b] + D[c][vi] + D[vi][d]

    <strong>zwróć</strong> removed - added
</pre>

### SwapEdgesMove

<pre>
<strong>SwapEdgesMove(i, j)</strong>

<strong>Dane ruchu:</strong>
    i, j - pozycje wyznaczające krawędzie
           (cycle[i], cycle[next(i)]) oraz (cycle[j], cycle[next(j)])

<strong>Założenie:</strong>
    <strong>Rozważamy</strong> tylko pary niesąsiednich krawędzi.

SwapEdgesMove.delta(cycle, D, profit)
    a ← cycle[i]
    b ← cycle[next(i)]
    c ← cycle[j]
    d ← cycle[next(j)]

    removed ← D[a][b] + D[c][d]
    added   ← D[a][c] + D[b][d]

    <strong>zwróć</strong> removed - added

SwapEdgesMove.apply(cycle)
    left ← next(i)
    right ← j

    <strong>Odwróć</strong> kolejność fragmentu cycle od pozycji left do pozycji right.
</pre>

### Neighborhood_SwapVertices

<pre>
<strong>Neighborhood_SwapVertices(cycle, vertices)</strong>

moves ← pusta lista
selected ← tablica logiczna długości |vertices|, początkowo wszędzie false

<strong>Dla każdej pozycji</strong> p od 0 do |cycle| - 1
    selected[cycle[p]] ← true

<strong>Dla każdego wierzchołka</strong> vertex z vertices
    v ← vertex.id

    <strong>jeżeli</strong> selected[v] = false
        <strong>dla każdej pozycji</strong> i od 0 do |cycle| - 1
            <strong>Dodaj</strong> InsertMove(v, i) do moves

<strong>jeżeli</strong> |cycle| &gt; 2
    <strong>dla każdej pozycji</strong> i od 0 do |cycle| - 1
        <strong>Dodaj</strong> DeleteMove(i) do moves

<strong>jeżeli</strong> |cycle| &gt; 2
    <strong>dla każdej pozycji</strong> i od 0 do |cycle| - 2
        <strong>dla każdej pozycji</strong> j od i + 1 do |cycle| - 1
            <strong>Dodaj</strong> SwapVerticesMove(i, j) do moves

<strong>zwróć</strong> moves
</pre>

### Neighborhood_SwapEdges

<pre>
<strong>Neighborhood_SwapEdges(cycle, vertices)</strong>

moves ← pusta lista
selected ← tablica logiczna długości |vertices|, początkowo wszędzie false

<strong>Dla każdej pozycji</strong> p od 0 do |cycle| - 1
    selected[cycle[p]] ← true

<strong>Dla każdego wierzchołka</strong> vertex z vertices
    v ← vertex.id

    <strong>jeżeli</strong> selected[v] = false
        <strong>dla każdej pozycji</strong> i od 0 do |cycle| - 1
            <strong>Dodaj</strong> InsertMove(v, i) do moves

<strong>jeżeli</strong> |cycle| &gt; 2
    <strong>dla każdej pozycji</strong> i od 0 do |cycle| - 1
        <strong>Dodaj</strong> DeleteMove(i) do moves

<strong>Dla każdej pozycji</strong> i od 0 do |cycle| - 2
    <strong>dla każdej pozycji</strong> j od i + 1 do |cycle| - 1
        <strong>jeżeli</strong> next(i) = j lub next(j) = i
            <strong>Pomiń</strong> tę parę
        <strong>w przeciwnym wypadku</strong>
            <strong>Dodaj</strong> SwapEdgesMove(i, j) do moves

<strong>zwróć</strong> moves
</pre>

### RandomSolution

<pre>
<strong>RandomSolution(start, vertices)</strong>

    <em>// Losujemy liczbę wierzchołków, które wejdą do rozwiązania.</em>
    k ← losowa liczba całkowita z przedziału [2, |vertices|]

    <em>// Cykl zaczynamy od zadanego wierzchołka startowego.</em>
    cycle ← [start]

    <em>// Tworzymy zbiór wszystkich pozostałych wierzchołków.</em>
    remaining ← wszystkie wierzchołki poza start

    selected_remaining ← losowo wybrane k - 1 różnych wierzchołków z remaining

    <em>// Losujemy ich kolejność w cyklu.</em>
    <strong>permute</strong>(selected_remaining)

    <em>// Dodajemy je za wierzchołkiem startowym.</em>
    <strong>dołącz</strong> selected_remaining na koniec cycle

    <strong>zwróć</strong> cycle
</pre>

### GreedyCycle

<pre>
<strong>GreedyCycle(startVertexId, vertices, D, profit)</strong>

cycle ← lista zawierająca startVertexId

secondVertex ← wierzchołek różny od startVertexId,
               dla którego D[startVertexId][v] jest najmniejsze
<strong>Dodaj</strong> secondVertex do cycle

notUsed ← wszystkie wierzchołki różne od startVertexId i secondVertex

<strong>dopóki</strong> notUsed nie jest puste
    bestVertex ← brak
    bestInsertionPosition ← brak
    bestCost ← +∞

    <strong>dla każdego wierzchołka</strong> v z notUsed
        vertexBestPosition ← brak
        vertexBestCost ← +∞

        <strong>dla każdej krawędzi</strong> (a, b) bieżącego cycle
            increase ← D[a][v] + D[v][b] - D[a][b]
            cost ← increase - profit[v]

            <strong>jeżeli</strong> cost &lt; vertexBestCost
                vertexBestCost ← cost
                vertexBestPosition ← pozycja bezpośrednio po a

        <strong>jeżeli</strong> vertexBestCost &lt; bestCost
            bestCost ← vertexBestCost
            bestVertex ← v
            bestInsertionPosition ← vertexBestPosition

    <strong>Wstaw</strong> bestVertex do cycle na pozycji bestInsertionPosition
    <strong>Usuń</strong> bestVertex z notUsed

<strong>zwróć</strong> cycle
</pre>

### PhaseTwoDelete

<pre>
<strong>PhaseTwoDelete(cycle, D, profit)</strong>

<strong>dopóki</strong> |cycle| &gt; 2
    bestImprovement ← 0
    bestIndex ← brak

    <strong>dla każdej pozycji</strong> i od 0 do |cycle| - 1
        a ← cycle[prev(i)]
        v ← cycle[i]
        b ← cycle[next(i)]

        improvement ← D[a][v] + D[v][b] - D[a][b] - profit[v]

        <strong>jeżeli</strong> improvement &gt; bestImprovement
            bestImprovement ← improvement
            bestIndex ← i

    <strong>jeżeli</strong> bestImprovement &gt; 0
        <strong>Usuń</strong> z cycle wierzchołek na pozycji bestIndex
    <strong>w przeciwnym wypadku</strong>
        <strong>przerwij pętlę</strong>

<strong>zwróć</strong> cycle
</pre>

### BestHeuristicSolution

<pre>
<strong>BestHeuristicSolution(startVertexId, vertices, D, profit)</strong>

cycle ← GreedyCycle(startVertexId, vertices, D, profit)
cycle ← PhaseTwoDelete(cycle, D, profit)

<strong>zwróć</strong> cycle
</pre>

### GreedyLocalSearch

<pre>
<strong>GreedyLocalSearch(initialSolutionAlgorithm, neighborhoodType, startVertexId, vertices, D, profit, random)</strong>

cycle ← initialSolutionAlgorithm(startVertexId, vertices, D, profit, random)

<strong>powtarzaj</strong>
    improved ← false

    <strong>jeżeli</strong> neighborhoodType = SWAP_VERTICES
        moves ← Neighborhood_SwapVertices(cycle, vertices)
    <strong>w przeciwnym wypadku</strong>
        moves ← Neighborhood_SwapEdges(cycle, vertices)

    <strong>Losowo przetasuj</strong> moves

    <strong>dla każdego move</strong> z moves
        Δ ← move.delta(cycle, D, profit)

        <strong>jeżeli</strong> Δ &gt; 0
            move.apply(cycle)
            improved ← true
            <strong>przerwij pętlę</strong>

<strong>dopóki</strong> improved = true

<strong>zwróć</strong> cycle
</pre>

### SteepestLocalSearch

<pre>
<strong>SteepestLocalSearch(initialSolutionAlgorithm, neighborhoodType, startVertexId, vertices, D, profit)</strong>

cycle ← initialSolutionAlgorithm(startVertexId, vertices, D, profit)

<strong>powtarzaj</strong>
    improved ← false
    bestMove ← brak
    bestDelta ← 0

    <strong>jeżeli</strong> neighborhoodType = SWAP_VERTICES
        moves ← Neighborhood_SwapVertices(cycle, vertices)
    <strong>w przeciwnym wypadku</strong>
        moves ← Neighborhood_SwapEdges(cycle, vertices)

    <strong>dla każdego move</strong> z moves
        Δ ← move.delta(cycle, D, profit)

        <strong>jeżeli</strong> Δ &gt; bestDelta
            bestDelta ← Δ
            bestMove ← move

    <strong>jeżeli</strong> bestMove ≠ brak
        bestMove.apply(cycle)
        improved ← true

<strong>dopóki</strong> improved = true

<strong>zwróć</strong> cycle
</pre>

### RandomWalk

<pre>
<strong>RandomWalk(initialSolutionAlgorithm, neighborhoodType, startVertexId, vertices, D, profit, timeLimit, random)</strong>

cycle ← initialSolutionAlgorithm(startVertexId, vertices, D, profit, random)
currentScore ← Evaluate(cycle, D, profit)

bestScore ← currentScore
bestCycle ← kopia cycle

endTime ← bieżący czas + timeLimit

<strong>Dopóki</strong> bieżący czas &lt; endTime
    <strong>jeżeli</strong> neighborhoodType = SWAP_VERTICES
        moves ← Neighborhood_SwapVertices(cycle, vertices)
    <strong>w przeciwnym wypadku</strong>
        moves ← Neighborhood_SwapEdges(cycle, vertices)

    move ← losowo wybrany element z moves
    Δ ← move.delta(cycle, D, profit)

    move.apply(cycle)
    currentScore ← currentScore + Δ

    <strong>jeżeli</strong> currentScore &gt; bestScore
        bestScore ← currentScore
        bestCycle ← kopia cycle

<strong>zwróć</strong> bestCycle

Evaluate(cycle, D, profit)
    score ← 0

    <strong>dla każdej pozycji</strong> i od 0 do |cycle| - 1
        score ← score + profit[cycle[i]]

    <strong>dla każdej pozycji</strong> i od 0 do |cycle| - 1
        score ← score - D[cycle[i]][cycle[next(i)]]

    <strong>zwróć</strong> score
</pre>

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
