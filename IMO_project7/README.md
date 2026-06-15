# Kod źródłowy do zadania 7

Pakiet zawiera dwa niezależne projekty użyte w eksperymentach do zadania 7.
Projekty są pozostawione osobno, ponieważ w takiej organizacji były uruchamiane podczas testów.

## 01_2regret_p2d_ils_sa

Projekt zawiera główne warianty algorytmów porównywanych w raporcie:

```text
2R_P2D
2R_FAST_P2D
2R_FAST_P2D_15_35_55_75
2R_P2D_ILS_SA
2R_FAST_P2D_ILS_SA
2R_FAST_P2D_15_35_55_75_ILS_SA
RANDOM_START_ILS
RANDOM_START_ILS_SA
```

Najważniejsze pliki:

```text
src/app/LabFinalFairRunPrepared.java
src/faircomparison/LabFinalAlgorithmDefinition.java
src/faircomparison/OriginalTwoRegretP2DStart.java
src/faircomparison/StagedCycleP2D15355575Start.java
src/algorithm/construction/FastTop2ExactTwoRegretCost.java
src/algorithm/construction/FastTop2ExactTwoRegretWithPhaseTwoDelete.java
src/algorithm/metaheuristic/IteratedLocalSearch.java
src/algorithm/metaheuristic/IteratedLocalSearchWithConstructionStartAndSaAcceptance.java
```

Uruchomienie:

```bash
cd 01_2regret_p2d_ils_sa
./run_lab_final_fair.sh 0.5 200 20260614
```

## 02_parallel_staged_p2d_ils_sa

Projekt zawiera testowaną wersję równoległą:

```text
Parallel STAGED_P2D_15_35_55_75 / ILS-SA
```

Najważniejsze pliki:

```text
src/app/ParallelStagedIlsSaRun.java
src/faircomparison/ParallelStagedIlsSaDefinition.java
src/faircomparison/StagedCycleP2D15355575Start.java
src/algorithm/metaheuristic/ParallelIteratedLocalSearchWithConstructionStartAndSaAcceptance.java
```

Uruchomienie:

```bash
cd 02_parallel_staged_p2d_ils_sa
./run_parallel_staged_ils_sa.sh 0.5 200 20260614 configs
```

## Dane i konfiguracje

Oba projekty zawierają dane i konfiguracje uruchomień:

```text
data/TSPA.csv
data/TSPB.csv
configs/TSPA_run_configs_200.csv
configs/TSPB_run_configs_200.csv
```

Konfiguracje mają format:

```text
runIndex,startVertexId,runSeed
```

## Parametry użyte w eksperymentach

```text
timeLimit = 0.5 s
runs = 200 na instancję
T0 = 500
Tmin = 10
perturbacja = 30 losowych SwapEdges
```
