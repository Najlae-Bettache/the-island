package fr.iatic.theisland.movement;

import fr.iatic.theisland.model.Board;
import fr.iatic.theisland.model.Boat;
import fr.iatic.theisland.model.BoatStatus;
import fr.iatic.theisland.model.Explorer;
import fr.iatic.theisland.model.ExplorerStatus;
import fr.iatic.theisland.model.HexCell;
import fr.iatic.theisland.model.HexCoordinate;
import fr.iatic.theisland.model.PieceState;

import java.util.Objects;

/**
 * Règles de déplacements des explorateurs et des bateaux.
 */
public final class MovementService {

    private final BoatControlService boatControlService;

    public MovementService() {
        this(new BoatControlService());
    }

    public MovementService(BoatControlService boatControlService) {
        this.boatControlService = Objects.requireNonNull(boatControlService, "Le service de contrôle est obligatoire.");
    }

    public ActionResult moveLandExplorerToTerrain(
            PieceState state,
            MovementContext context,
            Explorer explorer,
            HexCoordinate targetCoordinate
    ) {
        ActionResult commonResult = validateOwnedExplorerAndMovementPoint(context, explorer);
        if (!commonResult.isSuccess()) {
            return commonResult;
        }
        Objects.requireNonNull(state, "L'état des pions est obligatoire.");
        Objects.requireNonNull(targetCoordinate, "La destination est obligatoire.");

        if (explorer.getStatus() != ExplorerStatus.ON_LAND) {
            return ActionResult.failure("Seul un explorateur sur terre peut se déplacer sur une tuile.");
        }
        if (explorer.hasLeftMainIsland()) {
            return ActionResult.failure("Un explorateur ayant quitté l'île principale ne peut pas y revenir.");
        }

        Board board = state.getBoard();
        HexCoordinate source = explorer.getCoordinate().orElseThrow();

        if (!board.contains(targetCoordinate) || !board.areAdjacent(source, targetCoordinate)) {
            return ActionResult.failure("La tuile cible doit être adjacente.");
        }
        if (!board.getCell(targetCoordinate).hasTerrainTile()) {
            return ActionResult.failure("La destination doit être une tuile de terrain.");
        }

        if (!context.tryConsumeOneMovementPoint()) {
            return ActionResult.failure("Il ne reste plus de déplacement.");
        }
        explorer.moveOnLand(targetCoordinate);
        return ActionResult.success("Explorateur déplacé sur une tuile adjacente.");
    }

    public ActionResult boardBoatFromLand(
            PieceState state,
            MovementContext context,
            Explorer explorer,
            Boat targetBoat
    ) {
        ActionResult commonResult = validateOwnedExplorerAndMovementPoint(context, explorer);
        if (!commonResult.isSuccess()) {
            return commonResult;
        }
        Objects.requireNonNull(state, "L'état des pions est obligatoire.");
        Objects.requireNonNull(targetBoat, "Le bateau est obligatoire.");

        if (explorer.getStatus() != ExplorerStatus.ON_LAND) {
            return ActionResult.failure("Seul un explorateur sur terre peut monter depuis la terre.");
        }
        if (targetBoat.getStatus() != BoatStatus.ON_SEA) {
            return ActionResult.failure("Le bateau cible n'est pas sur la mer.");
        }
        if (!targetBoat.hasFreeSeat()) {
            return ActionResult.failure("Le bateau est complet.");
        }

        Board board = state.getBoard();
        HexCoordinate explorerCoordinate = explorer.getCoordinate().orElseThrow();
        HexCoordinate boatCoordinate = targetBoat.getCoordinate().orElseThrow();

        if (!board.areAdjacent(explorerCoordinate, boatCoordinate)) {
            return ActionResult.failure("Le bateau doit être sur une case de mer adjacente.");
        }

        if (!context.tryConsumeOneMovementPoint()) {
            return ActionResult.failure("Il ne reste plus de déplacement.");
        }
        targetBoat.addPassenger(explorer);
        explorer.boardBoat(targetBoat.getId());
        return ActionResult.success("Explorateur monté dans le bateau.");
    }

