package experiment.core;

import algorithm.OptimizationAlgorithm;

/**
 * Opisuje jeden algorytm porownywany w eksperymencie.
 */
public final class TestedAlgorithm {
    private final String name;
    private final Builder builder;

    /**
     * Tworzy opis algorytmu uwzglednianego w eksperymencie.
     *
     * @param name nazwa algorytmu uzywana w wynikach eksperymentu
     * @param builder sposob tworzenia swiezej instancji algorytmu dla pojedynczego uruchomienia
     */
    public TestedAlgorithm(String name, Builder builder) {
        this.name = name;
        this.builder = builder;
    }

    /**
     * Zwraca nazwe algorytmu uzywana w wynikach eksperymentu.
     *
     * @return nazwa algorytmu
     */
    public String name() {
        return name;
    }

    /**
     * Tworzy swieza instancje algorytmu dla podanego uruchomienia.
     *
     * @param runConfig konfiguracja pojedynczego uruchomienia
     * @return nowa instancja algorytmu
     */
    public OptimizationAlgorithm create(RunConfig runConfig) {
        return builder.create(name, runConfig);
    }

    @FunctionalInterface
    public interface Builder {
        OptimizationAlgorithm create(String name, RunConfig runConfig);
    }
}
