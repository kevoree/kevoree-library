package org.kevoree.library.engine;

import java.util.Collection;
import java.util.TreeSet;

/**
 * Created by mleduc on 17/03/16.
 */
public class Cell implements Comparable<Cell> {
	private final long x;
	private final long y;
	private final String qualifier;

	public Cell(long x, long y, final String qualifier) {
		this.x = x;
		this.y = y;
		this.qualifier = qualifier;
	}

	public long getX() {
		return x;
	}

	public long getY() {
		return y;
	}

	@Override
	public String toString() {
		return "Cell{" + "x=" + x + ", y=" + y + ", qualifier=" + qualifier + '}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		Cell cell = (Cell) o;

		if (x != cell.x)
			return false;
		return y == cell.y;

	}

	@Override
	public int hashCode() {
		int result = (int) (x ^ (x >>> 32));
		result = 31 * result + (int) (y ^ (y >>> 32));
		return result;
	}

	public Collection<? extends Cell> getNeighbours() {
		final Collection<Cell> cells = new TreeSet<>();
		cells.add(new Cell(x - 1, y - 1, null));
		cells.add(new Cell(x - 1, y, null));
		cells.add(new Cell(x - 1, y + 1, null));
		cells.add(new Cell(x, y - 1, null));
		cells.add(new Cell(x, y + 1, null));
		cells.add(new Cell(x + 1, y - 1, null));
		cells.add(new Cell(x + 1, y, null));
		cells.add(new Cell(x + 1, y + 1, null));
		return cells;
	}

	@Override
	public int compareTo(Cell o) {
		final long i = o.x - x;
		if (i == 0) {
			return (int) (o.y - y);
		}
		return (int) i;
	}

	public String getQualifier() {
		return qualifier;
	}
}
