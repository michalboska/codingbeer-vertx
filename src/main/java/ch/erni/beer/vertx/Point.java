package ch.erni.beer.vertx;

/**
 * We don't want to depend on AWT or similar libraries
 */
public class Point {
    private int x, y;

    public Point() {
        this(0, 0);
    }

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void set(Point point) {
        this.x = point.x;
        this.y = point.y;
    }

    @Override
    public String toString() {
        return String.format("Point: [x=%d; y=%d]", x, y);
    }
}
