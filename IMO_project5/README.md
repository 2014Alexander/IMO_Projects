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
f(C) = \mbox{suma profitów odwiedzonych wierzchołków} -
       \mbox{długość zamkniętego cyklu}
```

Pseudokody są zapisane dla jednej ustalonej instancji problemu. Instancja zawiera macierz odległości, profity wierzchołków i liczbę wierzchołków. Dla czytelności `instance` występuje tylko w procedurach najwyższego poziomu, a w pomocniczych procedurach jest traktowana jako wspólny kontekst.

### 3.1 Zadanie 5. Testy globalnej wypukłości

#### Generowanie lokalnych optimów

> Generuje 1000 lokalnych optimów z losowych rozwiązań startowych.
> Używa `GreedyLocalSearch`, ponieważ w zadaniu 5 lokalne przeszukiwanie jest zachłanne.
> Traktuje `SWAP_EDGES` jako wariant lokalnego przeszukiwania z wymianą krawędzi.
> Obejmuje ruchy wstawiania, usuwania oraz odwracania fragmentu cyklu.

```text
GenerateLocalOptima(\pvar{instance}, \pvar{localOptimaCount} = 1000)
    \pvar{localOptima} \(\leftarrow\) pusta lista
    \pvar{runs} \(\leftarrow\) przygotuj \pvar{localOptimaCount} konfiguracji uruchomień

    dla każdej konfiguracji \pvar{run} z \pvar{runs}
        \pvar{x} \(\leftarrow\) RandomSolution(\pvar{run.runSeed}).solve(\pvar{instance}, \pvar{run.startVertexId})
        \pvar{x} \(\leftarrow\) GreedyLocalSearch(SWAP_EDGES, \pvar{run.runSeed}).improve(\pvar{x})
        \pvar{objective} \(\leftarrow f(\pvar{x})\)
        dodaj \((\pvar{x}, \pvar{objective}, \pvar{run.startVertexId}, \pvar{run.runSeed})\) do \pvar{localOptima}

    zwróć \pvar{localOptima}
```

#### Wybór bardzo dobrego rozwiązania B

> Wyznacza bardzo dobre rozwiązanie `B` bez ręcznego wyboru wyniku.
> Wybiera `B` jako najlepsze rozwiązanie znalezione przez metody z poprzedniego etapu.
> Uruchamia MSLS i z jego średniego czasu wyznacza limit dla ILS, LNS i LNSa.

```text
SelectVeryGoodSolution(\pvar{instance})
    \pvar{runs} \(\leftarrow\) przygotuj 20 konfiguracji uruchomień jak w lab4

    \pvar{mslsResults} \(\leftarrow\) uruchom MSLS dla wszystkich \pvar{runs}
    \pvar{mslsAverageTime} \(\leftarrow\) średni czas działania MSLS
    \pvar{best} \(\leftarrow\) najlepszy wynik z \pvar{mslsResults}

    dla każdej metody \pvar{method} z listy ILS, LNS, LNSa
        \pvar{results} \(\leftarrow\) uruchom \pvar{method} dla wszystkich \pvar{runs}
                    z limitem czasu \pvar{mslsAverageTime}
        \pvar{candidate} \(\leftarrow\) najlepszy wynik z \pvar{results}

        jeżeli \(f(\pvar{candidate}) > f(\pvar{best})\)
            \pvar{best} \(\leftarrow\) \pvar{candidate}

    \pvar{B} \(\leftarrow\) \pvar{best}
    zapisz nazwę metody, seed, startVertexId i objective rozwiązania \pvar{B}

    zwróć \pvar{B}
```

#### Cechy rozwiązania

> Zamienia cykl na cechy potrzebne do porównywania rozwiązań.
> Zapamiętuje wybrane wierzchołki oraz krawędzie cyklu.
> Traktuje krawędzie jako nieskierowane, więc krawędź a-b i b-a oznacza to samo.
> Uwzględnia przejście z ostatniego wierzchołka do pierwszego jako zwykłą krawędź cyklu.

```text
SolutionFeatures(\pvar{cycle})
    \pvar{selectedVertices} \(\leftarrow\) tablica obecności wierzchołków w \pvar{cycle}
    \pvar{edges} \(\leftarrow\) pusta tablica krawędzi

    dla każdej pozycji w \pvar{cycle}
        \pvar{a} \(\leftarrow\) aktualny wierzchołek
        \pvar{b} \(\leftarrow\) następny wierzchołek w \pvar{cycle},
             z przejściem z końca na początek
        dodaj krawędź \((\min(\pvar{a}, \pvar{b}), \max(\pvar{a}, \pvar{b}))\) do \pvar{edges}

    posortuj \pvar{edges} i usuń powtórzenia

    zwróć \((\pvar{selectedVertices}, \pvar{edges})\)
