package fr.iatic.theisland.setup;

import fr.iatic.theisland.model.Board;
import fr.iatic.theisland.model.Boat;
import fr.iatic.theisland.model.BoatStatus;
import fr.iatic.theisland.model.Explorer;
import fr.iatic.theisland.model.ExplorerStatus;
import fr.iatic.theisland.model.HexCell;
import fr.iatic.theisland.model.HexCoordinate;
import fr.iatic.theisland.model.PieceState;
import fr.iatic.theisland.model.Player;
import fr.iatic.theisland.movement.ActionResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public final class AutoSetupService {

    private final InitialPlacementService placementService;

    public AutoSetupService(InitialPlacementService placementService) {
        this.placementService = Objects.requireNonNull(placementService, "Le service de placement est obligatoire.");
    }

    public void placeAllForDemo(PieceState state) {
        placeRemainingExplorers(state);
        placeRemainingBoats(state);
    }

    private void placeRemainingExplorers(PieceState state) {
        List<HexCoordinate> freeTerrainCoordinates = new ArrayList<>(
                state.getBoard().getInitialIslandSlotCoordinates().stream()
                        .filter(coordinate -> !state.hasAnyLandExplorerAt(coordinate))
                        .toList()
        );

        int coordinateIndex = 0;
        boolean progress;

        do {
            progress = false;
            for (Player player : state.getPlayers()) {
                Explorer explorer = player.getExplorers().stream()
                        .filter(candidate -> candidate.getStatus() == ExplorerStatus.UNPLACED)
                        .findFirst()
                        .orElse(null);

                if (explorer != null && coordinateIndex < freeTerrainCoordinates.size()) {
                    HexCoordinate coordinate = freeTerrainCoordinates.get(coordinateIndex++);
                    ActionResult result = placementService.placeExplorer(state, player, explorer, coordinate);
                    if (!result.isSuccess()) {
                        throw new IllegalStateException("Placement automatique impossible : " + result.getMessage());
                    }
                    progress = true;
                }
            }
        } while (progress);
    }

    private void placeRemainingBoats(PieceState state) {
        List<HexCoordinate> seaCoordinates = findValidBoatCoordinates(state);
        int index = 0;

        for (Player player : state.getPlayers()) {
            for (Boat boat : player.getSetupBoats()) {
                if (boat.getStatus() != BoatStatus.UNPLACED) {
                    continue;
                }

                while (index < seaCoordinates.size() && state.hasAnyBoatAt(seaCoordinates.get(index))) {
                    index++;
                }

                if (index >= seaCoordinates.size()) {
                    throw new IllegalStateException("Pas assez de cases de mer disponibles pour les bateaux.");
                }

                HexCoordinate coordinate = seaCoordinates.get(index++);
                ActionResult result = placementService.placeBoat(state, player, boat, coordinate);
                if (!result.isSuccess()) {
                    throw new IllegalStateException("Placement automatique impossible : " + result.getMessage());
                }
            }
        }
    }

    private List<HexCoordinate> findValidBoatCoordinates(PieceState state) {
        Board board = state.getBoard();
        List<HexCoordinate> coordinates = new ArrayList<>();

        for (HexCell cell : board.getAllCells()) {
            HexCoordinate coordinate = cell.getCoordinate();
            if (!cell.isSea()) {
                continue;
            }
            if (cell.isSeaSerpentSpawn()) {
                continue;
            }
            boolean adjacentToTerrain = board.getNeighbors(coordinate).stream()
                    .map(board::getCell)
                    .anyMatch(HexCell::hasTerrainTile);

            if (adjacentToTerrain) {
                coordinates.add(coordinate);
            }
        }

        return coordinates;
    }
}
