package algorithm.localsearch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Reprezentuje cykl z dodatkowym indeksem pozycji wierzchołków.
 * Klasa jest przeznaczona dla wersji lokalnego przeszukiwania z listą LM,
 * gdzie zapamiętane ruchy muszą odnajdywać aktualne pozycje wierzchołków
 * po zmianach kolejności w cyklu.
 */
public final class IndexedCycle {
    public static final int MISSING_EDGE = 0;
    public static final int SAME_RELATIVE_DIRECTION = 1;
    public static final int REVERSED_RELATIVE_DIRECTION = 2;

    public final int[] cycle;
    private final int[] positionByVertex;
    private int size;

    /**
     * Tworzy pusty cykl o podanej pojemności.
     * Wszystkie wierzchołki są początkowo oznaczone jako niewybrane.
     *
     * @param capacity liczba wszystkich wierzchołków w instancji
     */
    public IndexedCycle(int capacity) {
        this.cycle = new int[capacity];
        this.positionByVertex = new int[capacity];
        this.size = 0;
        Arrays.fill(this.positionByVertex, -1);
    }

    /**
     * Tworzy cykl z gotowej listy wierzchołków i od razu buduje indeks pozycji.
     *
     * @param initialCycle wierzchołki w kolejności przechodzenia cyklu
     * @param capacity liczba wszystkich wierzchołków w instancji
     */
    public IndexedCycle(List<Integer> initialCycle, int capacity) {
        this.cycle = new int[capacity];
        this.positionByVertex = new int[capacity];
        this.size = initialCycle.size();
        Arrays.fill(this.positionByVertex, -1);

        for (int i = 0; i < size; i++) {
            int vertex = initialCycle.get(i);
            this.cycle[i] = vertex;
            this.positionByVertex[vertex] = i;
        }
    }

    /**
     * Tworzy indeksowaną kopię zwykłego cyklu z istniejącej implementacji.
     * Konstruktor służy do przejścia z kodu z laboratorium 2 do wersji LM.
     *
     * @param other cykl źródłowy
     */
    public IndexedCycle(Cycle other) {
        this.cycle = new int[other.cycle.length];
        this.positionByVertex = new int[other.cycle.length];
        this.size = other.size();
        Arrays.fill(this.positionByVertex, -1);

        for (int i = 0; i < size; i++) {
            int vertex = other.cycle[i];
            this.cycle[i] = vertex;
            this.positionByVertex[vertex] = i;
        }
    }

    /**
     * Tworzy głęboką kopię innego IndexedCycle.
     * Kopia ma własne tablice cyklu i pozycji.
     *
     * @param other cykl źródłowy
     */
    public IndexedCycle(IndexedCycle other) {
        this.cycle = new int[other.cycle.length];
        this.positionByVertex = new int[other.positionByVertex.length];
        this.size = other.size;
        System.arraycopy(other.cycle, 0, this.cycle, 0, other.cycle.length);
        System.arraycopy(other.positionByVertex, 0, this.positionByVertex, 0, other.positionByVertex.length);
    }

    /**
     * Zwraca aktualną liczbę wierzchołków w cyklu.
     *
     * @return długość bieżącego cyklu
     */
    public int size() {
        return size;
    }

    /**
     * Zwraca pojemność tablicy cyklu, czyli liczbę wierzchołków instancji.
     *
     * @return maksymalna liczba wierzchołków
     */
    public int capacity() {
        return cycle.length;
    }

    /**
     * Zwraca wierzchołek stojący na podanej pozycji cyklu.
     *
     * @param position pozycja w cyklu
     * @return identyfikator wierzchołka na tej pozycji
     */
    public int vertexAt(int position) {
        return cycle[position];
    }

    /**
     * Zwraca aktualną pozycję podanego wierzchołka w czasie stałym.
     * Wartość -1 oznacza, że wierzchołek nie należy do cyklu.
     *
     * @param vertex identyfikator wierzchołka
     * @return pozycja w cyklu albo -1
     */
    public int positionOf(int vertex) {
        return positionByVertex[vertex];
    }

