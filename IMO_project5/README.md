# Zadanie 5 i Zadanie 6. Testy globalnej wypukłości oraz hybrydowy algorytm ewolucyjny

Kod programu: https://github.com/2014Alexander/IMO_Projects/tree/main/IMO_project5

## 1. Opis zadania

Oba zadania dotyczą zmodyfikowanego problemu komiwojażera z zyskami.
Rozwiązanie jest zamkniętym cyklem przechodzącym przez wybrany podzbiór wierzchołków.
Jakość rozwiązania liczono tak samo jak w poprzednich laboratoriach:

```text
f(C) = suma profitów odwiedzonych wierzchołków - długość zamkniętego cyklu
```

W zadaniu 5 wykonano test globalnej wypukłości.
Dla każdej instancji wygenerowano 1000 lokalnych optimów z losowych rozwiązań startowych po zachłannym lokalnym przeszukiwaniu.
Następnie każde lokalne optimum porównano z bardzo dobrym rozwiązaniem oraz ze wszystkimi pozostałymi lokalnymi optimami.
Podobieństwo liczono dwiema miarami: liczbą wspólnych wybranych wierzchołków oraz liczbą wspólnych krawędzi cyklu.

W zadaniu 6 zaimplementowano hybrydowy algorytm ewolucyjny HAE.
Algorytm pracuje w trybie steady state, używa populacji elitarnej o rozmiarze 20 i tworzy potomków przez rekombinację dwóch rodziców.
Porównano pięć wariantów HAE z metodami MSLS, ILS i LNS z poprzedniego zadania.
Dodatkowo podano wyniki heurystyki zachłannej używanej w LNS i HAE oraz wynik bazowego lokalnego przeszukiwania.

## 2. Zastosowane metody i konfiguracja

W zadaniu 5 lokalne optima generowano przez `RandomSolution` oraz `GreedyLocalSearch` z sąsiedztwem `SWAP_EDGES`.
Sąsiedztwo to obejmuje ruchy wstawiania, usuwania oraz wymiany krawędzi cyklu.
Bardzo dobre rozwiązanie `B` wybierano automatycznie jako najlepsze rozwiązanie znalezione przez metody z etapu lab4: MSLS, ILS, LNS i LNSa.

W zadaniu 6 jako bazowe lokalne przeszukiwanie dla HAE użyto `SteepestLocalSearchWithCandidateMoves`, czyli tej samej procedury, która była używana w lab4 dla MSLS, ILS i LNS.
Repair w HAE wykonuje `TwoRegretRepairOperator`, a następnie `PhaseTwoDelete`.
Jeżeli rekombinacja zwróci zbyt krótki cykl częściowy, przed repair działa krótki etap `RepairSeedAdapter`, który tworzy minimalny seed potrzebny do działania 2-regret repair.

Porównano następujące główne metody:

```text
MSLS
ILS
LNS
HAE_OP1_LS
HAE_OP2_LS
HAE_OP2_NO_LS
HAE_OP3_LS
HAE_OP3_NO_LS
```

Warianty `NO_LS` oznaczają brak lokalnego przeszukiwania po rekombinacji i repair w głównej pętli HAE.
Populacja początkowa HAE zawsze jest tworzona z użyciem lokalnego przeszukiwania.

Jako wyniki referencyjne podano:

```text
2REGRET_P2D
BASE_LS
```

`2REGRET_P2D` oznacza heurystykę 2-regret z końcową fazą `PhaseTwoDelete`.
`BASE_LS` oznacza pojedyncze lokalne przeszukiwanie uruchomione z losowego rozwiązania startowego.

## 3. Opis zaimplementowanych algorytmów w pseudokodzie

Pseudokody pokazują główną logikę nowych części zadań 5 i 6. Nie przepisują klas jeden do jednego, ale zachowują decyzje algorytmiczne istotne dla wyników eksperymentu.

Wszystkie metody korzystają z tej samej funkcji celu:

