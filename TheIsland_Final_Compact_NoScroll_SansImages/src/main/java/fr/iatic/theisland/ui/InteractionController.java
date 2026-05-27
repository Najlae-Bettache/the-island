package fr.iatic.theisland.ui;

import fr.iatic.theisland.engine.CreatureManager;
import fr.iatic.theisland.engine.TileRemovalService;
import fr.iatic.theisland.model.Board;
import fr.iatic.theisland.model.Boat;
import fr.iatic.theisland.model.BoatStatus;
import fr.iatic.theisland.model.Creature;
import fr.iatic.theisland.model.CreatureType;
import fr.iatic.theisland.model.Explorer;
import fr.iatic.theisland.model.ExplorerStatus;
import fr.iatic.theisland.model.GameState;
import fr.iatic.theisland.model.HexCell;
import fr.iatic.theisland.model.HexCoordinate;
import fr.iatic.theisland.model.PieceState;
import fr.iatic.theisland.model.Player;
import fr.iatic.theisland.model.TerrainTile;
import fr.iatic.theisland.model.TileEffect;
import fr.iatic.theisland.movement.ActionResult;
import fr.iatic.theisland.movement.BoatControlService;
import fr.iatic.theisland.movement.MovementContext;
import fr.iatic.theisland.movement.MovementService;
import fr.iatic.theisland.setup.AutoSetupService;
import fr.iatic.theisland.setup.InitialPlacementService;

import javax.swing.JOptionPane;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Contrôleur principal — Jeu complet.
 */
public final class InteractionController {

    private final PieceState state;
    private final GameState gameState;
    private final InitialPlacementService placementService;
    private final AutoSetupService autoSetupService;
    private final MovementService movementService;
    private final BoatControlService boatControlService;
    private final TileRemovalService tileRemovalService;
    private final CreatureManager creatureManager;

    private DemoPhase phase = DemoPhase.SETUP_EXPLORERS;
    private int activePlayerIndex = 0;
    private MovementContext movementContext;

    private SelectionKind selectionKind = SelectionKind.NONE;
    private Explorer selectedExplorer;
    private Boat selectedBoat;
    private Explorer selectedPassenger;
    private Creature selectedCreature;

    private TerrainTile selectedHandTile;
    private TileEffect pendingHandEffect;

    private CreatureType currentDiceResult = null;
    private String statusMessage = "Préparation : Joueur 1 place son premier explorateur.";

    public InteractionController(PieceState state) {
        this.state = Objects.requireNonNull(state);
        this.gameState = new GameState(state);
        this.placementService = new InitialPlacementService();
        this.autoSetupService = new AutoSetupService(placementService);
        this.movementService = new MovementService();
        this.boatControlService = new BoatControlService();
        this.tileRemovalService = new TileRemovalService();
        this.creatureManager = new CreatureManager();
    }

    // ═══════════════════════════════════════════════════════════════
    // DISPATCH PRINCIPAL
    // ═══════════════════════════════════════════════════════════════