    public ActionResult enterWaterFromLand(
            PieceState state,
            MovementContext context,
            Explorer explorer,
            HexCoordinate targetSeaCoordinate
    ) {
        ActionResult commonResult = validateOwnedExplorerAndMovementPoint(context, explorer);
        if (!commonResult.isSuccess()) {
            return commonResult;
        }
        Objects.requireNonNull(state, "L'état des pions est obligatoire.");
        Objects.requireNonNull(targetSeaCoordinate, "La destination est obligatoire.");

        if (explorer.getStatus() != ExplorerStatus.ON_LAND) {
            return ActionResult.failure("Seul un explorateur sur terre peut entrer dans l'eau depuis la terre.");
        }

        Board board = state.getBoard();
        HexCoordinate source = explorer.getCoordinate().orElseThrow();

        if (!board.contains(targetSeaCoordinate) || !board.areAdjacent(source, targetSeaCoordinate)) {
            return ActionResult.failure("La case de mer cible doit être adjacente.");
        }
        if (!board.getCell(targetSeaCoordinate).isSea()) {
            return ActionResult.failure("La destination doit être une case de mer.");
        }

        if (!context.tryConsumeOneMovementPoint()) {
            return ActionResult.failure("Il ne reste plus de déplacement.");
        }
        explorer.becomeSwimmer(targetSeaCoordinate);
        context.markSeaMovementUsed(explorer);
        return ActionResult.success("L'explorateur devient nageur.");
    }

    public ActionResult moveSwimmer(
            PieceState state,
            MovementContext context,
            Explorer explorer,
            HexCoordinate targetSeaCoordinate
    ) {
        ActionResult commonResult = validateOwnedExplorerAndMovementPoint(context, explorer);
        if (!commonResult.isSuccess()) {
            return commonResult;
        }
        Objects.requireNonNull(state, "L'état des pions est obligatoire.");
        Objects.requireNonNull(targetSeaCoordinate, "La destination est obligatoire.");

        if (explorer.getStatus() != ExplorerStatus.SWIMMER) {
            return ActionResult.failure("Seul un nageur peut se déplacer en mer.");
        }
        if (context.hasAlreadyMovedThroughSea(explorer)) {
            return ActionResult.failure("Ce nageur a déjà effectué son déplacement de case de mer ce tour.");
        }

        Board board = state.getBoard();
        HexCoordinate source = explorer.getCoordinate().orElseThrow();

        if (!board.contains(targetSeaCoordinate) || !board.areAdjacent(source, targetSeaCoordinate)) {
            return ActionResult.failure("Un nageur se déplace d'une seule case de mer adjacente.");
        }
        if (!board.getCell(targetSeaCoordinate).isSea()) {
            return ActionResult.failure("La destination doit être une case de mer.");
        }

        if (!context.tryConsumeOneMovementPoint()) {
            return ActionResult.failure("Il ne reste plus de déplacement.");
        }
        explorer.moveAsSwimmer(targetSeaCoordinate);
        context.markSeaMovementUsed(explorer);
        return ActionResult.success("Nageur déplacé d'une case de mer.");
    }

    public ActionResult boardBoatFromWater(
            PieceState state,
            MovementContext context,
            Explorer explorer,
            Boat targetBoat
    ) {
        ActionResult commonResult = validateOwnedExplorerAndMovementPoint(context, explorer);
        if (!commonResult.isSuccess()) {
            return commonResult;
        }
        Objects.requireNonNull(state, "L'état des pions est obligatoire.");
        Objects.requireNonNull(targetBoat, "Le bateau est obligatoire.");

        if (explorer.getStatus() != ExplorerStatus.SWIMMER) {
            return ActionResult.failure("Seul un nageur peut monter dans un bateau depuis la mer.");
        }
        if (targetBoat.getStatus() != BoatStatus.ON_SEA) {
            return ActionResult.failure("Le bateau cible n'est pas sur la mer.");
        }
        if (!targetBoat.hasFreeSeat()) {
            return ActionResult.failure("Le bateau est complet.");
        }

        HexCoordinate swimmerCoordinate = explorer.getCoordinate().orElseThrow();
        HexCoordinate boatCoordinate = targetBoat.getCoordinate().orElseThrow();

        if (!swimmerCoordinate.equals(boatCoordinate)) {
            return ActionResult.failure("Le nageur et le bateau doivent être sur la même case de mer.");
        }

        if (!context.tryConsumeOneMovementPoint()) {
            return ActionResult.failure("Il ne reste plus de déplacement.");
        }
        targetBoat.addPassenger(explorer);
        explorer.boardBoat(targetBoat.getId());
        return ActionResult.success("Le nageur monte dans le bateau.");
    }