```text
f(C) = suma profitów odwiedzonych wierzchołków -
       długość zamkniętego cyklu
```

Pseudokody są zapisane dla jednej ustalonej instancji problemu. Instancja zawiera macierz odległości, profity wierzchołków i liczbę wierzchołków. Dla czytelności `instance` występuje tylko w procedurach najwyższego poziomu, a w pomocniczych procedurach jest traktowana jako wspólny kontekst.

### 3.1 Zadanie 5. Testy globalnej wypukłości

#### Generowanie lokalnych optimów

> Generuje 1000 lokalnych optimów z losowych rozwiązań startowych.
> Używa `GreedyLocalSearch`, ponieważ w zadaniu 5 lokalne przeszukiwanie jest zachłanne.
> Traktuje `SWAP_EDGES` jako wariant lokalnego przeszukiwania z wymianą krawędzi.
> Obejmuje ruchy wstawiania, usuwania oraz odwracania fragmentu cyklu.

```text
GenerateLocalOptima(instance, localOptimaCount = 1000)
    localOptima <- pusta lista
    runs <- przygotuj localOptimaCount konfiguracji uruchomień

    dla każdej konfiguracji run z runs
        x <- RandomSolution(run.runSeed).solve(instance, run.startVertexId)
        x <- GreedyLocalSearch(SWAP_EDGES, run.runSeed).improve(x)
        objective <- f(x)
        dodaj (x, objective, run.startVertexId, run.runSeed) do localOptima

    zwróć localOptima
```

#### Wybór bardzo dobrego rozwiązania B

> Wyznacza bardzo dobre rozwiązanie `B` bez ręcznego wyboru wyniku.
> Wybiera `B` jako najlepsze rozwiązanie znalezione przez metody z poprzedniego etapu.
> Uruchamia MSLS i z jego średniego czasu wyznacza limit dla ILS, LNS i LNSa.

```text
SelectVeryGoodSolution(instance)
    runs <- przygotuj 20 konfiguracji uruchomień jak w lab4

    mslsResults <- uruchom MSLS dla wszystkich runs
    mslsAverageTime <- średni czas działania MSLS
    best <- najlepszy wynik z mslsResults

    dla każdej metody method z listy ILS, LNS, LNSa
        results <- uruchom method dla wszystkich runs
                    z limitem czasu mslsAverageTime
        candidate <- najlepszy wynik z results

        jeżeli f(candidate) > f(best)
            best <- candidate

    B <- best
    zapisz nazwę metody, seed, startVertexId i objective rozwiązania B

    zwróć B
```

#### Cechy rozwiązania

> Zamienia cykl na cechy potrzebne do porównywania rozwiązań.
> Zapamiętuje wybrane wierzchołki oraz krawędzie cyklu.
> Traktuje krawędzie jako nieskierowane, więc krawędź a-b i b-a oznacza to samo.
> Uwzględnia przejście z ostatniego wierzchołka do pierwszego jako zwykłą krawędź cyklu.

```text
SolutionFeatures(cycle)
    selectedVertices <- tablica obecności wierzchołków w cycle
    edges <- pusta tablica krawędzi

    dla każdej pozycji w cycle
        a <- aktualny wierzchołek
        b <- następny wierzchołek w cycle,
             z przejściem z końca na początek
        dodaj krawędź (min(a, b), max(a, b)) do edges

    posortuj edges i usuń powtórzenia

    zwróć (selectedVertices, edges)
```

#### Podobieństwo po wierzchołkach

> Liczy, ile wybranych wierzchołków występuje jednocześnie w obu rozwiązaniach.

```text
CommonVerticesSimilarity(featuresA, featuresB)
    count <- 0

    dla każdego wierzchołka v instancji
        jeżeli v jest wybrany w featuresA oraz w featuresB
            count <- count + 1

    zwróć count
```

#### Podobieństwo po krawędziach