```

#### Podobieństwo po wierzchołkach

> Liczy, ile wybranych wierzchołków występuje jednocześnie w obu rozwiązaniach.

```text
CommonVerticesSimilarity(\pvar{featuresA}, \pvar{featuresB})
    \pvar{count} \(\leftarrow 0\)

    dla każdego wierzchołka \(v\) instancji
        jeżeli \(v\) jest wybrany w \pvar{featuresA} oraz w \pvar{featuresB}
            \pvar{count} \(\leftarrow \pvar{count} + 1\)

    zwróć \pvar{count}
```

#### Podobieństwo po krawędziach

> Liczy, ile krawędzi cyklu występuje jednocześnie w obu rozwiązaniach.
> Zapisuje każdą krawędź jako nieskierowaną parę \((\min(a,b), \max(a,b))\).
> Wykorzystuje posortowanie list krawędzi zamiast porównywania każdej krawędzi z każdą.
> Przesuwa równolegle oba wskaźniki po listach krawędzi.
> Rozpoznaje równe aktualne krawędzie jako wspólną krawędź.
> Pomija mniejszą aktualną krawędź, ponieważ nie może pojawić się później w drugiej liście.

```text
CommonEdgesSimilarity(\pvar{featuresA}, \pvar{featuresB})
    \pvar{edgesA} \(\leftarrow\) posortowane krawędzie nieskierowane rozwiązania A
    \pvar{edgesB} \(\leftarrow\) posortowane krawędzie nieskierowane rozwiązania B
    \pvar{count} \(\leftarrow 0\)

    dopóki w obu listach są jeszcze nieprzejrzane krawędzie
        \pvar{edgeA} \(\leftarrow\) aktualna nieprzejrzana krawędź z \pvar{edgesA}
        \pvar{edgeB} \(\leftarrow\) aktualna nieprzejrzana krawędź z \pvar{edgesB}

        jeżeli \(\pvar{edgeA} = \pvar{edgeB}\)
            \pvar{count} \(\leftarrow \pvar{count} + 1\)
            przejdź do następnej krawędzi w obu listach

        w przeciwnym razie jeżeli \(\pvar{edgeA} < \pvar{edgeB}\)
            przejdź do następnej krawędzi w \pvar{edgesA}

        w przeciwnym razie
            przejdź do następnej krawędzi w \pvar{edgesB}

    zwróć \pvar{count}
```

#### Liczenie punktów do wykresów i korelacji

> Liczy dla każdego lokalnego optimum podobieństwo do bardzo dobrego rozwiązania `B`.
> Liczy dla każdego lokalnego optimum średnie podobieństwo do pozostałych lokalnych optimów.
> Wyznacza korelacje między wartością funkcji celu a każdą wartością podobieństwa.
> Liczy korelacje wyłącznie dla zbioru lokalnych optimów.

```text
Lab5Experiment(\pvar{instance})
    \pvar{B} \(\leftarrow\) SelectVeryGoodSolution(\pvar{instance})
    \pvar{localOptima} \(\leftarrow\) GenerateLocalOptima(\pvar{instance}, 1000)

    \pvar{bestFeatures} \(\leftarrow\) SolutionFeatures(\pvar{B.cycle})
    dla każdego lokalnego optimum \(\pvar{x_i}\)
        \pvar{features[i]} \(\leftarrow\) SolutionFeatures(\pvar{x_i.cycle})

    dla każdego lokalnego optimum \(\pvar{x_i}\)
        \pvar{similarityVerticesToBest[i]} \(\leftarrow\)
            CommonVerticesSimilarity(\pvar{features[i]}, \pvar{bestFeatures})
        \pvar{similarityEdgesToBest[i]} \(\leftarrow\)
            CommonEdgesSimilarity(\pvar{features[i]}, \pvar{bestFeatures})

    dla każdej pary lokalnych optimów \((\pvar{x_i}, \pvar{x_j})\), gdzie \(i < j\)
        \pvar{vertices} \(\leftarrow\) CommonVerticesSimilarity(\pvar{features[i]}, \pvar{features[j]})
        \pvar{edges} \(\leftarrow\) CommonEdgesSimilarity(\pvar{features[i]}, \pvar{features[j]})

        dodaj \pvar{vertices} do sumy podobieństwa wierzchołków dla \(i\) oraz \(j\)
        dodaj \pvar{edges} do sumy podobieństwa krawędzi dla \(i\) oraz \(j\)

    dla każdego lokalnego optimum \(\pvar{x_i}\)
        \pvar{avgVerticesToOthers[i]} \(\leftarrow \pvar{sumVertices[i]} / 999\)
        \pvar{avgEdgesToOthers[i]} \(\leftarrow \pvar{sumEdges[i]} / 999\)

    policz korelacje Pearsona:
        \(f(\pvar{x_i})\) z \pvar{similarityVerticesToBest[i]}
        \(f(\pvar{x_i})\) z \pvar{similarityEdgesToBest[i]}
        \(f(\pvar{x_i})\) z \pvar{avgVerticesToOthers[i]}
        \(f(\pvar{x_i})\) z \pvar{avgEdgesToOthers[i]}

    zapisz punkty do wykresów, korelacje i metadane rozwiązania \pvar{B}
