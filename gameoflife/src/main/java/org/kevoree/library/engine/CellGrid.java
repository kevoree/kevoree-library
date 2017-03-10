package org.kevoree.library.engine;

import java.util.*;
import java.util.function.Predicate;

/**
 * Created by mleduc on 10/03/16.
 */
public class CellGrid {

    private final Set<Cell> cells = new TreeSet<>();
    public final Set<Cell> nextTime = new TreeSet<>();

    public CellGrid() {

    }

    private CellGrid(CellGrid cellGrid, Cell cell) {
        this.cells.addAll(cellGrid.cells);
        this.nextTime.addAll(cellGrid.nextTime);
        this.cells.add(cell);
        addAllAndMe(cell);
    }

    private void addAllAndMe(Cell cell) {
        this.nextTime.addAll(cell.getNeighbours());
        this.nextTime.add(cell);
    }

    public CellGrid(CellGrid cg1, CellGrid cg2) {
        this.cells.addAll(cg1.cells);
        this.nextTime.addAll(cg1.nextTime);
        this.cells.addAll(cg2.cells);
        this.nextTime.addAll(cg2.nextTime);
    }

    public CellGrid(List<Cell> lstCells) {
        lstCells.stream().forEach(cell -> {
            this.cells.add(cell);
            addAllAndMe(cell);
        });
    }


    public CellGrid add(Cell o) {
        return new CellGrid(this, o);
    }

    public boolean isAlive(long x, long y) {
        return this.cells.contains(new Cell(x, y, null));
    }

    public long countNeighbourAlive(final long x, final long y) {
        final Collection<? extends Cell> neighbours = new Cell(x, y, null).getNeighbours();
        return this.cells.stream().filter(neighbours::contains).count();
    }

    @Override
    public String toString() {
        final Optional<Long> maxXOpt = this.cells.stream().max((o1, o2) -> ((Long) o1.getX()).compareTo(o2.getX())).map(Cell::getX);
        final Optional<Long> minXOpt = this.cells.stream().min((o1, o2) -> ((Long) o1.getX()).compareTo(o2.getX())).map(Cell::getX);
        final Optional<Long> maxYOpt = this.cells.stream().max((o1, o2) -> ((Long) o1.getY()).compareTo(o2.getY())).map(Cell::getY);
        final Optional<Long> minYOpt = this.cells.stream().min((o1, o2) -> ((Long) o1.getY()).compareTo(o2.getY())).map(Cell::getY);

        final StringBuilder sb = new StringBuilder();
        if (minXOpt.isPresent() && maxXOpt.isPresent() && minYOpt.isPresent() && maxYOpt.isPresent()) {
            for (long y = minYOpt.get(); y <= maxYOpt.get(); y++) {
                for (long x = minXOpt.get(); x <= maxXOpt.get(); x++) {
                    if (cells.contains(new Cell(x, y, null))) {
                        sb.append('#');
                    } else {
                        sb.append(' ');
                    }
                }
                sb.append('\n');
            }
        }
        return sb.toString();
    }

	public Cell get(long x, long y) {
		return this.cells.stream().filter(new Predicate<Cell>() {

			@Override
			public boolean test(Cell t) {

				return t.getX() == x && t.getY() == y;
			}
		}).findFirst().orElse(new Cell(x, y, null));
	}
}