> Liczy, ile krawędzi cyklu występuje jednocześnie w obu rozwiązaniach.
> Zapisuje każdą krawędź jako nieskierowaną parę (min(a,b), max(a,b)).
> Wykorzystuje posortowanie list krawędzi zamiast porównywania każdej krawędzi z każdą.
> Przesuwa równolegle oba wskaźniki po listach krawędzi.
> Rozpoznaje równe aktualne krawędzie jako wspólną krawędź.
> Pomija mniejszą aktualną krawędź, ponieważ nie może pojawić się później w drugiej liście.

```text
CommonEdgesSimilarity(featuresA, featuresB)
    edgesA <- posortowane krawędzie nieskierowane rozwiązania A
    edgesB <- posortowane krawędzie nieskierowane rozwiązania B
    count <- 0

    dopóki w obu listach są jeszcze nieprzejrzane krawędzie
        edgeA <- aktualna nieprzejrzana krawędź z edgesA
        edgeB <- aktualna nieprzejrzana krawędź z edgesB

        jeżeli edgeA = edgeB
            count <- count + 1
            przejdź do następnej krawędzi w obu listach

        w przeciwnym razie jeżeli edgeA < edgeB
            przejdź do następnej krawędzi w edgesA

        w przeciwnym razie
            przejdź do następnej krawędzi w edgesB

    zwróć count
```

#### Liczenie punktów do wykresów i korelacji

> Liczy dla każdego lokalnego optimum podobieństwo do bardzo dobrego rozwiązania `B`.
> Liczy dla każdego lokalnego optimum średnie podobieństwo do pozostałych lokalnych optimów.
> Wyznacza korelacje między wartością funkcji celu a każdą wartością podobieństwa.
> Liczy korelacje wyłącznie dla zbioru lokalnych optimów.

```text
Lab5Experiment(instance)
    B <- SelectVeryGoodSolution(instance)
    localOptima <- GenerateLocalOptima(instance, 1000)

    bestFeatures <- SolutionFeatures(B.cycle)
    dla każdego lokalnego optimum x_i
        features[i] <- SolutionFeatures(x_i.cycle)

    dla każdego lokalnego optimum x_i
        similarityVerticesToBest[i] <-
            CommonVerticesSimilarity(features[i], bestFeatures)
        similarityEdgesToBest[i] <-
            CommonEdgesSimilarity(features[i], bestFeatures)

    dla każdej pary lokalnych optimów (x_i, x_j), gdzie i < j
        vertices <- CommonVerticesSimilarity(features[i], features[j])
        edges <- CommonEdgesSimilarity(features[i], features[j])

        dodaj vertices do sumy podobieństwa wierzchołków dla i oraz j
        dodaj edges do sumy podobieństwa krawędzi dla i oraz j

    dla każdego lokalnego optimum x_i
        avgVerticesToOthers[i] <- sumVertices[i] / 999
        avgEdgesToOthers[i] <- sumEdges[i] / 999

    policz korelacje Pearsona:
        f(x_i) z similarityVerticesToBest[i]
        f(x_i) z similarityEdgesToBest[i]
        f(x_i) z avgVerticesToOthers[i]
        f(x_i) z avgEdgesToOthers[i]

    zapisz punkty do wykresów, korelacje i metadane rozwiązania B
```

Rozwiązanie `B` służy tylko jako punkt odniesienia przy liczeniu podobieństwa. Nie jest dodawane do zbioru 1000 lokalnych optimów i nie bierze udziału w średnim podobieństwie między lokalnymi optimami.

### 3.2 Zadanie 6. Hybrydowy algorytm ewolucyjny

#### Inicjalizacja populacji początkowej

> Buduje populację początkową z lokalnych optimów.
> Tworzy każdego osobnika z losowego rozwiązania startowego poprawionego lokalnym przeszukiwaniem.
> Przesuwa starty deterministycznie jak w MSLS, żeby nie wymuszać stale tego samego wierzchołka.
> Używa tej samej polityki różnorodności przy inicjalizacji i przy późniejszym dodawaniu potomków.