```

Rozwiązanie `B` służy tylko jako punkt odniesienia przy liczeniu podobieństwa. Nie jest dodawane do zbioru 1000 lokalnych optimów i nie bierze udziału w średnim podobieństwie między lokalnymi optimami.

### 3.2 Zadanie 6. Hybrydowy algorytm ewolucyjny

#### Inicjalizacja populacji początkowej

> Buduje populację początkową z lokalnych optimów.
> Tworzy każdego osobnika z losowego rozwiązania startowego poprawionego lokalnym przeszukiwaniem.
> Przesuwa starty deterministycznie jak w MSLS, żeby nie wymuszać stale tego samego wierzchołka.
> Używa tej samej polityki różnorodności przy inicjalizacji i przy późniejszym dodawaniu potomków.

```text
PopulationInitializer(\pvar{instance}, \pvar{startVertexId}, \pvar{populationSize} = 20, \pvar{runSeed})
    \pvar{randomSolution} \(\leftarrow\) RandomSolution(\pvar{runSeed})
    \pvar{population} \(\leftarrow\) pusta populacja elitarna
    \pvar{attempts} \(\leftarrow 0\)
    \pvar{maxAttempts} \(\leftarrow \pvar{populationSize} \cdot 1000\)

    dopóki \(\pvar{population.size} < \pvar{populationSize}\) oraz \(\pvar{attempts} < \pvar{maxAttempts}\)
        \pvar{currentStart} \(\leftarrow (\pvar{startVertexId} + \pvar{attempts}) \bmod \pvar{instance.size}\)
        \pvar{x} \(\leftarrow\) \pvar{randomSolution}.solve(\pvar{instance}, \pvar{currentStart})
        \pvar{x} \(\leftarrow\) SteepestLocalSearchWithCandidateMoves(\pvar{x})
        \pvar{objective} \(\leftarrow f(\pvar{x})\)
        \pvar{features} \(\leftarrow\) SolutionFeatures(\pvar{x})

        jeżeli \pvar{x} jest różne od obecnej populacji według PopulationDiversityPolicy
            dodaj osobnika \((\pvar{x}, \pvar{objective}, \pvar{features})\) do \pvar{population}

        \pvar{attempts} \(\leftarrow \pvar{attempts} + 1\)

    jeżeli \(\pvar{population.size} < \pvar{populationSize}\)
        zgłoś błąd inicjalizacji populacji

    zwróć \pvar{population}