    public ActionResult transferExplorerBetweenAdjacentBoats(
            PieceState state,
            MovementContext context,
            Explorer explorer,
            Boat targetBoat
    ) {
        ActionResult commonResult = validateOwnedExplorerAndMovementPoint(context, explorer);
        if (!commonResult.isSuccess()) {
            return commonResult;
        }
        Objects.requireNonNull(state, "L'état des pions est obligatoire.");
        Objects.requireNonNull(targetBoat, "Le bateau cible est obligatoire.");

        if (explorer.getStatus() != ExplorerStatus.IN_BOAT) {
            return ActionResult.failure("L'explorateur doit déjà être dans un bateau.");
        }
        if (targetBoat.getStatus() != BoatStatus.ON_SEA) {
            return ActionResult.failure("Le bateau cible n'est pas placé.");
        }
        if (!targetBoat.hasFreeSeat()) {
            return ActionResult.failure("Le bateau cible est complet.");
        }

        Boat sourceBoat = findCurrentBoat(state, explorer);
        if (sourceBoat == targetBoat) {
            return ActionResult.failure("Le bateau cible doit être différent.");
        }

        Board board = state.getBoard();
        HexCoordinate sourceCoordinate = sourceBoat.getCoordinate().orElseThrow();
        HexCoordinate targetCoordinate = targetBoat.getCoordinate().orElseThrow();

        if (!board.areAdjacent(sourceCoordinate, targetCoordinate)) {
            return ActionResult.failure("Les deux bateaux doivent être sur des cases de mer adjacentes.");
        }

        if (!context.tryConsumeOneMovementPoint()) {
            return ActionResult.failure("Il ne reste plus de déplacement.");
        }
        sourceBoat.removePassenger(explorer);
        targetBoat.addPassenger(explorer);
        explorer.boardBoat(targetBoat.getId());
        return ActionResult.success("Explorateur transféré dans un bateau adjacent.");
    }

    public ActionResult jumpFromBoatToSea(
            PieceState state,
            MovementContext context,
            Explorer explorer
    ) {
        ActionResult commonResult = validateOwnedExplorerAndMovementPoint(context, explorer);
        if (!commonResult.isSuccess()) {
            return commonResult;
        }
        Objects.requireNonNull(state, "L'état des pions est obligatoire.");

        if (explorer.getStatus() != ExplorerStatus.IN_BOAT) {
            return ActionResult.failure("L'explorateur doit être dans un bateau.");
        }

        Boat boat = findCurrentBoat(state, explorer);
        HexCoordinate seaCoordinate = boat.getCoordinate().orElseThrow();

        if (!context.tryConsumeOneMovementPoint()) {
            return ActionResult.failure("Il ne reste plus de déplacement.");
        }
        boat.removePassenger(explorer);
        explorer.becomeSwimmer(seaCoordinate);
        context.markSeaMovementUsed(explorer);
        return ActionResult.success("L'explorateur saute du bateau et devient nageur.");
    }