```text
PopulationInitializer(instance, startVertexId, populationSize = 20, runSeed)
    randomSolution <- RandomSolution(runSeed)
    population <- pusta populacja elitarna
    attempts <- 0
    maxAttempts <- populationSize * 1000

    dopóki population.size < populationSize oraz attempts < maxAttempts
        currentStart <- (startVertexId + attempts) mod instance.size
        x <- randomSolution.solve(instance, currentStart)
        x <- SteepestLocalSearchWithCandidateMoves(x)
        objective <- f(x)
        features <- SolutionFeatures(x)

        jeżeli x jest różne od obecnej populacji według PopulationDiversityPolicy
            dodaj osobnika (x, objective, features) do population

        attempts <- attempts + 1

    jeżeli population.size < populationSize
        zgłoś błąd inicjalizacji populacji

    zwróć population
```

#### Hybrydowy algorytm ewolucyjny HAE

> Prowadzi algorytm ewolucyjny w trybie steady state.
> Tworzy w jednej iteracji jednego potomka z dwóch losowo wybranych rodziców.
> Używa rekombinacji do utworzenia surowego partial cycle.
> Uzupełnia potomka przez repair.
> Uruchamia local search po repair tylko w wariantach z lokalnym przeszukiwaniem.
> Obejmuje limitem czasu także inicjalizację populacji.

```text
HAE(instance, startVertexId, runSeed, timeLimit,
    recombinationOperator, useLocalSearch)

    random <- Random(runSeed)
    endTime <- aktualny czas + timeLimit
    population <- PopulationInitializer(instance, startVertexId, 20, runSeed)
    iterations <- 0

    dopóki aktualny czas < endTime
        parent1, parent2 <- wylosuj dwa różne osobniki z population

        child <- recombinationOperator(parent1, parent2, random)
        child <- RepairStage(child, parent1, parent2)

        jeżeli useLocalSearch
            child <- SteepestLocalSearchWithCandidateMoves(child)

        childObjective <- f(child)
        iterations <- iterations + 1

        jeżeli childObjective nie jest lepszy od najgorszego osobnika w population
            pomiń child
        w przeciwnym razie
            childFeatures <- SolutionFeatures(child)
            childIndividual <- (child, childObjective, childFeatures)

            jeżeli childIndividual jest różny od population według PopulationDiversityPolicy
                dodaj childIndividual do population
                usuń najgorszego osobnika

    zwróć najlepszy osobnik z population
```


#### Polityka różnorodności populacji

> Nie dopuszcza kopii rozwiązań w populacji.
> Traktuje w podstawowej wersji dwa rozwiązania z tym samym objective jako kopie.

```text
ObjectiveDiversityPolicy(candidate, population)
    dla każdego osobnika x w population
        jeżeli f(candidate) = f(x)
            zwróć false

    zwróć true
```

#### Etap naprawy potomka

> Oddziela techniczne przygotowanie wejścia dla repair od semantyki rekombinacji.
> Przyjmuje, że rekombinacja może zwrócić pusty albo jednoelementowy partial cycle.
> Zapewnia przez `RepairSeedAdapter` minimalny rozmiar wejścia dla 2-regret repair.
> Wykonuje właściwy repair metodą 2-regret.

```text
RepairStage(partialChild, parent1, parent2)
    jeżeli partialChild ma mniej niż 2 wierzchołki
        uzupełnij partialChild pierwszymi brakującymi wierzchołkami
        z parent1, a potem z parent2

    child <- TwoRegretRepairOperator(partialChild)

    zwróć child
```

`TwoRegretRepairOperator` uzupełnia cykl metodą 2-regret i na końcu wykonuje `PhaseTwoDelete`.

#### Operator 1: wspólne części rodziców

> Zachowuje części trasy wspólne dla obu rodziców.
> Buduje wielowierzchołkowe fragmenty ze wspólnych krawędzi.
> Zachowuje wspólne wierzchołki bez wspólnej krawędzi jako części jednoelementowe.

