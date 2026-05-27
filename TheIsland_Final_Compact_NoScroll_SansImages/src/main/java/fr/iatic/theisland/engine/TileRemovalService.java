package fr.iatic.theisland.engine;

import fr.iatic.theisland.model.Boat;
import fr.iatic.theisland.model.Board;
import fr.iatic.theisland.model.Creature;
import fr.iatic.theisland.model.CreatureType;
import fr.iatic.theisland.model.Explorer;
import fr.iatic.theisland.model.GameState;
import fr.iatic.theisland.model.HexCell;
import fr.iatic.theisland.model.HexCoordinate;
import fr.iatic.theisland.model.PieceState;
import fr.iatic.theisland.model.Player;
import fr.iatic.theisland.model.TerrainTile;
import fr.iatic.theisland.model.TerrainType;
import fr.iatic.theisland.model.TileEffect;
import fr.iatic.theisland.movement.ActionResult;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Mission 3 — Retrait des tuiles de terrain.
 *
 * Règles respectées :
 * 1. Ordre : plage → forêt → montagne.
 * 2. La tuile doit être adjacente à une case de mer.
 * 3. Les explorateurs dessus tombent à la mer (nageurs).
 * 4. Effets immédiats (vert) appliqués sur-le-champ.
 * 5. Effets différés (rouge) gardés en main.
 */
public final class TileRemovalService {

    /** Vérifie si le retrait est possible (ne modifie rien). */
    public ActionResult canRemoveTile(GameState gs, HexCoordinate coord) {
        Objects.requireNonNull(gs,    "L'état du jeu est obligatoire.");
        Objects.requireNonNull(coord, "La coordonnée est obligatoire.");
        Board board = gs.getPieceState().getBoard();
        if (!board.contains(coord))
            return ActionResult.failure("Case hors du plateau.");
        HexCell cell = board.getCell(coord);
        if (!cell.hasTerrainTile())
            return ActionResult.failure("Aucune tuile de terrain ici.");
        TerrainType type = cell.getTerrainTile().orElseThrow().getType();
        if (type == TerrainType.FOREST
                && board.countPlacedTerrainTilesOfType(TerrainType.BEACH) > 0)
            return ActionResult.failure("Retirez d'abord toutes les tuiles Plage.");
        if (type == TerrainType.MOUNTAIN
                && (board.countPlacedTerrainTilesOfType(TerrainType.BEACH) > 0
                ||  board.countPlacedTerrainTilesOfType(TerrainType.FOREST) > 0))
            return ActionResult.failure("Retirez d'abord toutes les tuiles Plage et Forêt.");
        boolean adjSea = board.getNeighbors(coord).stream()
                .map(board::getCell).anyMatch(HexCell::isSea);
        if (!adjSea)
            return ActionResult.failure("Cette tuile n'est pas adjacente à la mer.");
        return ActionResult.success("Retrait possible.");
    }

    /** Retire la tuile et applique ses effets. Retourne un résultat descriptif. */
    public TileRemovalResult removeTile(GameState gs, Player activePlayer, HexCoordinate coord) {
        Objects.requireNonNull(gs,           "L'état du jeu est obligatoire.");
        Objects.requireNonNull(activePlayer, "Le joueur actif est obligatoire.");
        Objects.requireNonNull(coord,        "La coordonnée est obligatoire.");

        ActionResult check = canRemoveTile(gs, coord);
        if (!check.isSuccess()) return TileRemovalResult.failure(check.getMessage());

        HexCell cell  = gs.getPieceState().getBoard().getCell(coord);
        TerrainTile tile = cell.removeTerrainTile();
        TileEffect effect = tile.getHiddenEffect();

        // Explorateurs qui tombent
        List<Explorer> fallen = new ArrayList<>(gs.getPieceState().getLandExplorersAt(coord));
        for (Explorer e : fallen) e.becomeSwimmer(coord);

        // Appliquer l'effet
        String effectMsg = "";
        if (effect.isImmediate()) {
            effectMsg = applyImmediate(gs, activePlayer, coord, effect);
        } else if (effect.isHandEffect()) {
            gs.addHandTile(activePlayer, tile);
            effectMsg = "Tuile gardée en main : " + labelEffect(effect) + ".";
        }

        String fallenMsg = fallen.isEmpty() ? "" : fallen.size() + " explorateur(s) tombé(s) à la mer. ";
        return TileRemovalResult.success(
                "Tuile " + tile.getType().getLabel() + " retirée. " + fallenMsg + effectMsg,
                tile, effect);
    }

    // ── Effets immédiats ──────────────────────────────────────────────────────

    private String applyImmediate(GameState gs, Player activePlayer,
                                   HexCoordinate coord, TileEffect effect) {
        return switch (effect) {
            case SHARK_APPEARS -> applyShark(gs, coord);
            case WHALE_APPEARS -> applyWhale(gs, coord);
            case BOAT_APPEARS  -> applyBoat(gs, activePlayer, coord);
            case WHIRLPOOL     -> applyWhirlpool(gs, coord);
            case VOLCANO       -> { gs.triggerGameOver(); yield "🌋 VOLCAN ! Fin de la partie !"; }
            default            -> "";
        };
    }

