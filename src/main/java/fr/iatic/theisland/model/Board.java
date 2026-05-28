package fr.iatic.theisland.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Plateau logique du jeu.
 */
public final class Board {

    private final int rows;
    private final int columns;
    private final HexCell[][] cells;
    private final List<HexCoordinate> initialIslandSlotCoordinates;
    private final List<HexCoordinate> rescueIslandCoordinates;
    private final List<HexCoordinate> seaSerpentSpawnCoordinates;

    public Board(
            int rows,
            int columns,
            List<HexCoordinate> initialIslandSlotCoordinates,
            List<HexCoordinate> rescueIslandCoordinates,
            List<HexCoordinate> seaSerpentSpawnCoordinates
    ) {
        if (rows <= 0 || columns <= 0) {
            throw new IllegalArgumentException("Le plateau doit avoir des dimensions positives.");
        }

        this.rows = rows;
        this.columns = columns;
        this.initialIslandSlotCoordinates = List.copyOf(
                Objects.requireNonNull(initialIslandSlotCoordinates, "Les emplacements de l'île sont obligatoires.")
        );
        this.rescueIslandCoordinates = List.copyOf(
                Objects.requireNonNull(rescueIslandCoordinates, "Les abris sont obligatoires.")
        );
        this.seaSerpentSpawnCoordinates = List.copyOf(
                Objects.requireNonNull(seaSerpentSpawnCoordinates, "Les départs de serpents sont obligatoires.")
        );

        validateCoordinates();
        validateNoDuplicates(this.initialIslandSlotCoordinates, "emplacements d'île");
        validateNoDuplicates(this.rescueIslandCoordinates, "abris");
        validateNoDuplicates(this.seaSerpentSpawnCoordinates, "départs de serpents");
        validateNoIllegalOverlaps();

        this.cells = new HexCell[rows][columns];
        initializeCells();
    }

    private void validateCoordinates() {
        for (HexCoordinate coordinate : allSpecialCoordinates()) {
            if (!contains(coordinate)) {
                throw new IllegalArgumentException("Coordonnée hors plateau : " + coordinate);
            }
        }
    }

    private void validateNoDuplicates(List<HexCoordinate> coordinates, String label) {
        Set<HexCoordinate> unique = new HashSet<>(coordinates);
        if (unique.size() != coordinates.size()) {
            throw new IllegalArgumentException("Coordonnées dupliquées dans " + label + ".");
        }
    }

    private void validateNoIllegalOverlaps() {
        assertNoOverlap(initialIslandSlotCoordinates, rescueIslandCoordinates,
                "Une case ne peut pas être à la fois un emplacement de tuile et un abri.");
        assertNoOverlap(initialIslandSlotCoordinates, seaSerpentSpawnCoordinates,
                "Un serpent ne démarre pas sur une tuile initiale.");
        assertNoOverlap(rescueIslandCoordinates, seaSerpentSpawnCoordinates,
                "Un serpent ne démarre pas sur un abri.");
    }