```text
CommonPartsRecombination(parent1, parent2)
    commonEdges <- krawędzie parent1, które występują też w parent2
    edgeFragments <- podścieżki zbudowane ze wspólnych krawędzi

    used <- wierzchołki należące do edgeFragments
    parts <- edgeFragments

    dla każdego wspólnego wierzchołka v rodziców parent1 i parent2
        jeżeli v nie należy do used
            dodaj v jako jednoelementową część do parts

    losowo ustaw kolejność części w parts
    dla każdej części losowo zdecyduj, czy odwrócić jej kierunek
    child <- połącz części w jeden partial cycle

    zwróć child
```

#### Operator 2: wspólne wierzchołki i krawędzie po filtracji

> Zaczyna od jednego rodzica i zostawia tylko strukturę potwierdzoną przez drugiego rodzica.
> Usuwa najpierw wierzchołki nieobecne w drugim rodzicu.
> Łączy sąsiadów, którzy stają się sąsiadami po filtracji sekwencji.
> Sprawdza dopiero na nowych połączeniach, które krawędzie występują także w drugim rodzicu.
> Buduje fragmenty potomka z zachowanych krawędzi po filtracji.
> Zwraca krótką sekwencję jako raw partial cycle dla `RepairStage`, jeżeli po filtracji zostało mniej niż 2 wierzchołki.

```text
CommonEdgesAndVerticesRecombination(parent1, parent2)
    base <- losowo wybrany rodzic, którego kolejność będzie filtrowana
    filter <- drugi rodzic

    filtered <- pusta sekwencja
    dla każdego wierzchołka v w cyklu base
        jeżeli v występuje w filter
            dodaj v do filtered

    jeżeli filtered ma mniej niż 2 wierzchołki
        zwróć filtered jako partial cycle

    retainedEdges <- pusta tablica zachowanych połączeń
    dla każdej krawędzi a-b w cyklu utworzonym przez filtered
        jeżeli krawędź a-b występuje w filter
            oznacz połączenie a-b jako zachowane

    fragments <- podścieżki zbudowane z zachowanych połączeń
    losowo ustaw kolejność fragments
    dla każdego fragmentu losowo zdecyduj, czy odwrócić jego kierunek
    child <- połącz fragments w jeden partial cycle

    zwróć child
```

#### Operator 3: wspólne wierzchołki

> Zachowuje tylko wierzchołki wspólne dla obu rodziców.
> Bierze kolejność zachowanych wierzchołków z losowo wybranego rodzica bazowego.
> Nie usuwa krawędzi tylko dlatego, że nie występowały w drugim rodzicu.

```text
CommonVerticesRecombination(parent1, parent2)
    base <- losowo wybrany rodzic z pary parent1, parent2
    filter <- drugi rodzic

    child <- pusty partial cycle
    dla każdego wierzchołka v w cyklu base
        jeżeli v występuje w filter
            dodaj v do child

    zwróć child
```

Po usunięciu pozostałych wierzchołków sąsiedzi w przefiltrowanej sekwencji stają się połączeni. To odróżnia operator 3 od samego wyboru zbioru wspólnych wierzchołków.

#### Warianty HAE użyte w eksperymencie

```text
HAE_OP1_LS     = HAE z CommonPartsRecombination i local search po repair
HAE_OP2_LS     = HAE z CommonEdgesAndVerticesRecombination i local search po repair
HAE_OP2_NO_LS  = HAE z CommonEdgesAndVerticesRecombination bez local search po repair
HAE_OP3_LS     = HAE z CommonVerticesRecombination i local search po repair
HAE_OP3_NO_LS  = HAE z CommonVerticesRecombination bez local search po repair
```

We wszystkich wariantach populacja początkowa jest poprawiana lokalnym przeszukiwaniem. Wariant `NO_LS` oznacza tylko brak lokalnego przeszukiwania po rekombinacji i repair w głównej pętli HAE.

#### Metody referencyjne w zadaniu 6