    public ActionResult moveBoat(
            PieceState state,
            MovementContext context,
            Boat boat,
            HexCoordinate targetSeaCoordinate
    ) {
        Objects.requireNonNull(state, "L'état des pions est obligatoire.");
        Objects.requireNonNull(context, "Le contexte de déplacement est obligatoire.");
        Objects.requireNonNull(boat, "Le bateau est obligatoire.");
        Objects.requireNonNull(targetSeaCoordinate, "La destination est obligatoire.");

        if (!context.hasAtLeastOneMovementPoint()) {
            return ActionResult.failure("Il ne reste plus de déplacement.");
        }
        if (boat.getStatus() != BoatStatus.ON_SEA) {
            return ActionResult.failure("Seul un bateau déjà placé peut être déplacé.");
        }
        if (!boatControlService.canPlayerMoveBoat(context.getActivePlayer(), boat)) {
            return ActionResult.failure("Ce joueur ne contrôle pas ce bateau.");
        }

        Board board = state.getBoard();
        HexCoordinate sourceCoordinate = boat.getCoordinate().orElseThrow();

        if (!board.contains(targetSeaCoordinate) || !board.areAdjacent(sourceCoordinate, targetSeaCoordinate)) {
            return ActionResult.failure("Un bateau se déplace vers une case de mer adjacente.");
        }

        HexCell targetCell = board.getCell(targetSeaCoordinate);
        if (!targetCell.isSea()) {
            return ActionResult.failure("La destination du bateau doit être une case de mer.");
        }
        if (state.hasAnyBoatAt(targetSeaCoordinate)) {
            return ActionResult.failure("Cette case de mer contient déjà un bateau.");
        }

        if (!context.tryConsumeOneMovementPoint()) {
            return ActionResult.failure("Il ne reste plus de déplacement.");
        }
        boat.moveTo(targetSeaCoordinate);
        return ActionResult.success("Bateau déplacé d'une case de mer.");
    }

    public ActionResult disembarkExplorerToRescueIsland(
            PieceState state,
            MovementContext context,
            Explorer explorer,
            HexCoordinate rescueIslandCoordinate
    ) {
        ActionResult commonResult = validateOwnedExplorerAndMovementPoint(context, explorer);
        if (!commonResult.isSuccess()) {
            return commonResult;
        }
        Objects.requireNonNull(state, "L'état des pions est obligatoire.");
        Objects.requireNonNull(rescueIslandCoordinate, "La case d'abri est obligatoire.");

        if (explorer.getStatus() != ExplorerStatus.IN_BOAT) {
            return ActionResult.failure("Seul un explorateur dans un bateau peut débarquer.");
        }

        Board board = state.getBoard();
        if (!board.contains(rescueIslandCoordinate)) {
            return ActionResult.failure("La case d'abri n'appartient pas au plateau.");
        }
        if (!board.getCell(rescueIslandCoordinate).isRescueIsland()) {
            return ActionResult.failure("La destination doit être une île de sauvetage.");
        }

        Boat currentBoat = findCurrentBoat(state, explorer);
        HexCoordinate boatCoordinate = currentBoat.getCoordinate().orElseThrow();

        if (!board.areAdjacent(boatCoordinate, rescueIslandCoordinate)) {
            return ActionResult.failure("Le bateau doit être adjacent à l'île de sauvetage.");
        }

        if (!context.tryConsumeOneMovementPoint()) {
            return ActionResult.failure("Il ne reste plus de déplacement.");
        }
        currentBoat.removePassenger(explorer);
        explorer.saveOnRescueIsland(rescueIslandCoordinate);
        return ActionResult.success("Explorateur sauvé.");
    }

    private ActionResult validateOwnedExplorerAndMovementPoint(MovementContext context, Explorer explorer) {
        Objects.requireNonNull(context, "Le contexte de déplacement est obligatoire.");
        Objects.requireNonNull(explorer, "L'explorateur est obligatoire.");

        if (!context.hasAtLeastOneMovementPoint()) {
            return ActionResult.failure("Il ne reste plus de déplacement.");
        }
        if (explorer.getOwner() != context.getActivePlayer()) {
            return ActionResult.failure("Un joueur ne peut pas déplacer l'explorateur d'un autre joueur.");
        }
        return ActionResult.success("Validation commune réussie.");
    }

    private Boat findCurrentBoat(PieceState state, Explorer explorer) {
        String boatId = explorer.getBoatId().orElseThrow(
                () -> new IllegalStateException("L'explorateur n'est lié à aucun bateau.")
        );
        return state.findBoatById(boatId).orElseThrow(
                () -> new IllegalStateException("Le bateau de l'explorateur est introuvable.")
        );
    }
}
