package fr.iatic.theisland.model;

import fr.iatic.theisland.setup.BoardFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests du plateau initial.
 */
class BoardInitializationTest {

    @Test
    void boardContainsFortyTerrainTiles() {
        Board board = BoardFactory.createInitialBoard();
        assertEquals(40, board.countPlacedTerrainTiles());
    }

    @Test
    void boardContainsExpectedTerrainDistribution() {
        Board board = BoardFactory.createInitialBoard();
        assertEquals(16, board.countPlacedTerrainTilesOfType(TerrainType.BEACH));
        assertEquals(16, board.countPlacedTerrainTilesOfType(TerrainType.FOREST));
        assertEquals(8, board.countPlacedTerrainTilesOfType(TerrainType.MOUNTAIN));
    }

    @Test
    void boardContainsRescueIslandsAndSerpentSpawns() {
        Board board = BoardFactory.createInitialBoard();
        assertEquals(8, board.countRescueIslands());
        assertEquals(5, board.countSeaSerpentSpawns());
    }

    @Test
    void centerCellHasSixNeighbors() {
        Board board = BoardFactory.createInitialBoard();
        assertEquals(6, board.getNeighbors(new HexCoordinate(5, 6)).size());
    }

    @Test
    void adjacencyIsSymmetric() {
        Board board = BoardFactory.createInitialBoard();
        HexCoordinate first = new HexCoordinate(5, 6);
        HexCoordinate second = board.getNeighbors(first).get(0);

        assertTrue(board.areAdjacent(first, second));
        assertTrue(board.areAdjacent(second, first));
    }
}