    private String applyShark(GameState gs, HexCoordinate coord) {
        if (!gs.takeSharkFromStock()) return "Pas de requin en stock.";
        Creature shark = new Creature(CreatureType.REQUIN, coord);
        gs.addCreature(shark);

        List<Explorer> swimmers = new ArrayList<>(gs.getPieceState().getSwimmersAt(coord));
        if (!swimmers.isEmpty() && consumeDefenseForSwimmers(gs, swimmers, TileEffect.CANCEL_SHARK)) {
            gs.removeCreature(shark);
            return "🦈 Un requin surgit, mais une défense Requin est jouée : le requin est retiré.";
        }

        int killed = killSwimmersAt(gs.getPieceState(), coord);
        return "🦈 Un requin surgit !" + (killed > 0 ? " " + killed + " nageur(s) dévoré(s)." : "");
    }

    private String applyWhale(GameState gs, HexCoordinate coord) {
        if (!gs.takeWhaleFromStock()) return "Pas de baleine en stock.";
        gs.addCreature(new Creature(CreatureType.BALEINE, coord));
        return "🐋 Une baleine apparaît !";
    }

    private String applyBoat(GameState gs, Player activePlayer, HexCoordinate coord) {
        if (!gs.takeBoatFromStock()) return "Pas de bateau en stock.";
        Boat boat = new Boat("BOAT-T-" + System.nanoTime(), activePlayer);
        boat.placeAt(coord);
        gs.registerExtraBoat(boat);
        List<Explorer> swimmers = new ArrayList<>(gs.getPieceState().getSwimmersAt(coord));
        int boarded = 0;
        for (Explorer s : swimmers) {
            if (boarded >= Boat.MAX_PASSENGERS) break;
            boat.addPassenger(s);
            s.boardBoat(boat.getId());
            boarded++;
        }
        return "⛵ Un bateau apparaît !" + (boarded > 0 ? " " + boarded + " nageur(s) embarqué(s)." : "");
    }

    private String applyWhirlpool(GameState gs, HexCoordinate coord) {
        Board board = gs.getPieceState().getBoard();
        List<HexCoordinate> zone = new ArrayList<>();
        zone.add(coord);
        board.getNeighbors(coord).stream()
                .filter(n -> board.getCell(n).isSea()).forEach(zone::add);
        int total = 0;
        for (HexCoordinate c : zone) {
            total += killSwimmersAt(gs.getPieceState(), c);
            total += capsizeBoats(gs, c);
            for (Creature cr : new ArrayList<>(gs.getCreaturesAt(c))) gs.removeCreature(cr);
        }
        return "🌀 Tourbillon ! " + total + " pion(s) englouti(s).";
    }

    private int killSwimmersAt(PieceState ps, HexCoordinate coord) {
        List<Explorer> sw = new ArrayList<>(ps.getSwimmersAt(coord));
        sw.forEach(Explorer::removeFromGame);
        return sw.size();
    }

    private int capsizeBoats(GameState gs, HexCoordinate coord) {
        int count = 0;
        Map<String, Boat> unique = new LinkedHashMap<>();
        for (Boat boat : gs.getPieceState().getBoatsAt(coord)) {
            unique.put(boat.getId(), boat);
        }
        for (Boat boat : gs.getExtraBoatsAt(coord)) {
            unique.put(boat.getId(), boat);
        }

        for (Boat b : unique.values()) {
            if (!b.isEmpty()) {
                for (Explorer p : new ArrayList<>(b.getPassengers())) {
                    b.removePassenger(p);
                    p.removeFromGame();
                    count++;
                }
                b.removeFromGame();
            }
        }
        return count;
    }

    private boolean consumeDefenseForSwimmers(GameState gs, List<Explorer> swimmers, TileEffect defense) {
        for (Explorer swimmer : swimmers) {
            if (gs.removeFirstHandTile(swimmer.getOwner(), defense)) {
                return true;
            }
        }
        return false;
    }

    private String labelEffect(TileEffect e) {
        return switch (e) {
            case DOLPHIN      -> "Dauphin (nageur +1-3 cases)";
            case WIND         -> "Vent (bateau +1-3 cases)";
            case MOVE_SERPENT -> "Déplacer un serpent";
            case MOVE_SHARK   -> "Déplacer un requin";
            case MOVE_WHALE   -> "Déplacer une baleine";
            case CANCEL_SHARK -> "Annuler requin (défense)";
            case CANCEL_WHALE -> "Annuler baleine (défense)";
            default           -> e.name();
        };
    }

    // ── Résultat ──────────────────────────────────────────────────────────────
    public static final class TileRemovalResult {
        private final boolean    success;
        private final String     message;
        private final TerrainTile removedTile;
        private final TileEffect  revealedEffect;

        private TileRemovalResult(boolean s, String m, TerrainTile t, TileEffect e) {
            success = s; message = m; removedTile = t; revealedEffect = e;
        }
        public static TileRemovalResult success(String m, TerrainTile t, TileEffect e) {
            return new TileRemovalResult(true, m, t, e);
        }
        public static TileRemovalResult failure(String m) {
            return new TileRemovalResult(false, m, null, null);
        }
        public boolean   isSuccess()       { return success;  }
        public String    getMessage()      { return message;  }
        public Optional<TerrainTile> getRemovedTile()   { return Optional.ofNullable(removedTile);   }
        public Optional<TileEffect>  getRevealedEffect(){ return Optional.ofNullable(revealedEffect); }
    }
}