    /**
     * Sprawdza, czy wierzchołek należy do aktualnego cyklu.
     *
     * @param vertex identyfikator wierzchołka
     * @return true, jeżeli wierzchołek jest wybrany
     */
    public boolean containsVertex(int vertex) {
        return positionByVertex[vertex] != -1;
    }

    /**
     * Zwraca poprzednią pozycję w cyklu z uwzględnieniem domknięcia cyklu.
     *
     * @param index bieżąca pozycja
     * @return poprzednia pozycja
     */
    public int prevIndex(int index) {
        return index == 0 ? size - 1 : index - 1;
    }

    /**
     * Zwraca następną pozycję w cyklu z uwzględnieniem domknięcia cyklu.
     *
     * @param index bieżąca pozycja
     * @return następna pozycja
     */
    public int nextIndex(int index) {
        return index + 1 == size ? 0 : index + 1;
    }

    /**
     * Zwraca poprzedniego sąsiada podanego wierzchołka w aktualnym cyklu.
     *
     * @param vertex identyfikator wierzchołka
     * @return poprzedni wierzchołek w cyklu
     */
    public int prevVertex(int vertex) {
        int position = positionByVertex[vertex];
        return cycle[prevIndex(position)];
    }

    /**
     * Zwraca następnego sąsiada podanego wierzchołka w aktualnym cyklu.
     *
     * @param vertex identyfikator wierzchołka
     * @return następny wierzchołek w cyklu
     */
    public int nextVertex(int vertex) {
        int position = positionByVertex[vertex];
        return cycle[nextIndex(position)];
    }

    /**
     * Sprawdza, czy dwie pozycje są sąsiednie w zamkniętym cyklu.
     *
     * @param firstPosition pierwsza pozycja
     * @param secondPosition druga pozycja
     * @return true, jeżeli pozycje są sąsiednie
     */
    public boolean areAdjacentPositions(int firstPosition, int secondPosition) {
        return nextIndex(firstPosition) == secondPosition || nextIndex(secondPosition) == firstPosition;
    }

    /**
     * Sprawdza, czy dwa wierzchołki są połączone krawędzią cyklu.
     * Kierunek przechodzenia nie ma znaczenia.
     *
     * @param firstVertex pierwszy wierzchołek
     * @param secondVertex drugi wierzchołek
     * @return true, jeżeli wierzchołki są sąsiednie w cyklu
     */
    public boolean areAdjacentVertices(int firstVertex, int secondVertex) {
        return hasUndirectedEdge(firstVertex, secondVertex);
    }

    /**
     * Sprawdza, czy w aktualnym kierunku przechodzenia cyklu istnieje krawędź fromVertex -> toVertex.
     *
     * @param fromVertex początek skierowanej krawędzi
     * @param toVertex koniec skierowanej krawędzi
     * @return true, jeżeli krawędź skierowana występuje w cyklu
     */
    public boolean hasDirectedEdge(int fromVertex, int toVertex) {
        int fromPosition = positionByVertex[fromVertex];
        return fromPosition != -1 && cycle[nextIndex(fromPosition)] == toVertex;
    }

    /**
     * Sprawdza, czy dwa wierzchołki tworzą krawędź aktualnego cyklu w dowolnym kierunku.
     *
     * @param firstVertex pierwszy wierzchołek
     * @param secondVertex drugi wierzchołek
     * @return true, jeżeli krawędź występuje w cyklu
     */
    public boolean hasUndirectedEdge(int firstVertex, int secondVertex) {
        int firstPosition = positionByVertex[firstVertex];
        return firstPosition != -1
            && (cycle[nextIndex(firstPosition)] == secondVertex
            || cycle[prevIndex(firstPosition)] == secondVertex);
    }

    /**
     * Sprawdza, czy middleVertex leży bezpośrednio między leftVertex i rightVertex.
     * Kierunek przechodzenia cyklu może być zgodny albo przeciwny.
     *
     * @param leftVertex pierwszy sąsiad
     * @param middleVertex sprawdzany wierzchołek środkowy
     * @param rightVertex drugi sąsiad
     * @return true, jeżeli trzy wierzchołki tworzą kolejny fragment cyklu
     */
    public boolean isBetween(int leftVertex, int middleVertex, int rightVertex) {
        return hasDirectedEdge(leftVertex, middleVertex) && hasDirectedEdge(middleVertex, rightVertex)
            || hasDirectedEdge(rightVertex, middleVertex) && hasDirectedEdge(middleVertex, leftVertex);
    }

