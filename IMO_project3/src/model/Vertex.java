package model;

public class Vertex {
    public final int id;
    public final int x;
    public final int y;
    public final int profit;

    public Vertex(int id, int x, int y, int profit) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.profit = profit;
    }

    @Override
    public String toString() {
        return "Vertex{" +
                "id=" + id +
                ", x=" + x +
                ", y=" + y +
                ", profit=" + profit +
                '}';
    }
}