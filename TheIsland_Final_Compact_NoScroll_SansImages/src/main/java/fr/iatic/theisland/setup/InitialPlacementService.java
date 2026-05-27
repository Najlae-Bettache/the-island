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

import java.util.Objects;

/**
 * Vérifie et applique les placements de la préparation de partie.
 */
public final class InitialPlacementService {

    public ActionResult placeExplorer(
            PieceState state,
            Player player,
            Explorer explorer,
            HexCoordinate coordinate
    ) {
        Objects.requireNonNull(state, "L'état des pions est obligatoire.");
        Objects.requireNonNull(player, "Le joueur est obligatoire.");
        Objects.requireNonNull(explorer, "L'explorateur est obligatoire.");
        Objects.requireNonNull(coordinate, "La coordonnée est obligatoire.");

        if (explorer.getOwner() != player) {
            return ActionResult.failure("On ne peut placer que ses propres explorateurs.");
        }
        if (explorer.getStatus() != ExplorerStatus.UNPLACED) {
            return ActionResult.failure("Cet explorateur est déjà placé.");
        }

        Board board = state.getBoard();
        if (!board.contains(coordinate)) {
            return ActionResult.failure("La case demandée n'appartient pas au plateau.");
        }

        HexCell cell = board.getCell(coordinate);
        if (!cell.hasTerrainTile()) {
            return ActionResult.failure("Un explorateur doit être placé sur une tuile de terrain.");
        }
        if (state.hasAnyLandExplorerAt(coordinate)) {
            return ActionResult.failure("Cette tuile contient déjà un explorateur lors de la préparation.");
        }

        explorer.placeOnLand(coordinate);
        return ActionResult.success("Explorateur placé.");
    }

    public ActionResult placeBoat(
            PieceState state,
            Player player,
            Boat boat,
            HexCoordinate coordinate
    ) {
        Objects.requireNonNull(state, "L'état des pions est obligatoire.");
        Objects.requireNonNull(player, "Le joueur est obligatoire.");
        Objects.requireNonNull(boat, "Le bateau est obligatoire.");
        Objects.requireNonNull(coordinate, "La coordonnée est obligatoire.");

        if (boat.getSetupOwner() != player) {
            return ActionResult.failure("Ce bateau n'a pas été attribué à ce joueur pour la préparation.");
        }
        if (boat.getStatus() != BoatStatus.UNPLACED) {
            return ActionResult.failure("Ce bateau est déjà placé.");
        }

        Board board = state.getBoard();
        if (!board.contains(coordinate)) {
            return ActionResult.failure("La case demandée n'appartient pas au plateau.");
        }

        HexCell cell = board.getCell(coordinate);
        if (!cell.isSea()) {
            return ActionResult.failure("Un bateau doit être placé sur une case de mer.");
        }
        if (cell.isSeaSerpentSpawn()) {
            return ActionResult.failure("Cette case est réservée au départ d'un serpent de mer.");
        }
        if (state.hasAnyBoatAt(coordinate)) {
            return ActionResult.failure("La case de mer est déjà occupée par un bateau.");
        }
        if (!isAdjacentToAnyTerrainTile(board, coordinate)) {
            return ActionResult.failure("Le bateau doit être placé sur une mer voisine d'une tuile de terrain.");
        }

        boat.placeAt(coordinate);
        return ActionResult.success("Bateau placé.");
    }

    private boolean isAdjacentToAnyTerrainTile(Board board, HexCoordinate coordinate) {
        return board.getNeighbors(coordinate).stream()
                .map(board::getCell)
                .anyMatch(HexCell::hasTerrainTile);
    }
}