> Podaje wyniki heurystyki zachłannej używanej w LNS i HAE.
> Wykonuje konstrukcję 2-regret z końcowym usuwaniem nieopłacalnych wierzchołków.

```text
2REGRET_P2D(instance, startVertexId)
    x <- TwoRegretCost(instance, startVertexId)
    x <- PhaseTwoDelete(x)

    zwróć x
```

> Podaje wynik pojedynczego bazowego lokalnego przeszukiwania.
> Zaczyna od losowego rozwiązania startowego.
> Używa tego samego `SteepestLocalSearchWithCandidateMoves` co MSLS, ILS, LNS i HAE.

```text
BASE_LS(instance, startVertexId, runSeed)
    x <- RandomSolution(runSeed).solve(instance, startVertexId)
    x <- SteepestLocalSearchWithCandidateMoves(x)

    zwróć x
```

## 4. Sposób przeprowadzenia eksperymentu

Eksperymenty wykonano dla instancji TSPA oraz TSPB.
Wyniki funkcji celu, czasu działania i liczby iteracji są podawane osobno dla każdej instancji.

W zadaniu 5 dla każdej instancji wygenerowano 1000 lokalnych optimów.
Dla każdego lokalnego optimum zapisano wartość funkcji celu, podobieństwo do bardzo dobrego rozwiązania `B` oraz średnie podobieństwo do pozostałych lokalnych optimów.
Współczynniki korelacji policzono osobno dla podobieństwa po wierzchołkach i po krawędziach.
Rozwiązanie `B` nie wchodzi do zbioru 1000 lokalnych optimów i nie jest uwzględniane przy korelacji.

W zadaniu 6 każdą metodę główną uruchomiono 20 razy.
Dla metod ILS, LNS i HAE warunkiem stopu był limit czasu równy średniemu czasowi jednego uruchomienia MSLS na tej samej instancji.
Czas HAE obejmuje także inicjalizację populacji początkowej.

Liczba iteracji oznacza:

```text
MSLS: 200 startów lokalnego przeszukiwania
ILS: liczba perturbacji
LNS: liczba cykli destroy-repair
HAE: liczba prób utworzenia i oceny potomka
```

Dla metod referencyjnych liczba iteracji nie jest podawana, ponieważ nie są one iteracyjnymi metaheurystykami w sensie MSLS, ILS, LNS i HAE.

## 5. Wyniki zadania 5

Poniżej pokazano ustawienia eksperymentu oraz informacje o bardzo dobrym rozwiązaniu `B`, które było punktem odniesienia przy liczeniu podobieństwa.

![Ustawienia i bardzo dobre rozwiązanie B.](images/lab5/00_lab5_ustawienia_i_bardzo_dobre_rozwiazanie.png)

Dla każdej instancji pokazano zależność wartości funkcji celu od podobieństwa lokalnego optimum do rozwiązania `B` oraz od średniego podobieństwa do pozostałych lokalnych optimów.

### TSPA

![TSPA - podobieństwo po wierzchołkach.](images/lab5/01_tspa_wierzcholki.png)

![TSPA - podobieństwo po krawędziach.](images/lab5/02_tspa_krawedzie.png)

### TSPB

![TSPB - podobieństwo po wierzchołkach.](images/lab5/01_tspb_wierzcholki.png)

![TSPB - podobieństwo po krawędziach.](images/lab5/02_tspb_krawedzie.png)

Współczynniki korelacji zestawiono w osobnej tabeli.

![Korelacje dla zadania 5.](images/lab5/05_lab5_korelacje.png)

## 6. Wyniki zadania 6

W komórkach tabel podano średnią oraz zakres od minimum do maksimum.
Wyniki głównego porównania i wyniki referencyjne są rozdzielone w tabelach.

![Tabela wartości funkcji celu.](images/lab6/01_lab6_tabela_funkcja_celu.png)

![Tabela czasu działania.](images/lab6/02_lab6_tabela_czas_dzialania.png)

