package fr.iatic.theisland.setup;

import fr.iatic.theisland.model.Board;
import fr.iatic.theisland.model.Boat;
import fr.iatic.theisland.model.Explorer;
import fr.iatic.theisland.model.HexCoordinate;
import fr.iatic.theisland.model.PieceState;
import fr.iatic.theisland.model.Player;
import fr.iatic.theisland.movement.ActionResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests de préparation de partie.
 */
class InitialPlacementServiceTest {

    @Test
    void explorerCanBePlacedOnFreeTerrainTile() {
        Board board = BoardFactory.createInitialBoard();
        PieceState state = GameSetupFactory.createTwoPlayerPieceState(board);
        InitialPlacementService service = new InitialPlacementService();

        Player player = state.getPlayers().get(0);
        Explorer explorer = player.getExplorers().get(0);
        HexCoordinate coordinate = board.getInitialIslandSlotCoordinates().get(0);

        ActionResult result = service.placeExplorer(state, player, explorer, coordinate);
        assertTrue(result.isSuccess());
    }

    @Test
    void explorerCannotBePlacedOnSea() {
        Board board = BoardFactory.createInitialBoard();
        PieceState state = GameSetupFactory.createTwoPlayerPieceState(board);
        InitialPlacementService service = new InitialPlacementService();

        Player player = state.getPlayers().get(0);
        Explorer explorer = player.getExplorers().get(0);

        ActionResult result = service.placeExplorer(state, player, explorer, new HexCoordinate(0, 0));
        assertFalse(result.isSuccess());
    }

    @Test
    void boatCanBePlacedOnSeaAdjacentToTerrain() {
        Board board = BoardFactory.createInitialBoard();
        PieceState state = GameSetupFactory.createTwoPlayerPieceState(board);
        InitialPlacementService service = new InitialPlacementService();

        Player player = state.getPlayers().get(0);
        Boat boat = player.getSetupBoats().get(0);

        HexCoordinate validSea = findAdjacentSeaCell(board);
        ActionResult result = service.placeBoat(state, player, boat, validSea);
        assertTrue(result.isSuccess());
    }

    private HexCoordinate findAdjacentSeaCell(Board board) {
        return board.getAllCells().stream()
                .filter(cell -> cell.isSea())
                .map(cell -> cell.getCoordinate())
                .filter(coordinate -> board.getNeighbors(coordinate).stream()
                        .map(board::getCell)
                        .anyMatch(cell -> cell.hasTerrainTile()))
                .filter(coordinate -> !board.getCell(coordinate).isSeaSerpentSpawn())
                .findFirst()
                .orElseThrow();
    }
}
