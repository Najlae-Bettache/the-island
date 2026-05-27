package fr.iatic.theisland.movement;

import fr.iatic.theisland.model.Board;
import fr.iatic.theisland.model.Boat;
import fr.iatic.theisland.model.Explorer;
import fr.iatic.theisland.model.ExplorerStatus;
import fr.iatic.theisland.model.HexCoordinate;
import fr.iatic.theisland.model.PieceState;
import fr.iatic.theisland.model.Player;
import fr.iatic.theisland.setup.BoardFactory;
import fr.iatic.theisland.setup.GameSetupFactory;
import fr.iatic.theisland.setup.InitialPlacementService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests des règles de déplacement.
 */
class MovementServiceTest {

    @Test
    void landExplorerCanMoveToAdjacentTerrainTile() {
        TestWorld world = createWorld();
        Explorer explorer = world.redPlayer.getExplorers().get(0);
        HexCoordinate source = world.board.getInitialIslandSlotCoordinates().get(0);
        HexCoordinate target = findAdjacentTerrain(world.board, source);

        world.placement.placeExplorer(world.state, world.redPlayer, explorer, source);

        MovementContext context = new MovementContext(world.redPlayer);
        ActionResult result = world.movement.moveLandExplorerToTerrain(
                world.state,
                context,
                explorer,
                target
        );

        assertTrue(result.isSuccess());
        assertEquals(target, explorer.getCoordinate().orElseThrow());
        assertEquals(2, context.getRemainingMovementPoints());
    }

    @Test
    void explorerCanBoardBoatFromLand() {
        TestWorld world = createWorld();
        Explorer explorer = world.redPlayer.getExplorers().get(0);
        HexCoordinate land = world.board.getInitialIslandSlotCoordinates().get(0);
        HexCoordinate sea = findAdjacentSea(world.board, land);

        world.placement.placeExplorer(world.state, world.redPlayer, explorer, land);
        Boat boat = world.redPlayer.getSetupBoats().get(0);
        world.placement.placeBoat(world.state, world.redPlayer, boat, sea);

        MovementContext context = new MovementContext(world.redPlayer);
        ActionResult result = world.movement.boardBoatFromLand(world.state, context, explorer, boat);

        assertTrue(result.isSuccess());
        assertEquals(ExplorerStatus.IN_BOAT, explorer.getStatus());
        assertEquals(1, boat.getPassengers().size());
    }

    @Test
    void swimmerCannotMoveTwiceThroughSeaInSameTurn() {
        TestWorld world = createWorld();
        Explorer explorer = world.redPlayer.getExplorers().get(0);
        HexCoordinate land = world.board.getInitialIslandSlotCoordinates().get(0);
        HexCoordinate sea = findAdjacentSea(world.board, land);

        world.placement.placeExplorer(world.state, world.redPlayer, explorer, land);

        MovementContext context = new MovementContext(world.redPlayer);
        ActionResult enterWater = world.movement.enterWaterFromLand(world.state, context, explorer, sea);
        assertTrue(enterWater.isSuccess());

        HexCoordinate anotherSea = findAdjacentSea(world.board, sea);
        ActionResult secondSeaMove = world.movement.moveSwimmer(world.state, context, explorer, anotherSea);

        assertFalse(secondSeaMove.isSuccess());
    }

    @Test
    void emptyBoatCanBeMovedByAnyPlayer() {
        TestWorld world = createWorld();
        Boat boat = world.redPlayer.getSetupBoats().get(0);
        HexCoordinate sea = findAdjacentSea(world.board, world.board.getInitialIslandSlotCoordinates().get(0));
        world.placement.placeBoat(world.state, world.redPlayer, boat, sea);

        HexCoordinate targetSea = findAdjacentSea(world.board, sea);
        MovementContext blueTurn = new MovementContext(world.bluePlayer);
        ActionResult result = world.movement.moveBoat(world.state, blueTurn, boat, targetSea);

        assertTrue(result.isSuccess());
    }