```

#### Hybrydowy algorytm ewolucyjny HAE

> Prowadzi algorytm ewolucyjny w trybie steady state.
> Tworzy w jednej iteracji jednego potomka z dwóch losowo wybranych rodziców.
> Używa rekombinacji do utworzenia surowego partial cycle.
> Uzupełnia potomka przez repair.
> Uruchamia local search po repair tylko w wariantach z lokalnym przeszukiwaniem.
> Obejmuje limitem czasu także inicjalizację populacji.

```text
HAE(\pvar{instance}, \pvar{startVertexId}, \pvar{runSeed}, \pvar{timeLimit},
    \pvar{recombinationOperator}, \pvar{useLocalSearch})

    \pvar{random} \(\leftarrow\) Random(\pvar{runSeed})
    \pvar{endTime} \(\leftarrow\) aktualny czas \(+\ \pvar{timeLimit}\)
    \pvar{population} \(\leftarrow\) PopulationInitializer(\pvar{instance}, \pvar{startVertexId}, 20, \pvar{runSeed})
    \pvar{iterations} \(\leftarrow 0\)

    dopóki aktualny czas \(< \pvar{endTime}\)
        \pvar{parent1}, \pvar{parent2} \(\leftarrow\) wylosuj dwa różne osobniki z \pvar{population}

        \pvar{child} \(\leftarrow\) \pvar{recombinationOperator}(\pvar{parent1}, \pvar{parent2}, \pvar{random})
        \pvar{child} \(\leftarrow\) RepairStage(\pvar{child}, \pvar{parent1}, \pvar{parent2})

        jeżeli \pvar{useLocalSearch}
            \pvar{child} \(\leftarrow\) SteepestLocalSearchWithCandidateMoves(\pvar{child})

        \pvar{childObjective} \(\leftarrow f(\pvar{child})\)
        \pvar{iterations} \(\leftarrow \pvar{iterations} + 1\)

        jeżeli \pvar{childObjective} nie jest lepszy od najgorszego osobnika w \pvar{population}
            pomiń \pvar{child}
        w przeciwnym razie
            \pvar{childFeatures} \(\leftarrow\) SolutionFeatures(\pvar{child})
            \pvar{childIndividual} \(\leftarrow (\pvar{child}, \pvar{childObjective}, \pvar{childFeatures})\)

            jeżeli \pvar{childIndividual} jest różny od \pvar{population} według PopulationDiversityPolicy
                dodaj \pvar{childIndividual} do \pvar{population}
                usuń najgorszego osobnika

    zwróć najlepszy osobnik z \pvar{population}
```

#### Polityka różnorodności populacji

> Nie dopuszcza kopii rozwiązań w populacji.
> Traktuje w podstawowej wersji dwa rozwiązania z tym samym objective jako kopie.

```text
ObjectiveDiversityPolicy(\pvar{candidate}, \pvar{population})
    dla każdego osobnika \pvar{x} w \pvar{population}
        jeżeli \(f(\pvar{candidate}) = f(\pvar{x})\)
            zwróć false

    zwróć true
```

#### Etap naprawy potomka

> Oddziela techniczne przygotowanie wejścia dla repair od semantyki rekombinacji.
> Przyjmuje, że rekombinacja może zwrócić pusty albo jednoelementowy partial cycle.
> Zapewnia przez `RepairSeedAdapter` minimalny rozmiar wejścia dla 2-regret repair.
> Wykonuje właściwy repair metodą 2-regret.

```text
RepairStage(\pvar{partialChild}, \pvar{parent1}, \pvar{parent2})
    jeżeli \pvar{partialChild} ma mniej niż 2 wierzchołki
        uzupełnij \pvar{partialChild} pierwszymi brakującymi wierzchołkami
        z \pvar{parent1}, a potem z \pvar{parent2}

    \pvar{child} \(\leftarrow\) TwoRegretRepairOperator(\pvar{partialChild})

    zwróć \pvar{child}
```

`TwoRegretRepairOperator` uzupełnia cykl metodą 2-regret i na końcu wykonuje `PhaseTwoDelete`.

#### Operator 1: wspólne części rodziców

> Zachowuje części trasy wspólne dla obu rodziców.
> Buduje wielowierzchołkowe fragmenty ze wspólnych krawędzi.
> Zachowuje wspólne wierzchołki bez wspólnej krawędzi jako części jednoelementowe.

```text
CommonPartsRecombination(\pvar{parent1}, \pvar{parent2})
    \pvar{commonEdges} \(\leftarrow\) krawędzie \pvar{parent1}, które występują też w \pvar{parent2}
    \pvar{edgeFragments} \(\leftarrow\) podścieżki zbudowane ze wspólnych krawędzi

    \pvar{used} \(\leftarrow\) wierzchołki należące do \pvar{edgeFragments}
    \pvar{parts} \(\leftarrow \pvar{edgeFragments}\)

    dla każdego wspólnego wierzchołka \(v\) rodziców \pvar{parent1} i \pvar{parent2}
        jeżeli \(v\) nie należy do \pvar{used}
            dodaj \(v\) jako jednoelementową część do \pvar{parts}

    losowo ustaw kolejność części w \pvar{parts}
    dla każdej części losowo zdecyduj, czy odwrócić jej kierunek
    \pvar{child} \(\leftarrow\) połącz części w jeden partial cycle

    zwróć \pvar{child}
