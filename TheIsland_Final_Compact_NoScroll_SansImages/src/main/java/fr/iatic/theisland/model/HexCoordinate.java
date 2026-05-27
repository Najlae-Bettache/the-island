package fr.iatic.theisland.model;

import java.util.Objects;

/**
 * Coordonnée ligne/colonne d'une case hexagonale.
 */
public final class HexCoordinate {

    private final int row;
    private final int column;

    public HexCoordinate(int row, int column) {
        if (row < 0 || column < 0) {
            throw new IllegalArgumentException("Les coordonnées ne peuvent pas être négatives.");
        }
        this.row = row;
        this.column = column;
    }

    public int getRow() {
        return row;
    }

    public int getColumn() {
        return column;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof HexCoordinate)) {
            return false;
        }
        HexCoordinate that = (HexCoordinate) other;
        return row == that.row && column == that.column;
    }

    @Override
    public int hashCode() {
        return Objects.hash(row, column);
    }

    @Override
    public String toString() {
        return "(" + row + ", " + column + ")";
    }
}
