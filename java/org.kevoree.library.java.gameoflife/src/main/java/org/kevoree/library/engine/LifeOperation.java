package org.kevoree.library.engine;

/**
 * Created by mleduc on 11/03/16.
 */
public class LifeOperation {
    public final LifeOperationType type;
    public final long x;
    public final long y;
    public final String qualifier;

    public enum LifeOperationType {New, Dead};

    private LifeOperation(final LifeOperationType type, final long x, final long y, final String qualifier) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.qualifier = qualifier;
    }

    public static LifeOperation newCell(final long x, final long y, final String qualifier) {
        return new LifeOperation(LifeOperationType.New, x, y, qualifier);
    }

    public static LifeOperation deadCell(final long x, final long y, final String qualifier) {
        return new LifeOperation(LifeOperationType.Dead, x, y, qualifier);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LifeOperation that = (LifeOperation) o;

        if (x != that.x) return false;
        if (y != that.y) return false;
        return type == that.type;

    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + (int) (x ^ (x >>> 32));
        result = 31 * result + (int) (y ^ (y >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "LifeOperation{" +
                "type=" + type +
                ", x=" + x +
                ", y=" + y +
                '}';
    }
}