```

#### Operator 2: wspólne wierzchołki i krawędzie po filtracji

> Zaczyna od jednego rodzica i zostawia tylko strukturę potwierdzoną przez drugiego rodzica.
> Usuwa najpierw wierzchołki nieobecne w drugim rodzicu.
> Łączy sąsiadów, którzy stają się sąsiadami po filtracji sekwencji.
> Sprawdza dopiero na nowych połączeniach, które krawędzie występują także w drugim rodzicu.
> Buduje fragmenty potomka z zachowanych krawędzi po filtracji.
> Zwraca krótką sekwencję jako raw partial cycle dla `RepairStage`, jeżeli po filtracji zostało mniej niż 2 wierzchołki.

```text
CommonEdgesAndVerticesRecombination(\pvar{parent1}, \pvar{parent2})
    \pvar{base} \(\leftarrow\) losowo wybrany rodzic, którego kolejność będzie filtrowana
    \pvar{filter} \(\leftarrow\) drugi rodzic

    \pvar{filtered} \(\leftarrow\) pusta sekwencja
    dla każdego wierzchołka \(v\) w cyklu \pvar{base}
        jeżeli \(v\) występuje w \pvar{filter}
            dodaj \(v\) do \pvar{filtered}

    jeżeli \pvar{filtered} ma mniej niż 2 wierzchołki
        zwróć \pvar{filtered} jako partial cycle

    \pvar{retainedEdges} \(\leftarrow\) pusta tablica zachowanych połączeń
    dla każdej krawędzi \(\pvar{a}-\pvar{b}\) w cyklu utworzonym przez \pvar{filtered}
        jeżeli krawędź \(\pvar{a}-\pvar{b}\) występuje w \pvar{filter}
            oznacz połączenie \(\pvar{a}-\pvar{b}\) jako zachowane

    \pvar{fragments} \(\leftarrow\) podścieżki zbudowane z zachowanych połączeń
    losowo ustaw kolejność \pvar{fragments}
    dla każdego fragmentu losowo zdecyduj, czy odwrócić jego kierunek
    \pvar{child} \(\leftarrow\) połącz \pvar{fragments} w jeden partial cycle

    zwróć \pvar{child}
```

#### Operator 3: wspólne wierzchołki

> Zachowuje tylko wierzchołki wspólne dla obu rodziców.
> Bierze kolejność zachowanych wierzchołków z losowo wybranego rodzica bazowego.
> Nie usuwa krawędzi tylko dlatego, że nie występowały w drugim rodzicu.

```text
CommonVerticesRecombination(\pvar{parent1}, \pvar{parent2})
    \pvar{base} \(\leftarrow\) losowo wybrany rodzic z pary \pvar{parent1}, \pvar{parent2}
    \pvar{filter} \(\leftarrow\) drugi rodzic

    \pvar{child} \(\leftarrow\) pusty partial cycle
    dla każdego wierzchołka \(v\) w cyklu \pvar{base}
        jeżeli \(v\) występuje w \pvar{filter}
            dodaj \(v\) do \pvar{child}

    zwróć \pvar{child}
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
2REGRET_P2D(\pvar{instance}, \pvar{startVertexId})
    \pvar{x} \(\leftarrow\) TwoRegretCost(\pvar{instance}, \pvar{startVertexId})
    \pvar{x} \(\leftarrow\) PhaseTwoDelete(\pvar{x})

    zwróć \pvar{x}
```

> Podaje wynik pojedynczego bazowego lokalnego przeszukiwania.
> Zaczyna od losowego rozwiązania startowego.
> Używa tego samego `SteepestLocalSearchWithCandidateMoves` co MSLS, ILS, LNS i HAE.

```text
BASE_LS(\pvar{instance}, \pvar{startVertexId}, \pvar{runSeed})
    \pvar{x} \(\leftarrow\) RandomSolution(\pvar{runSeed}).solve(\pvar{instance}, \pvar{startVertexId})
    \pvar{x} \(\leftarrow\) SteepestLocalSearchWithCandidateMoves(\pvar{x})

    zwróć \pvar{x}
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

W zadaniu 5 na obu instancjach otrzymano dodatnią korelację między wartością funkcji celu a podobieństwem lokalnych optimów. Lepsze lokalne optima częściej miały wspólne elementy z bardzo dobrym rozwiązaniem oraz z innymi lokalnymi optimami. Oznacza to, że dobre rozwiązania nie były całkowicie rozrzucone losowo po przestrzeni rozwiązań, tylko zaczynały tworzyć podobne struktury.