    public void handleCellClick(HexCoordinate coord) {
        Objects.requireNonNull(coord);
        if (gameState.isGameOver()) {
            statusMessage = "La partie est terminée.";
            return;
        }

        switch (phase) {
            case SETUP_EXPLORERS -> placeExplorerAt(coord);
            case SETUP_BOATS -> placeBoatAt(coord);
            case PLAY_HAND_TILE -> handleHandTileClick(coord);
            case MOVEMENT -> handleMovementClick(coord);
            case TILE_REMOVAL -> handleTileRemovalClick(coord);
            case CREATURE_MOVE -> handleCreatureMoveClick(coord);
            default -> statusMessage = "Action indisponible.";
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // SETUP
    // ═══════════════════════════════════════════════════════════════

    public void finishSetupAutomatically() {
        if (phase == DemoPhase.MOVEMENT || phase.ordinal() > DemoPhase.MOVEMENT.ordinal()) {
            statusMessage = "La préparation est déjà terminée.";
            return;
        }
        autoSetupService.placeAllForDemo(state);
        creatureManager.initializeSerpents(gameState);
        startPhaseHandTile();
    }

    private void placeExplorerAt(HexCoordinate coord) {
        Explorer ex = nextUnplacedExplorer();
        if (ex == null) {
            advanceSetup();
            return;
        }

        ActionResult r = placementService.placeExplorer(state, getActivePlayer(), ex, coord);
        if (!r.isSuccess()) {
            statusMessage = r.getMessage();
            return;
        }

        advanceSetup();
        if (phase == DemoPhase.SETUP_EXPLORERS) {
            Explorer nx = nextUnplacedExplorer();
            statusMessage = "Explorateur placé. " + getActivePlayer().getName() + " joue."
                    + (nx != null ? " Valeur : " + nx.getTreasureValue() + "." : "");
        } else {
            statusMessage = "Tous les explorateurs placés. " + getActivePlayer().getName() + " place un bateau.";
        }
    }

    private void placeBoatAt(HexCoordinate coord) {
        Boat boat = nextUnplacedBoat();
        if (boat == null) {
            advanceSetup();
            return;
        }

        ActionResult r = placementService.placeBoat(state, getActivePlayer(), boat, coord);
        if (!r.isSuccess()) {
            statusMessage = r.getMessage();
            return;
        }

        advanceSetup();
        if (phase == DemoPhase.SETUP_BOATS) {
            statusMessage = "Bateau placé. " + getActivePlayer().getName() + " place un bateau.";
        } else {
            creatureManager.initializeSerpents(gameState);
            startPhaseHandTile();
        }
    }

    private void advanceSetup() {
        if (phase == DemoPhase.SETUP_EXPLORERS) {
            if (allExplorersPlaced()) {
                phase = DemoPhase.SETUP_BOATS;
                activePlayerIndex = 0;
                return;
            }
            activePlayerIndex = (activePlayerIndex + 1) % state.getPlayers().size();
        } else if (phase == DemoPhase.SETUP_BOATS) {
            if (allBoatsPlaced()) {
                phase = DemoPhase.PLAY_HAND_TILE;
                activePlayerIndex = 0;
                movementContext = new MovementContext(getActivePlayer());
                clearSelection();
            } else {
                activePlayerIndex = (activePlayerIndex + 1) % state.getPlayers().size();
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // MISSION 3 — Tuiles rouges jouables en main
    // ═══════════════════════════════════════════════════════════════

    public void skipHandTilePhase() {
        if (phase != DemoPhase.PLAY_HAND_TILE) return;
        clearSelection();
        phase = DemoPhase.MOVEMENT;
        movementContext = new MovementContext(getActivePlayer());
        statusMessage = getActivePlayer().getName() + " — 3 déplacements disponibles.";
    }

    public void playHandTile(TerrainTile tile) {
        if (phase != DemoPhase.PLAY_HAND_TILE) {
            statusMessage = "Ce n'est pas le moment de jouer une tuile.";
            return;
        }
        if (tile == null || !gameState.getHandTiles(getActivePlayer()).contains(tile)) {
            statusMessage = "Cette tuile n'est pas dans la main du joueur actif.";
            return;
        }

        TileEffect effect = tile.getHiddenEffect();
        if (effect == TileEffect.CANCEL_SHARK || effect == TileEffect.CANCEL_WHALE) {
            statusMessage = labelEffect(effect)
                    + " est une défense : elle se joue automatiquement quand l'attaque arrive.";
            return;
        }

        clearSelection();
        selectedHandTile = tile;
        pendingHandEffect = effect;

        switch (effect) {
            case DOLPHIN -> {
                selectionKind = SelectionKind.HAND_DOLPHIN_SWIMMER;
                statusMessage = "Dauphin : cliquez sur un de vos nageurs.";
            }
            case WIND -> {
                selectionKind = SelectionKind.HAND_WIND_BOAT;
                statusMessage = "Vent : cliquez sur un bateau que vous contrôlez.";
            }
            case MOVE_SERPENT -> beginCreatureHandTile(CreatureType.SERPENT);
            case MOVE_SHARK -> beginCreatureHandTile(CreatureType.REQUIN);
            case MOVE_WHALE -> beginCreatureHandTile(CreatureType.BALEINE);
            default -> statusMessage = "Cette tuile ne se joue pas depuis la main.";
        }
    }

    private void beginCreatureHandTile(CreatureType type) {
        selectionKind = SelectionKind.HAND_CREATURE;
        statusMessage = labelEffect(pendingHandEffect) + " : cliquez sur une créature "
                + describeCreature(type) + ".";
    }

    private void handleHandTileClick(HexCoordinate coord) {
        if (selectedHandTile == null || pendingHandEffect == null) {
            statusMessage = "Cliquez « Passer » ou choisissez une tuile de votre main.";
            return;
        }

        switch (selectionKind) {
            case HAND_DOLPHIN_SWIMMER -> selectSwimmerForDolphin(coord);
            case HAND_DOLPHIN_TARGET -> resolveDolphinTarget(coord);
            case HAND_WIND_BOAT -> selectBoatForWind(coord);
            case HAND_WIND_TARGET -> resolveWindTarget(coord);
            case HAND_CREATURE -> selectCreatureForHandTile(coord);
            case HAND_CREATURE_TARGET -> resolveCreatureHandTarget(coord);
            default -> statusMessage = "Action de tuile non valide.";
        }
    }

    private void selectSwimmerForDolphin(HexCoordinate coord) {
        Optional<Explorer> swimmer = state.getSwimmersAt(coord).stream()
                .filter(ex -> ex.getOwner() == getActivePlayer())
                .findFirst();
        if (swimmer.isEmpty()) {
            statusMessage = "Cliquez sur un nageur du joueur actif.";
            return;
        }
        selectedExplorer = swimmer.get();
        selectionKind = SelectionKind.HAND_DOLPHIN_TARGET;
        statusMessage = "Nageur sélectionné. Cliquez sur une case de mer à 1, 2 ou 3 cases.";
    }

    private void resolveDolphinTarget(HexCoordinate target) {
        if (selectedExplorer == null) {
            clearSelection();
            statusMessage = "Aucun nageur sélectionné.";
            return;
        }
        if (!isSeaReachable(selectedExplorer.getCoordinate().orElseThrow(), target, 3)) {
            statusMessage = "Le dauphin déplace un nageur de 1 à 3 cases de mer.";
            return;
        }

        selectedExplorer.moveAsSwimmer(target);
        int killed = killSwimmerIfPredatorOn(target, selectedExplorer);
        consumeHandTileAndStartMovement("Tuile Dauphin jouée. Nageur déplacé."
                + (killed > 0 ? " Il est éliminé par un prédateur sur la case." : ""));
    }

    private void selectBoatForWind(HexCoordinate coord) {
        Optional<Boat> boat = allBoatsAt(coord).stream()
                .filter(b -> boatControlService.canPlayerMoveBoat(getActivePlayer(), b))
                .findFirst();

        if (boat.isEmpty()) {
            statusMessage = "Cliquez sur un bateau que vous contrôlez.";
            return;
        }

        selectedBoat = boat.get();
        selectionKind = SelectionKind.HAND_WIND_TARGET;
        statusMessage = "Bateau sélectionné. Cliquez sur une case de mer à 1, 2 ou 3 cases.";
    }

    private void resolveWindTarget(HexCoordinate target) {
        if (selectedBoat == null) {
            clearSelection();
            statusMessage = "Aucun bateau sélectionné.";
            return;
        }
        HexCoordinate source = selectedBoat.getCoordinate().orElse(null);
        if (source == null || !isSeaReachable(source, target, 3)) {
            statusMessage = "Le vent déplace un bateau de 1 à 3 cases de mer.";
            return;
        }
        if (!allBoatsAt(target).isEmpty()) {
            statusMessage = "Cette case de mer contient déjà un bateau.";
            return;
        }

        selectedBoat.moveTo(target);
        consumeHandTileAndStartMovement("Tuile Vent jouée. Bateau déplacé.");
    }

    private void selectCreatureForHandTile(HexCoordinate coord) {
        CreatureType expected = expectedCreatureForPendingTile();
        if (expected == null) {
            statusMessage = "Tuile créature invalide.";
            return;
        }

        Optional<Creature> creature = gameState.getCreaturesAt(coord).stream()
                .filter(c -> c.getType() == expected)
                .findFirst();

        if (creature.isEmpty()) {
            statusMessage = "Cliquez sur une créature " + describeCreature(expected) + ".";
            return;
        }

        selectedCreature = creature.get();
        selectionKind = SelectionKind.HAND_CREATURE_TARGET;
        statusMessage = "Créature sélectionnée. Cliquez sur une case de mer libre.";
    }

    private void resolveCreatureHandTarget(HexCoordinate target) {
        if (selectedCreature == null) {
            clearSelection();
            statusMessage = "Aucune créature sélectionnée.";
            return;
        }
        if (!isEmptySeaForCreature(target)) {
            statusMessage = "La destination doit être une case de mer libre.";
            return;
        }

        selectedCreature.setPosition(target);
        consumeHandTileAndStartMovement("Tuile " + labelEffect(pendingHandEffect)
                + " jouée. Créature déplacée.");
    }

    private CreatureType expectedCreatureForPendingTile() {
        if (pendingHandEffect == TileEffect.MOVE_SERPENT) return CreatureType.SERPENT;
        if (pendingHandEffect == TileEffect.MOVE_SHARK) return CreatureType.REQUIN;
        if (pendingHandEffect == TileEffect.MOVE_WHALE) return CreatureType.BALEINE;
        return null;
    }

    private void consumeHandTileAndStartMovement(String message) {
        gameState.removeHandTile(getActivePlayer(), selectedHandTile);
        clearSelection();
        phase = DemoPhase.MOVEMENT;
        movementContext = new MovementContext(getActivePlayer());
        statusMessage = message + " Phase déplacement : 3 déplacements disponibles.";
    }

    // ═══════════════════════════════════════════════════════════════
    // MISSION 2 — Mouvement
    // ═══════════════════════════════════════════════════════════════

    private void handleMovementClick(HexCoordinate coord) {
        if (selectionKind == SelectionKind.NONE) {
            trySelectAt(coord);
            return;
        }

        switch (selectionKind) {
            case EXPLORER -> handleExplorerDestination(coord);
            case BOAT -> handleBoatDestination(coord);
            case PASSENGER -> handlePassengerDestination(coord);
            default -> statusMessage = "Sélection invalide.";
        }
    }

    private void trySelectAt(HexCoordinate coord) {
        Optional<Explorer> ex = findSelectableExplorerAt(coord);
        if (ex.isPresent()) {
            selectedExplorer = ex.get();
            selectedBoat = null;
            selectedPassenger = null;
            selectionKind = SelectionKind.EXPLORER;
            statusMessage = "Explorateur sélectionné : cliquez sur une destination valide.";
            return;
        }

        List<Boat> boats = allBoatsAt(coord);
        if (!boats.isEmpty()) {
            chooseBoatAction(boats.get(0));
            return;
        }

        statusMessage = "Aucun pion sélectionnable ici pour " + getActivePlayer().getName() + ".";
    }

    private Optional<Explorer> findSelectableExplorerAt(HexCoordinate coord) {
        List<Explorer> land = state.getLandExplorersAt(coord).stream()
                .filter(e -> e.getOwner() == getActivePlayer()).toList();
        if (!land.isEmpty()) return Optional.of(land.get(0));

        List<Explorer> sw = state.getSwimmersAt(coord).stream()
                .filter(e -> e.getOwner() == getActivePlayer()).toList();
        if (!sw.isEmpty()) return Optional.of(sw.get(0));

        return Optional.empty();
    }

    private void chooseBoatAction(Boat boat) {
        List<String> opts = new ArrayList<>();
        if (boatControlService.canPlayerMoveBoat(getActivePlayer(), boat)) {
            opts.add("Déplacer le bateau");
        }

        List<Explorer> mine = boat.getPassengers().stream()
                .filter(p -> p.getOwner() == getActivePlayer()).toList();
        mine.forEach(p -> opts.add("Déplacer le passager " + p.getId()));

        if (opts.isEmpty()) {
            statusMessage = "Ce bateau n'est pas sous votre contrôle.";
            return;
        }

        Object choice = JOptionPane.showInputDialog(
                null, "Que voulez-vous faire ?",
                "Bateau sélectionné", JOptionPane.QUESTION_MESSAGE,
                null, opts.toArray(), opts.get(0));

        if (choice == null) {
            statusMessage = "Sélection annulée.";
            return;
        }

        String sel = choice.toString();
        if (sel.equals("Déplacer le bateau")) {
            selectedBoat = boat;
            selectedExplorer = null;
            selectedPassenger = null;
            selectionKind = SelectionKind.BOAT;
            statusMessage = "Bateau sélectionné : cliquez sur une case de mer adjacente.";
            return;
        }

        mine.stream().filter(p -> sel.endsWith(p.getId())).findFirst().ifPresent(p -> {
            selectedPassenger = p;
            selectedBoat = boat;
            selectedExplorer = null;
            selectionKind = SelectionKind.PASSENGER;
            statusMessage = "Passager sélectionné : cliquez un bateau ou abri adjacent, ou Sauter.";
        });
    }

    private void handleExplorerDestination(HexCoordinate target) {
        if (selectedExplorer == null) {
            clearSelection();
            return;
        }

        ActionResult r;
        if (selectedExplorer.getStatus() == ExplorerStatus.ON_LAND) {
            r = tryMoveLand(target);
        } else if (selectedExplorer.getStatus() == ExplorerStatus.SWIMMER) {
            r = tryMoveSwimmer(target);
        } else {
            r = ActionResult.failure("Cet explorateur ne peut pas être déplacé.");
        }
        applyMovementResult(r);
    }

    private ActionResult tryMoveLand(HexCoordinate target) {
        if (state.getBoard().getCell(target).hasTerrainTile()) {
            return movementService.moveLandExplorerToTerrain(state, movementContext, selectedExplorer, target);
        }

        List<Boat> boats = allBoatsAt(target);
        if (!boats.isEmpty()) {
            return movementService.boardBoatFromLand(state, movementContext, selectedExplorer, boats.get(0));
        }

        if (state.getBoard().getCell(target).isSea()) {
            return movementService.enterWaterFromLand(state, movementContext, selectedExplorer, target);
        }

        return ActionResult.failure("Destination non valide.");
    }

    private ActionResult tryMoveSwimmer(HexCoordinate target) {
        HexCoordinate pos = selectedExplorer.getCoordinate().orElseThrow();

        if (pos.equals(target)) {
            List<Boat> boats = allBoatsAt(target);
            if (!boats.isEmpty()) {
                return movementService.boardBoatFromWater(state, movementContext, selectedExplorer, boats.get(0));
            }
        }

        if (state.getBoard().getCell(target).isSea()) {
            return movementService.moveSwimmer(state, movementContext, selectedExplorer, target);
        }

        return ActionResult.failure("Destination non valide pour ce nageur.");
    }

    private void handleBoatDestination(HexCoordinate target) {
        if (selectedBoat == null) {
            clearSelection();
            return;
        }
        applyMovementResult(movementService.moveBoat(state, movementContext, selectedBoat, target));
    }

    private void handlePassengerDestination(HexCoordinate target) {
        if (selectedPassenger == null) {
            clearSelection();
            return;
        }

        ActionResult r;
        if (state.getBoard().getCell(target).isRescueIsland()) {
            r = movementService.disembarkExplorerToRescueIsland(state, movementContext, selectedPassenger, target);
        } else if (!allBoatsAt(target).isEmpty()) {
            r = movementService.transferExplorerBetweenAdjacentBoats(
                    state, movementContext, selectedPassenger, allBoatsAt(target).get(0));
        } else {
            r = ActionResult.failure("Cliquez sur un abri ou bateau adjacent.");
        }

        applyMovementResult(r);
    }

    public void jumpSelectedPassengerIntoWater() {
        if (phase != DemoPhase.MOVEMENT
                || selectionKind != SelectionKind.PASSENGER
                || selectedPassenger == null) {
            statusMessage = "Aucun passager sélectionné.";
            return;
        }
        applyMovementResult(movementService.jumpFromBoatToSea(state, movementContext, selectedPassenger));
    }

    private void applyMovementResult(ActionResult r) {
        if (r.isSuccess()) {
            int killedByPredators = removeSwimmersOnPredators();
            clearSelection();

            String predatorMessage = killedByPredators > 0
                    ? " " + killedByPredators + " nageur(s) éliminé(s) par un serpent ou un requin."
                    : "";

            statusMessage = r.getMessage() + predatorMessage + " Déplacements restants : "
                    + movementContext.getRemainingMovementPoints() + ".";
        } else {
            statusMessage = r.getMessage();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // MISSION 3 — Retrait de tuile
    // ═══════════════════════════════════════════════════════════════

    private void handleTileRemovalClick(HexCoordinate coord) {
        TileRemovalService.TileRemovalResult r =
                tileRemovalService.removeTile(gameState, getActivePlayer(), coord);
        statusMessage = r.getMessage();

        if (!r.isSuccess()) return;

        if (gameState.isGameOver()) {
            phase = DemoPhase.GAME_OVER;
            return;
        }

        phase = DemoPhase.CREATURE_DICE;
        statusMessage = r.getMessage() + " Maintenant, lancez le dé de créature.";
    }

    // ═══════════════════════════════════════════════════════════════
    // MISSION 4 — Dé et créature
    // ═══════════════════════════════════════════════════════════════

    public void rollCreatureDice() {
        if (phase != DemoPhase.CREATURE_DICE) {
            statusMessage = "Ce n'est pas le moment de lancer le dé.";
            return;
        }

        currentDiceResult = creatureManager.rollDice();
        List<Creature> available = gameState.getCreaturesOfType(currentDiceResult);

        if (available.isEmpty()) {
            statusMessage = "Dé : " + describeCreature(currentDiceResult)
                    + " — Aucune créature de ce type sur le plateau.";
            endTurn();
            return;
        }

        phase = DemoPhase.CREATURE_MOVE;
        statusMessage = "Dé : " + describeCreature(currentDiceResult)
                + " — Cliquez sur une créature puis sa destination.";
    }

    private void handleCreatureMoveClick(HexCoordinate coord) {
        if (selectedCreature == null) {
            List<Creature> here = gameState.getCreaturesAt(coord).stream()
                    .filter(c -> c.getType() == currentDiceResult).toList();
            if (here.isEmpty()) {
                statusMessage = "Aucune créature " + describeCreature(currentDiceResult) + " ici.";
                return;
            }

            selectedCreature = here.get(0);
            statusMessage = describeCreature(currentDiceResult)
                    + " sélectionné(e). Cliquez sur sa destination.";
            return;
        }

        ActionResult r = creatureManager.moveCreature(gameState, selectedCreature, coord);
        if (!r.isSuccess()) {
            statusMessage = r.getMessage();
            return;
        }

        selectedCreature = null;
        statusMessage = r.getMessage();

        if (gameState.isGameOver()) {
            phase = DemoPhase.GAME_OVER;
            return;
        }

        endTurn();
    }

    public void skipCreatureMove() {
        if (phase != DemoPhase.CREATURE_MOVE && phase != DemoPhase.CREATURE_DICE) return;
        selectedCreature = null;
        statusMessage = "Déplacement de créature passé.";
        endTurn();
    }

    private void endTurn() {
        currentDiceResult = null;
        activePlayerIndex = (activePlayerIndex + 1) % state.getPlayers().size();
        movementContext = new MovementContext(getActivePlayer());
        clearSelection();
        startPhaseHandTile();
    }

    private void startPhaseHandTile() {
        phase = DemoPhase.PLAY_HAND_TILE;
        statusMessage = "Tour de " + getActivePlayer().getName()
                + " — Phase 1 : jouer une tuile (ou Passer).";
    }

    // ═══════════════════════════════════════════════════════════════
    // Bouton "fin de déplacements" : MOVEMENT → TILE_REMOVAL
    // ═══════════════════════════════════════════════════════════════

    public void nextPlayer() {
        if (phase == DemoPhase.SETUP_EXPLORERS || phase == DemoPhase.SETUP_BOATS) {
            statusMessage = "Passage au joueur suivant disponible après la préparation.";
            return;
        }

        if (phase == DemoPhase.MOVEMENT) {
            phase = DemoPhase.TILE_REMOVAL;
            clearSelection();
            statusMessage = getActivePlayer().getName()
                    + " — Phase 3 : cliquez une tuile surlignée pour la retirer.";
            return;
        }

        statusMessage = "Utilisez les boutons de phase.";
    }

    public void cancelSelection() {
        clearSelection();
        statusMessage = "Sélection annulée.";
    }

    // ═══════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════

    private List<Boat> allBoatsAt(HexCoordinate coord) {
        Map<String, Boat> unique = new LinkedHashMap<>();
        for (Boat boat : state.getBoatsAt(coord)) unique.put(boat.getId(), boat);
        for (Boat boat : gameState.getExtraBoatsAt(coord)) unique.put(boat.getId(), boat);
        return new ArrayList<>(unique.values());
    }

    private void clearSelection() {
        selectionKind = SelectionKind.NONE;
        selectedExplorer = null;
        selectedBoat = null;
        selectedPassenger = null;
        selectedCreature = null;
        selectedHandTile = null;
        pendingHandEffect = null;
    }

    private Explorer nextUnplacedExplorer() {
        return getActivePlayer().getExplorers().stream()
                .filter(e -> e.getStatus() == ExplorerStatus.UNPLACED).findFirst().orElse(null);
    }

    private Boat nextUnplacedBoat() {
        return getActivePlayer().getSetupBoats().stream()
                .filter(b -> b.getStatus() == BoatStatus.UNPLACED).findFirst().orElse(null);
    }

    private boolean allExplorersPlaced() {
        return state.getAllExplorers().stream()
                .noneMatch(e -> e.getStatus() == ExplorerStatus.UNPLACED);
    }

    private boolean allBoatsPlaced() {
        return state.getPlayers().stream().flatMap(p -> p.getSetupBoats().stream())
                .noneMatch(b -> b.getStatus() == BoatStatus.UNPLACED);
    }

    private boolean isSeaReachable(HexCoordinate source, HexCoordinate target, int maxDistance) {
        Board board = state.getBoard();
        if (!board.contains(target) || !board.getCell(target).isSea()) return false;
        if (source.equals(target)) return false;

        ArrayDeque<HexCoordinate> queue = new ArrayDeque<>();
        Map<HexCoordinate, Integer> distance = new LinkedHashMap<>();
        queue.add(source);
        distance.put(source, 0);

        while (!queue.isEmpty()) {
            HexCoordinate current = queue.removeFirst();
            int currentDistance = distance.get(current);

            if (currentDistance >= maxDistance) continue;

            for (HexCoordinate neighbor : board.getNeighbors(current)) {
                HexCell cell = board.getCell(neighbor);
                if (!cell.isSea()) continue;
                if (distance.containsKey(neighbor)) continue;

                int nextDistance = currentDistance + 1;
                if (neighbor.equals(target)) return nextDistance <= maxDistance;
                distance.put(neighbor, nextDistance);
                queue.addLast(neighbor);
            }
        }
        return false;
    }

    private boolean isEmptySeaForCreature(HexCoordinate coord) {
        if (!state.getBoard().contains(coord)) return false;
        if (!state.getBoard().getCell(coord).isSea()) return false;
        return allBoatsAt(coord).isEmpty()
                && state.getSwimmersAt(coord).isEmpty()
                && gameState.getCreaturesAt(coord).isEmpty();
    }

    private int removeSwimmersOnPredators() {
        int killed = 0;
        for (Explorer swimmer : new ArrayList<>(state.getAllExplorers())) {
            if (swimmer.getStatus() != ExplorerStatus.SWIMMER) {
                continue;
            }

            HexCoordinate coordinate = swimmer.getCoordinate().orElse(null);
            if (coordinate == null) {
                continue;
            }

            boolean predatorHere = gameState.getCreaturesAt(coordinate).stream()
                    .anyMatch(creature -> creature.getType() == CreatureType.REQUIN
                            || creature.getType() == CreatureType.SERPENT);

            if (predatorHere) {
                swimmer.removeFromGame();
                killed++;
            }
        }
        return killed;
    }

    private int killSwimmerIfPredatorOn(HexCoordinate coord, Explorer explorer) {
        boolean danger = gameState.getCreaturesAt(coord).stream()
                .anyMatch(c -> c.getType() == CreatureType.REQUIN || c.getType() == CreatureType.SERPENT);
        if (danger && explorer.getStatus() == ExplorerStatus.SWIMMER) {
            explorer.removeFromGame();
            return 1;
        }
        return 0;
    }

    public String describeCreature(CreatureType t) {
        return switch (t) {
            case SERPENT -> "Serpent de mer";
            case REQUIN -> "Requin";
            case BALEINE -> "Baleine";
        };
    }

    public String labelEffect(TileEffect e) {
        return switch (e) {
            case DOLPHIN -> "Dauphin";
            case WIND -> "Vent";
            case MOVE_SERPENT -> "Déplacer serpent";
            case MOVE_SHARK -> "Déplacer requin";
            case MOVE_WHALE -> "Déplacer baleine";
            case CANCEL_SHARK -> "Défense requin";
            case CANCEL_WHALE -> "Défense baleine";
            case SHARK_APPEARS -> "Apparition requin";
            case WHALE_APPEARS -> "Apparition baleine";
            case BOAT_APPEARS -> "Apparition bateau";
            case WHIRLPOOL -> "Tourbillon";
            case VOLCANO -> "Volcan";
            default -> e.name();
        };
    }

    public String labelHandTile(TerrainTile tile) {
        return labelEffect(tile.getHiddenEffect());
    }

    // ═══════════════════════════════════════════════════════════════
    // Getters
    // ═══════════════════════════════════════════════════════════════

    public PieceState getState() { return state; }
    public GameState getGameState() { return gameState; }
    public DemoPhase getPhase() { return phase; }
    public Player getActivePlayer() { return state.getPlayers().get(activePlayerIndex); }
    public MovementContext getMovementContext() { return movementContext; }
    public SelectionKind getSelectionKind() { return selectionKind; }
    public String getStatusMessage() { return statusMessage; }

    public Optional<Explorer> getSelectedExplorer() { return Optional.ofNullable(selectedExplorer); }
    public Optional<Boat> getSelectedBoat() { return Optional.ofNullable(selectedBoat); }
    public Optional<Explorer> getSelectedPassenger() { return Optional.ofNullable(selectedPassenger); }
    public Optional<Creature> getSelectedCreature() { return Optional.ofNullable(selectedCreature); }
    public Optional<CreatureType> getCurrentDiceResult() { return Optional.ofNullable(currentDiceResult); }

    public Explorer getNextUnplacedExplorerPreview() {
        return phase == DemoPhase.SETUP_EXPLORERS ? nextUnplacedExplorer() : null;
    }

    public Boat getNextUnplacedBoatPreview() {
        return phase == DemoPhase.SETUP_BOATS ? nextUnplacedBoat() : null;
    }
}