    /**
     * Określa względny kierunek dwóch zapamiętanych krawędzi względem aktualnego cyklu.
     * Wynik MISSING_EDGE oznacza, że co najmniej jedna z krawędzi nie istnieje już w cyklu.
     * Wynik SAME_RELATIVE_DIRECTION oznacza, że obie krawędzie mają ten sam względny kierunek
     * co w chwili zapisania ruchu. Wynik REVERSED_RELATIVE_DIRECTION oznacza przeciwny
     * względny kierunek jednej krawędzi względem drugiej.
     *
     * @param firstStart początek pierwszej zapamiętanej krawędzi
     * @param firstEnd koniec pierwszej zapamiętanej krawędzi
     * @param secondStart początek drugiej zapamiętanej krawędzi
     * @param secondEnd koniec drugiej zapamiętanej krawędzi
     * @return kod stanu kierunku krawędzi
     */
    public int relativeDirectionOfEdges(int firstStart, int firstEnd, int secondStart, int secondEnd) {
        boolean firstForward = hasDirectedEdge(firstStart, firstEnd);
        boolean firstBackward = hasDirectedEdge(firstEnd, firstStart);
        boolean secondForward = hasDirectedEdge(secondStart, secondEnd);
        boolean secondBackward = hasDirectedEdge(secondEnd, secondStart);

        if (firstForward && secondForward || firstBackward && secondBackward) {
            return SAME_RELATIVE_DIRECTION;
        }
        if (firstForward && secondBackward || firstBackward && secondForward) {
            return REVERSED_RELATIVE_DIRECTION;
        }
        return MISSING_EDGE;
    }

    /**
     * Dodaje wierzchołek na koniec wewnętrznej reprezentacji cyklu.
     * Metoda aktualizuje również indeks pozycji tego wierzchołka.
     *
     * @param vertex dodawany wierzchołek
     */
    public void append(int vertex) {
        cycle[size] = vertex;
        positionByVertex[vertex] = size;
        size++;
    }

    /**
     * Wstawia wierzchołek bezpośrednio po podanej pozycji.
     * Wszystkie przesunięte wierzchołki otrzymują nowe wpisy w indeksie pozycji.
     *
     * @param position pozycja, po której ma nastąpić wstawienie
     * @param vertex wstawiany wierzchołek
     */
    public void insertAfterPosition(int position, int vertex) {
        int insertPosition = position + 1;

        for (int k = size; k > insertPosition; k--) {
            cycle[k] = cycle[k - 1];
            positionByVertex[cycle[k]] = k;
        }

        cycle[insertPosition] = vertex;
        positionByVertex[vertex] = insertPosition;
        size++;
    }

    /**
     * Wstawia wierzchołek bezpośrednio po wskazanym wierzchołku cyklu.
     *
     * @param previousVertex wierzchołek poprzedzający miejsce wstawienia
     * @param insertedVertex wstawiany wierzchołek
     */
    public void insertAfterVertex(int previousVertex, int insertedVertex) {
        insertAfterPosition(positionByVertex[previousVertex], insertedVertex);
    }

    /**
     * Wstawia wierzchołek pomiędzy dwa sąsiednie wierzchołki aktualnego cyklu.
     * Metoda wybiera właściwy kierunek wstawienia na podstawie aktualnego układu krawędzi.
     *
     * @param firstVertex pierwszy koniec krawędzi
     * @param secondVertex drugi koniec krawędzi
     * @param insertedVertex wstawiany wierzchołek
     */
    public void insertBetween(int firstVertex, int secondVertex, int insertedVertex) {
        if (hasDirectedEdge(firstVertex, secondVertex)) {
            insertAfterVertex(firstVertex, insertedVertex);
        } else {
            insertAfterVertex(secondVertex, insertedVertex);
        }
    }