    @Test
    void occupiedBoatIsControlledByPassengerMajority() {
        TestWorld world = createWorld();
        Boat boat = world.redPlayer.getSetupBoats().get(0);

        HexCoordinate sea = findSeaAdjacentToAtLeastTerrain(world.board, 3);
        List<HexCoordinate> adjacentTerrain = world.board.getNeighbors(sea).stream()
                .filter(coordinate -> world.board.getCell(coordinate).hasTerrainTile())
                .limit(3)
                .toList();

        HexCoordinate redLand1 = adjacentTerrain.get(0);
        HexCoordinate redLand2 = adjacentTerrain.get(1);
        HexCoordinate blueLand = adjacentTerrain.get(2);

        Explorer red1 = world.redPlayer.getExplorers().get(0);
        Explorer red2 = world.redPlayer.getExplorers().get(1);
        Explorer blue1 = world.bluePlayer.getExplorers().get(0);

        world.placement.placeExplorer(world.state, world.redPlayer, red1, redLand1);
        world.placement.placeExplorer(world.state, world.redPlayer, red2, redLand2);
        world.placement.placeExplorer(world.state, world.bluePlayer, blue1, blueLand);
        world.placement.placeBoat(world.state, world.redPlayer, boat, sea);

        assertTrue(world.movement.boardBoatFromLand(
                world.state,
                new MovementContext(world.redPlayer),
                red1,
                boat
        ).isSuccess());
        assertTrue(world.movement.boardBoatFromLand(
                world.state,
                new MovementContext(world.redPlayer),
                red2,
                boat
        ).isSuccess());
        assertTrue(world.movement.boardBoatFromLand(
                world.state,
                new MovementContext(world.bluePlayer),
                blue1,
                boat
        ).isSuccess());

        HexCoordinate targetSea = findAdjacentSea(world.board, sea);
        ActionResult blueMove = world.movement.moveBoat(
                world.state,
                new MovementContext(world.bluePlayer),
                boat,
                targetSea
        );

        assertFalse(blueMove.isSuccess());
    }


    @Test
    void boatCannotMoveOntoAnotherBoatCell() {
        TestWorld world = createWorld();

        Boat movingBoat = world.redPlayer.getSetupBoats().get(0);
        Boat blockingBoat = world.bluePlayer.getSetupBoats().get(0);

        HexCoordinate movingSea = findAdjacentSea(world.board, world.board.getInitialIslandSlotCoordinates().get(0));
        world.placement.placeBoat(world.state, world.redPlayer, movingBoat, movingSea);

        HexCoordinate occupiedSea = world.board.getNeighbors(movingSea).stream()
                .filter(coordinate -> world.board.getCell(coordinate).isSea())
                .filter(coordinate -> !world.board.getCell(coordinate).isSeaSerpentSpawn())
                .filter(coordinate -> !coordinate.equals(movingSea))
                .filter(coordinate -> world.board.getNeighbors(coordinate).stream()
                        .map(world.board::getCell)
                        .anyMatch(cell -> cell.hasTerrainTile()))
                .findFirst()
                .orElseThrow();

        world.placement.placeBoat(world.state, world.bluePlayer, blockingBoat, occupiedSea);

        ActionResult result = world.movement.moveBoat(
                world.state,
                new MovementContext(world.redPlayer),
                movingBoat,
                occupiedSea
        );

        assertFalse(result.isSuccess());
    }

    private TestWorld createWorld() {
        Board board = BoardFactory.createInitialBoard();
        PieceState state = GameSetupFactory.createTwoPlayerPieceState(board);
        Player redPlayer = state.getPlayers().get(0);
        Player bluePlayer = state.getPlayers().get(1);
        return new TestWorld(board, state, redPlayer, bluePlayer, new InitialPlacementService(), new MovementService());
    }

    private HexCoordinate findAdjacentTerrain(Board board, HexCoordinate source) {
        return board.getNeighbors(source).stream()
                .filter(coordinate -> board.getCell(coordinate).hasTerrainTile())
                .findFirst()
                .orElseThrow();
    }

    private HexCoordinate findAdjacentSea(Board board, HexCoordinate source) {
        return board.getNeighbors(source).stream()
                .filter(coordinate -> board.getCell(coordinate).isSea())
                .filter(coordinate -> !board.getCell(coordinate).isSeaSerpentSpawn())
                .findFirst()
                .orElseThrow();
    }

    private HexCoordinate findSeaAdjacentToAtLeastTerrain(Board board, int minimumTerrainNeighbors) {
        return board.getAllCells().stream()
                .filter(cell -> cell.isSea())
                .map(cell -> cell.getCoordinate())
                .filter(coordinate -> !board.getCell(coordinate).isSeaSerpentSpawn())
                .filter(coordinate -> board.getNeighbors(coordinate).stream()
                        .filter(neighbor -> board.getCell(neighbor).hasTerrainTile())
                        .count() >= minimumTerrainNeighbors)
                .findFirst()
                .orElseThrow();
    }

    private record TestWorld(
            Board board,
            PieceState state,
            Player redPlayer,
            Player bluePlayer,
            InitialPlacementService placement,
            MovementService movement
    ) {
    }
}
