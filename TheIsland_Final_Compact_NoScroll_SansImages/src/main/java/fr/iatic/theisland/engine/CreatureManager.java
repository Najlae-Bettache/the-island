package fr.iatic.theisland.engine;

import fr.iatic.theisland.model.Boat;
import fr.iatic.theisland.model.Board;
import fr.iatic.theisland.model.Creature;
import fr.iatic.theisland.model.CreatureType;
import fr.iatic.theisland.model.Dice;
import fr.iatic.theisland.model.Explorer;
import fr.iatic.theisland.model.GameState;
import fr.iatic.theisland.model.HexCell;
import fr.iatic.theisland.model.HexCoordinate;
import fr.iatic.theisland.model.PieceState;
import fr.iatic.theisland.model.Player;
import fr.iatic.theisland.model.TileEffect;
import fr.iatic.theisland.movement.ActionResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;


public final class CreatureManager {

    
    public void initializeSerpents(GameState gs) {
        Objects.requireNonNull(gs);
        for (HexCoordinate spawn : gs.getPieceState().getBoard().getSeaSerpentSpawnCoordinates()) {
            gs.addCreature(new Creature(CreatureType.SERPENT, spawn));
        }
    }

   
    public CreatureType rollDice() {
        return Dice.roll();
    }

    
    public ActionResult moveCreature(GameState gs, Creature creature, HexCoordinate target) {
        Objects.requireNonNull(gs,       "L'état du jeu est obligatoire.");
        Objects.requireNonNull(creature, "La créature est obligatoire.");
        Objects.requireNonNull(target,   "La case cible est obligatoire.");

        Board board = gs.getPieceState().getBoard();
        if (!board.contains(target))
            return ActionResult.failure("Case hors du plateau.");
        if (!board.getCell(target).isSea())
            return ActionResult.failure("Les créatures se déplacent uniquement sur la mer.");

        int dist = bfsDistance(board, creature.getPosition(), target);
        if (dist < 1 || dist > creature.getMaxMoves())
            return ActionResult.failure(
                    "Distance invalide (max " + creature.getMaxMoves() + " case(s)).");

        creature.setPosition(target);

        return switch (creature.getType()) {
            case SERPENT -> applySerpent(gs, target);
            case REQUIN  -> applyShark(gs, target);
            case BALEINE -> applyWhale(gs, creature, target);
        };
    }

    
    private ActionResult applySerpent(GameState gs, HexCoordinate target) {
        PieceState ps = gs.getPieceState();
        int killed = 0;
        for (Explorer e : new ArrayList<>(ps.getSwimmersAt(target))) {
            e.removeFromGame(); killed++;
        }
        for (Boat b : allBoatsAt(gs, target)) {
            if (!b.isEmpty()) {
                for (Explorer p : new ArrayList<>(b.getPassengers())) {
                    b.removePassenger(p); p.removeFromGame(); killed++;
                }
                b.removeFromGame();
            }
        }
        return killed == 0
                ? ActionResult.success("Le serpent se déplace. Aucun pion touché.")
                : ActionResult.success("🐍 Le serpent de mer frappe ! " + killed + " pion(s) éliminé(s).");
    }

    
    private ActionResult applyShark(GameState gs, HexCoordinate target) {
        List<Explorer> swimmers = new ArrayList<>(gs.getPieceState().getSwimmersAt(target));
        if (swimmers.isEmpty())
            return ActionResult.success("Le requin se déplace. Aucun nageur touché.");

        if (consumeDefenseForSwimmers(gs, swimmers, TileEffect.CANCEL_SHARK)) {
            Creature shark = gs.getCreaturesAt(target).stream()
                    .filter(c -> c.getType() == CreatureType.REQUIN)
                    .findFirst()
                    .orElse(null);
            if (shark != null) gs.removeCreature(shark);
            return ActionResult.success("🛡 Défense Requin jouée : le requin est retiré, les nageurs restent.");
        }

        swimmers.forEach(Explorer::removeFromGame);
        return ActionResult.success("🦈 Le requin attaque ! " + swimmers.size() + " nageur(s) dévoré(s).");
    }

    
    private ActionResult applyWhale(GameState gs, Creature whale, HexCoordinate target) {
        PieceState ps = gs.getPieceState();
        Boat victim = allBoatsAt(gs, target).stream().filter(b -> !b.isEmpty()).findFirst().orElse(null);
        if (victim == null)
            return ActionResult.success("La baleine se déplace. Aucun bateau occupé touché.");

        List<Explorer> passengers = new ArrayList<>(victim.getPassengers());
        if (consumeDefenseForPassengers(gs, passengers, TileEffect.CANCEL_WHALE)) {
            gs.removeCreature(whale);
            return ActionResult.success("🛡 Défense Baleine jouée : la baleine est retirée, le bateau reste.");
        }

        for (Explorer p : passengers) { victim.removePassenger(p); p.becomeSwimmer(target); }
        victim.removeFromGame();

        boolean sharkHere = gs.getCreaturesAt(target).stream()
                .anyMatch(c -> c.getType() == CreatureType.REQUIN);
        if (sharkHere) {
            passengers.forEach(Explorer::removeFromGame);
            return ActionResult.success(
                    "🐋 La baleine chavire le bateau ! Les nageurs sont aussitôt dévorés par le requin. "
                    + passengers.size() + " pion(s) éliminé(s).");
        }
        return ActionResult.success(
                "🐋 La baleine chavire le bateau ! " + passengers.size() + " explorateur(s) → nageur(s).");
    }

    private boolean consumeDefenseForSwimmers(GameState gs, List<Explorer> swimmers, TileEffect defense) {
        for (Explorer swimmer : swimmers) {
            if (gs.removeFirstHandTile(swimmer.getOwner(), defense)) {
                return true;
            }
        }
        return false;
    }

    private boolean consumeDefenseForPassengers(GameState gs, List<Explorer> passengers, TileEffect defense) {
        for (Explorer passenger : passengers) {
            if (gs.removeFirstHandTile(passenger.getOwner(), defense)) {
                return true;
            }
        }
        return false;
    }



    private List<Boat> allBoatsAt(GameState gs, HexCoordinate coord) {
        Map<String, Boat> unique = new LinkedHashMap<>();
        for (Boat boat : gs.getPieceState().getBoatsAt(coord)) {
            unique.put(boat.getId(), boat);
        }
        for (Boat boat : gs.getExtraBoatsAt(coord)) {
            unique.put(boat.getId(), boat);
        }
        return new ArrayList<>(unique.values());
    }

    
    private int bfsDistance(Board board, HexCoordinate from, HexCoordinate to) {
        if (from.equals(to)) return 0;
        Set<HexCoordinate> visited = new HashSet<>();
        Queue<HexCoordinate> queue = new LinkedList<>();
        visited.add(from); queue.add(from);
        int depth = 0;
        while (!queue.isEmpty()) {
            depth++;
            int size = queue.size();
            for (int i = 0; i < size; i++) {
                HexCoordinate current = queue.poll();
                for (HexCoordinate n : board.getNeighbors(current)) {
                    if (!board.getCell(n).isSea()) continue;
                    if (n.equals(to)) return depth;
                    if (visited.add(n)) queue.add(n);
                }
            }
        }
        return Integer.MAX_VALUE;
    }
}
