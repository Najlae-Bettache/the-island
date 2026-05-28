package fr.iatic.theisland.setup;

import fr.iatic.theisland.model.Board;
import fr.iatic.theisland.model.HexCoordinate;
import fr.iatic.theisland.model.TerrainTile;

import java.util.ArrayList;
import java.util.List;


public final class BoardFactory {

    private static final int ROWS = 11;
    private static final int COLUMNS = 13;

    private BoardFactory() {
     
    }

    public static Board createInitialBoard() {
        Board board = new Board(
                ROWS,
                COLUMNS,
                createInitialIslandSlotCoordinates(),
                createRescueIslandCoordinates(),
                createSeaSerpentSpawnCoordinates()
        );

        List<TerrainTile> tiles = TerrainTileFactory.createShuffledTerrainTilesWithEarlyVolcano();
        board.placeIslandTiles(tiles);
        return board;
    }

   
    static List<HexCoordinate> createInitialIslandSlotCoordinates() {
        List<HexCoordinate> coordinates = new ArrayList<>(40);

        addRow(coordinates, 2, 5, 7);   // 3
        addRow(coordinates, 3, 4, 8);   // 5
        addRow(coordinates, 4, 3, 9);   // 7
        addRow(coordinates, 5, 3, 10);  // 8
        addRow(coordinates, 6, 3, 10);  // 8
        addRow(coordinates, 7, 4, 9);   // 6
        addRow(coordinates, 8, 5, 7);   // 3

        if (coordinates.size() != 40) {
            throw new IllegalStateException("La zone initiale doit contenir exactement 40 cases.");
        }

        return coordinates;
    }

    
    static List<HexCoordinate> createRescueIslandCoordinates() {
        return List.of(
                new HexCoordinate(0, 1),
                new HexCoordinate(1, 1),
                new HexCoordinate(0, 11),
                new HexCoordinate(1, 11),
                new HexCoordinate(9, 1),
                new HexCoordinate(10, 1),
                new HexCoordinate(9, 11),
                new HexCoordinate(10, 11)
        );
    }

    
    static List<HexCoordinate> createSeaSerpentSpawnCoordinates() {
        return List.of(
                new HexCoordinate(1, 6),
                new HexCoordinate(4, 1),
                new HexCoordinate(4, 11),
                new HexCoordinate(9, 5),
                new HexCoordinate(9, 8)
        );
    }

    private static void addRow(List<HexCoordinate> coordinates, int row, int startColumn, int endColumn) {
        for (int column = startColumn; column <= endColumn; column++) {
            coordinates.add(new HexCoordinate(row, column));
        }
    }
}