    /**
     * Usuwa wierzchołek znajdujący się na podanej pozycji.
     * Po usunięciu aktualizuje pozycje wszystkich przesuniętych wierzchołków.
     *
     * @param position pozycja usuwanego wierzchołka
     */
    public void removeAt(int position) {
        int removedVertex = cycle[position];
        positionByVertex[removedVertex] = -1;

        for (int k = position; k < size - 1; k++) {
            cycle[k] = cycle[k + 1];
            positionByVertex[cycle[k]] = k;
        }

        size--;
    }

    /**
     * Usuwa wskazany wierzchołek z cyklu.
     * Pozycja wierzchołka jest odczytywana z indeksu positionByVertex.
     *
     * @param vertex usuwany wierzchołek
     */
    public void removeVertex(int vertex) {
        removeAt(positionByVertex[vertex]);
    }

    /**
     * Zamienia miejscami dwa wierzchołki w cyklu i aktualizuje ich pozycje.
     *
     * @param firstPosition pierwsza pozycja
     * @param secondPosition druga pozycja
     */
    public void swapPositions(int firstPosition, int secondPosition) {
        int firstVertex = cycle[firstPosition];
        int secondVertex = cycle[secondPosition];

        cycle[firstPosition] = secondVertex;
        cycle[secondPosition] = firstVertex;

        positionByVertex[firstVertex] = secondPosition;
        positionByVertex[secondVertex] = firstPosition;
    }

    /**
     * Odwraca fragment cyklu od firstPosition do lastPosition w kierunku przechodzenia cyklu.
     * Operacja jest używana do wykonania ruchu wymiany dwóch krawędzi.
     *
     * @param firstPosition pierwsza pozycja odwracanego fragmentu
     * @param lastPosition ostatnia pozycja odwracanego fragmentu
     */
    public void reverseFragment(int firstPosition, int lastPosition) {
        int length = forwardDistance(firstPosition, lastPosition) + 1;
        int left = firstPosition;
        int right = lastPosition;

        for (int i = 0; i < length / 2; i++) {
            swapPositions(left, right);
            left = nextIndex(left);
            right = prevIndex(right);
        }
    }

    /**
     * Wykonuje ruch wymiany dwóch krawędzi zapisanych przez identyfikatory wierzchołków.
     * Przyjmuje, że w cyklu istnieją skierowane krawędzie firstStart -> firstEnd
     * oraz secondStart -> secondEnd, a następnie odwraca fragment między firstEnd i secondStart.
     *
     * @param firstStart początek pierwszej usuwanej krawędzi
     * @param firstEnd koniec pierwszej usuwanej krawędzi
     * @param secondStart początek drugiej usuwanej krawędzi
     * @param secondEnd koniec drugiej usuwanej krawędzi
     */
    public void swapEdgesByVertices(int firstStart, int firstEnd, int secondStart, int secondEnd) {
        reverseFragment(positionByVertex[firstEnd], positionByVertex[secondStart]);
    }

    /**
     * Zwraca aktualny cykl jako listę identyfikatorów wierzchołków.
     *
     * @return lista wierzchołków w kolejności przechodzenia cyklu
     */
    public List<Integer> toList() {
        ArrayList<Integer> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            result.add(cycle[i]);
        }
        return result;
    }

    /**
     * Tworzy zwykły Cycle na podstawie aktualnego IndexedCycle.
     * Metoda pozwala zwrócić wynik LM do istniejących elementów projektu.
     *
     * @return cykl w starej reprezentacji
     */
    public Cycle toCycle() {
        Cycle result = new Cycle(cycle.length);
        for (int i = 0; i < size; i++) {
            result.append(cycle[i]);
        }
        return result;
    }

    /**
     * Liczy liczbę kroków od jednej pozycji do drugiej w kierunku przechodzenia cyklu.
     *
     * @param fromPosition pozycja początkowa
     * @param toPosition pozycja końcowa
     * @return liczba przejść między pozycjami
     */
    private int forwardDistance(int fromPosition, int toPosition) {
        int distance = 0;
        int current = fromPosition;

        while (current != toPosition) {
            current = nextIndex(current);
            distance++;
        }

        return distance;
    }
}