    private void assertNoOverlap(
            List<HexCoordinate> first,
            List<HexCoordinate> second,
            String errorMessage
    ) {
        Set<HexCoordinate> overlap = new HashSet<>(first);
        overlap.retainAll(second);
        if (!overlap.isEmpty()) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private List<HexCoordinate> allSpecialCoordinates() {
        List<HexCoordinate> result = new ArrayList<>();
        result.addAll(initialIslandSlotCoordinates);
        result.addAll(rescueIslandCoordinates);
        result.addAll(seaSerpentSpawnCoordinates);
        return result;
    }

    private void initializeCells() {
        Set<HexCoordinate> islandSlots = new HashSet<>(initialIslandSlotCoordinates);
        Set<HexCoordinate> rescueIslands = new HashSet<>(rescueIslandCoordinates);
        Set<HexCoordinate> serpentSpawns = new HashSet<>(seaSerpentSpawnCoordinates);

        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                HexCoordinate coordinate = new HexCoordinate(row, column);
                BaseCellType baseType = rescueIslands.contains(coordinate)
                        ? BaseCellType.RESCUE_ISLAND
                        : BaseCellType.SEA;

                cells[row][column] = new HexCell(
                        coordinate,
                        baseType,
                        islandSlots.contains(coordinate),
                        serpentSpawns.contains(coordinate)
                );
            }
        }
    }

    public boolean contains(HexCoordinate coordinate) {
        Objects.requireNonNull(coordinate, "La coordonnée est obligatoire.");
        return coordinate.getRow() < rows && coordinate.getColumn() < columns;
    }

    public HexCell getCell(HexCoordinate coordinate) {
        Objects.requireNonNull(coordinate, "La coordonnée est obligatoire.");
        if (!contains(coordinate)) {
            throw new IllegalArgumentException("Coordonnée hors plateau : " + coordinate);
        }
        return cells[coordinate.getRow()][coordinate.getColumn()];
    }

    public HexCell getCell(int row, int column) {
        return getCell(new HexCoordinate(row, column));
    }

    public List<HexCell> getAllCells() {
        List<HexCell> result = new ArrayList<>(rows * columns);
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                result.add(cells[row][column]);
            }
        }
        return Collections.unmodifiableList(result);
    }

    public List<HexCoordinate> getNeighbors(HexCoordinate coordinate) {
        Objects.requireNonNull(coordinate, "La coordonnée est obligatoire.");
        if (!contains(coordinate)) {
            throw new IllegalArgumentException("Coordonnée hors plateau : " + coordinate);
        }

        int row = coordinate.getRow();
        int column = coordinate.getColumn();
        List<HexCoordinate> candidates = new ArrayList<>(6);

        addCandidate(candidates, row - 1, column);
        addCandidate(candidates, row + 1, column);

        if (column % 2 == 0) {
            addCandidate(candidates, row - 1, column - 1);
            addCandidate(candidates, row, column - 1);
            addCandidate(candidates, row - 1, column + 1);
            addCandidate(candidates, row, column + 1);
        } else {
            addCandidate(candidates, row, column - 1);
            addCandidate(candidates, row + 1, column - 1);
            addCandidate(candidates, row, column + 1);
            addCandidate(candidates, row + 1, column + 1);
        }

        List<HexCoordinate> valid = new ArrayList<>(6);
        for (HexCoordinate candidate : candidates) {
            if (contains(candidate)) {
                valid.add(candidate);
            }
        }
        return Collections.unmodifiableList(valid);
    }

    private void addCandidate(List<HexCoordinate> coordinates, int row, int column) {
        if (row >= 0 && column >= 0) {
            coordinates.add(new HexCoordinate(row, column));
        }
    }

    public boolean areAdjacent(HexCoordinate first, HexCoordinate second) {
        Objects.requireNonNull(first, "La première coordonnée est obligatoire.");
        Objects.requireNonNull(second, "La deuxième coordonnée est obligatoire.");
        return getNeighbors(first).contains(second);
    }

    public void placeIslandTiles(List<TerrainTile> tiles) {
        Objects.requireNonNull(tiles, "La liste de tuiles est obligatoire.");
        if (tiles.size() != initialIslandSlotCoordinates.size()) {
            throw new IllegalArgumentException(
                    "Il faut exactement " + initialIslandSlotCoordinates.size()
                            + " tuiles, reçu : " + tiles.size()
            );
        }

        for (int index = 0; index < initialIslandSlotCoordinates.size(); index++) {
            HexCoordinate coordinate = initialIslandSlotCoordinates.get(index);
            getCell(coordinate).placeTerrainTile(tiles.get(index));
        }
    }

    public long countPlacedTerrainTiles() {
        return getAllCells().stream()
                .filter(HexCell::hasTerrainTile)
                .count();
    }

    public long countPlacedTerrainTilesOfType(TerrainType type) {
        Objects.requireNonNull(type, "Le type est obligatoire.");
        return getAllCells().stream()
                .filter(HexCell::hasTerrainTile)
                .map(cell -> cell.getTerrainTile().orElseThrow().getType())
                .filter(foundType -> foundType == type)
                .count();
    }

    public long countRescueIslands() {
        return getAllCells().stream()
                .filter(HexCell::isRescueIsland)
                .count();
    }

    public long countSeaSerpentSpawns() {
        return getAllCells().stream()
                .filter(HexCell::isSeaSerpentSpawn)
                .count();
    }

    public List<HexCoordinate> getInitialIslandSlotCoordinates() {
        return Collections.unmodifiableList(initialIslandSlotCoordinates);
    }

    public List<HexCoordinate> getRescueIslandCoordinates() {
        return Collections.unmodifiableList(rescueIslandCoordinates);
    }

    public List<HexCoordinate> getSeaSerpentSpawnCoordinates() {
        return Collections.unmodifiableList(seaSerpentSpawnCoordinates);
    }

    public int getRows() {
        return rows;
    }

    public int getColumns() {
        return columns;
    }
}