Zależność była wyraźnie silniejsza dla instancji TSPB niż dla TSPA. Dla TSPB korelacje mieściły się w zakresie od około 0.79 do 0.87, a dla TSPA od około 0.53 do 0.66. Można to odczytać tak, że na TSPB dobre lokalne optima skupiają się wokół bardziej podobnego układu wierzchołków i krawędzi. Na TSPA istnieje więcej różnych lokalnych optimów o dobrej jakości, ale mniej podobnej strukturze.

W obu instancjach podobieństwo po krawędziach dawało silniejszy sygnał niż podobieństwo po samych wierzchołkach. Jest to zgodne z charakterem problemu: jakość rozwiązania zależy nie tylko od tego, które wierzchołki zostaną wybrane, ale też od tego, jak zostaną połączone w zamkniętym cyklu. Dwa rozwiązania mogą odwiedzać podobne wierzchołki, ale mieć inną długość trasy, jeżeli używają innych krawędzi.

Bardzo dobre rozwiązanie użyte jako punkt odniesienia zostało w obu instancjach znalezione przez ILS. W tym eksperymencie ILS dał więc najlepszy pojedynczy wynik spośród metod użytych do wyznaczenia rozwiązania referencyjnego dla testu globalnej wypukłości.

W zadaniu 6 najlepsze średnie wyniki w głównym porównaniu nadal uzyskały metody z poprzedniego zadania: na TSPA najlepszy był LNS, a na TSPB najlepszy był ILS. HAE nie poprawił najlepszych średnich wyników tych metod, ale jego najlepszy wariant był konkurencyjny względem MSLS i LNS. To pokazuje, że rekombinacja dobrych rozwiązań działała sensownie, ale w tej konfiguracji nie była wystarczająco silna, żeby zastąpić najlepsze metody perturbacyjne z lab4.

Najlepszym wariantem HAE był HAE z operatorem 1 i lokalnym przeszukiwaniem po rekombinacji. Operator 1 zachowuje zarówno wspólne krawędzie, jak i wspólne wierzchołki rodziców, dlatego przekazuje do potomka więcej stabilnej struktury niż operator 2 i operator 3. Wyniki sugerują, że korzystniejsze było zachowywanie większej części wspólnej struktury rodziców niż mocniejsze niszczenie rozwiązania i późniejsze odbudowywanie go przez repair.

Operator 2 z lokalnym przeszukiwaniem był znacznie lepszy niż jego wersja bez lokalnego przeszukiwania. Na TSPA różnica średnich wartości funkcji celu wynosiła około 784 punkty, a na TSPB około 1087 punktów. Oznacza to, że sam repair po rekombinacji operatora 2 nie wystarczał do pełnego wykorzystania powstałego potomka. Lokalna poprawa była potrzebna, żeby uporządkować rozwiązanie po połączeniu fragmentów.

Dla operatora 3 różnica między wersją z lokalnym przeszukiwaniem i bez niego była mniejsza. Operator 3 zachowuje tylko wspólne wierzchołki i kolejność z rodzica bazowego, więc tworzy prostszy partial cycle niż operator 2. Taki potomek może być łatwiejszy do naprawienia przez sam repair, ale jednocześnie traci więcej informacji o wspólnych krawędziach rodziców, co ogranicza jakość końcowego wyniku.

Warianty bez lokalnego przeszukiwania wykonywały więcej iteracji, ale większa liczba iteracji nie przełożyła się automatycznie na lepszą jakość rozwiązań. Szczególnie widać to dla operatora 3, który wykonywał najwięcej iteracji, lecz nie dawał najlepszych wyników. Liczba iteracji pokazuje więc bardziej koszt pojedynczej próby niż skuteczność metody. Dobra iteracja HAE musi nie tylko szybko tworzyć potomka, ale też zachowywać i poprawiać wartościową strukturę rozwiązania.

Metody referencyjne 2-regret z PhaseTwoDelete oraz bazowe lokalne przeszukiwanie osiągnęły wyniki wyraźnie słabsze od głównych metaheurystyk. Oznacza to, że samo zachłanne konstruowanie rozwiązania albo pojedyncze lokalne przeszukiwanie z losowego startu nie wystarcza do uzyskania jakości porównywalnej z MSLS, ILS, LNS i najlepszymi wariantami HAE. Lepsze wyniki wymagały wielokrotnego wychodzenia z lokalnych optimów przez perturbację, destroy-repair albo rekombinację.

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