![Tabela liczby iteracji.](images/lab6/03_lab6_tabela_liczba_iteracji.png)


## 7. Wnioski

W zadaniu 5 dla obu instancji otrzymano dodatnią korelację między wartością funkcji celu a podobieństwem lokalnych optimów. Oznacza to, że lepsze lokalne optima częściej miały wspólne wierzchołki i krawędzie z bardzo dobrym rozwiązaniem B oraz z innymi lokalnymi optimami. Dobre rozwiązania nie były więc całkowicie rozproszone po przestrzeni rozwiązań, tylko tworzyły zauważalne skupienia podobnych struktur.

Zależność była znacznie silniejsza dla TSPB niż dla TSPA. Dla TSPB korelacje wynosiły od 0.793 do 0.870, a dla TSPA od 0.532 do 0.662. Można to interpretować tak, że w TSPB dobre lokalne optima były bardziej podobne do siebie i do rozwiązania referencyjnego. W TSPA dobre wyniki można było uzyskać przy większej różnorodności cykli.

W obu instancjach podobieństwo po krawędziach dawało silniejszy sygnał niż podobieństwo po samych wierzchołkach. Dla TSPA korelacja z podobieństwem do B wzrosła z 0.558 dla wierzchołków do 0.662 dla krawędzi, a dla średniego podobieństwa do pozostałych optimów z 0.532 do 0.619. Dla TSPB najmocniejszy wynik także dotyczył średniego podobieństwa po krawędziach i wyniósł 0.870. Jest to zgodne z charakterem problemu, bo jakość rozwiązania zależy nie tylko od wyboru wierzchołków, ale też od sposobu połączenia ich w cyklu.

Bardzo dobre rozwiązanie B zostało w obu instancjach znalezione przez ILS. Dla TSPA miało wartość funkcji celu 8624, a dla TSPB 20213. To pokazuje, że ILS był bardzo mocnym źródłem pojedynczych dobrych rozwiązań. W dalszym porównaniu średnich wyników sytuacja była jednak różna: na TSPA najwyższą średnią uzyskał LNS, a na TSPB ILS.

W zadaniu 6 najlepsze średnie wyniki w głównym porównaniu uzyskały metody z poprzedniego etapu. Na TSPA najlepszy był LNS ze średnią 8360.850, a na TSPB najlepszy był ILS ze średnią 20050.900. HAE nie przebił najlepszej metody dla żadnej instancji, ale najlepszy wariant HAE był wyraźnie lepszy od MSLS i bliski najlepszym metodom perturbacyjnym.

Najlepszym wariantem HAE w obu instancjach był HAE_OP1_LS. Na TSPA osiągnął średnią 7968.850, czyli był lepszy od MSLS o około 392 punkty, ale słabszy od LNS także o około 392 punkty. Na TSPB osiągnął średnią 19841.150, czyli był lepszy od MSLS o około 648 punktów i słabszy od ILS tylko o około 210 punktów. Na TSPB HAE_OP1_LS był też lepszy od LNS.

Operator 1 okazał się najstabilniejszym operatorem rekombinacji w naszych wynikach. Ten operator zachowuje wspólne krawędzie i wspólne wierzchołki rodziców, więc przekazuje potomkowi najbardziej bezpośrednią część wspólnej struktury dobrych rozwiązań. Wyniki sugerują, że dla tej konfiguracji bardziej opłacało się zachowywać potwierdzone fragmenty rodziców niż silniej filtrować rozwiązanie i liczyć głównie na późniejszy repair.

Lokalne przeszukiwanie po repair miało duże znaczenie dla jakości HAE. Najlepiej widać to dla operatora 2: na TSPA wersja z local search osiągnęła 7847.350, a wersja bez local search 7063.850, czyli różnica wyniosła około 784 punkty. Na TSPB różnica była jeszcze większa: 19793.950 wobec 18707.300, czyli około 1087 punktów. Repair 2-regret z PhaseTwoDelete odbudowywał poprawny cykl, ale bez lokalnego przeszukiwania jakość potomka po rekombinacji była wyraźnie słabsza.

