package algorithm.localsearch;

import java.util.List;

public final class Cycle {
    public final int[] cycle;
    private int size;

    public Cycle(int capacity) {
        this.cycle = new int[capacity];
        this.size = 0;
    }

    public Cycle(Cycle other) {
        this.cycle = new int[other.cycle.length];
        this.size = other.size;
        System.arraycopy(other.cycle, 0, this.cycle, 0, this.size);
    }

    public Cycle(List<Integer> initialCycle, int capacity) {
        this.cycle = new int[capacity];
        this.size = initialCycle.size();

        for (int i = 0; i < size; i++) {
            this.cycle[i] = initialCycle.get(i);
        }
    }

    public int size() {
        return size;
    }

    public int prevIndex(int index) {
        return index == 0 ? size - 1 : index - 1;
    }

    public int nextIndex(int index) {
        return index + 1 == size ? 0 : index + 1;
    }

    public int indexOfVertex(int vertex) {
        for (int i = 0; i < size; i++) {
            if (cycle[i] == vertex) {
                return i;
            }
        }
        return -1;
    }

    public boolean areAdjacentPositions(int i, int j) {
        return nextIndex(i) == j || nextIndex(j) == i;
    }

    public void swapPositions(int i, int j) {
        int tmp = cycle[i];
        cycle[i] = cycle[j];
        cycle[j] = tmp;
    }

    public void reverseFragment(int left, int right) {
        while (left < right) {
            swapPositions(left, right);
            left++;
            right--;
        }
    }

    public void insertAfter(int i, int vertex) {
        int insertIndex = i + 1;

        for (int k = size; k > insertIndex; k--) {
            cycle[k] = cycle[k - 1];
        }

        cycle[insertIndex] = vertex;
        size++;
    }

    public void append(int vertex) {
        cycle[size] = vertex;
        size++;
    }

    public void removeAt(int index) {
        for (int k = index; k < size - 1; k++) {
            cycle[k] = cycle[k + 1];
        }

        size--;
    }
}