Dla operatora 3 wpływ local search był mniejszy. Na TSPA różnica między HAE_OP3_LS i HAE_OP3_NO_LS wyniosła około 80 punktów, a na TSPB około 135 punktów. Operator 3 zachowuje tylko wspólne wierzchołki w kolejności jednego rodzica, więc tworzy prostszy partial cycle i traci więcej informacji o krawędziach. Taki potomek jest łatwiejszy do naprawienia, ale ma mniejszy potencjał jakościowy.

Liczba iteracji nie była dobrym samodzielnym wskaźnikiem jakości metody. HAE_OP3_NO_LS wykonywał najwięcej iteracji: średnio 1509.450 na TSPA i 2293.950 na TSPB, ale nie dawał najlepszych wyników. Z kolei HAE_OP1_LS wykonywał dużo mniej iteracji, odpowiednio 304.400 i 457.800, a mimo to był najlepszym wariantem HAE. Ważniejsza była jakość tworzonego potomka niż sama liczba prób wykonanych w limicie czasu.

Metody referencyjne 2REGRET_P2D i BASE_LS osiągnęły wyniki wyraźnie słabsze od głównych metaheurystyk. Na TSPA 2REGRET_P2D uzyskał średnio 6065.250, a BASE_LS 5835.650. Na TSPB było to odpowiednio 18236.550 i 16800.650. Oznacza to, że pojedyncza heurystyka konstrukcyjna albo pojedyncze lokalne przeszukiwanie z losowego startu nie wystarczały do uzyskania jakości porównywalnej z MSLS, ILS, LNS i najlepszymi wariantami HAE.

Ogólny wynik zadania 6 pokazuje kompromis między siłą pojedynczej próby a liczbą prób w tym samym czasie. Warianty bez local search wykonywały więcej iteracji, ale traciły jakość. Warianty z local search wykonywały mniej iteracji, ale dawały lepsze potomki. Najlepsze wyniki HAE uzyskano wtedy, gdy rekombinacja zachowywała wartościową strukturę rodziców, a local search poprawiał rozwiązanie po repair.

## 8. Wizualizacje najlepszych rozwiązań

![TSPA - MSLS.](images/lab6/best_solutions/main/tspa_msls.png)

![TSPA - ILS.](images/lab6/best_solutions/main/tspa_ils.png)

![TSPA - LNS.](images/lab6/best_solutions/main/tspa_lns.png)

![TSPA - HAE OP1 LS.](images/lab6/best_solutions/main/tspa_hae_op1_ls.png)

![TSPA - HAE OP2 LS.](images/lab6/best_solutions/main/tspa_hae_op2_ls.png)

![TSPA - HAE OP2 NO LS.](images/lab6/best_solutions/main/tspa_hae_op2_no_ls.png)

![TSPA - HAE OP3 LS.](images/lab6/best_solutions/main/tspa_hae_op3_ls.png)

![TSPA - HAE OP3 NO LS.](images/lab6/best_solutions/main/tspa_hae_op3_no_ls.png)

![TSPB - MSLS.](images/lab6/best_solutions/main/tspb_msls.png)

![TSPB - ILS.](images/lab6/best_solutions/main/tspb_ils.png)

![TSPB - LNS.](images/lab6/best_solutions/main/tspb_lns.png)

![TSPB - HAE OP1 LS.](images/lab6/best_solutions/main/tspb_hae_op1_ls.png)

![TSPB - HAE OP2 LS.](images/lab6/best_solutions/main/tspb_hae_op2_ls.png)

![TSPB - HAE OP2 NO LS.](images/lab6/best_solutions/main/tspb_hae_op2_no_ls.png)

![TSPB - HAE OP3 LS.](images/lab6/best_solutions/main/tspb_hae_op3_ls.png)

![TSPB - HAE OP3 NO LS.](images/lab6/best_solutions/main/tspb_hae_op3_no_ls.png)

