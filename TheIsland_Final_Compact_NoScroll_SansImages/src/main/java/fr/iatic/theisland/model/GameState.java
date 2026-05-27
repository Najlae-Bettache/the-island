package fr.iatic.theisland.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * État global de la partie pour les missions 3 et 4 :
 * créatures, tuiles en main, bateaux bonus et fin de jeu.
 */
public final class GameState {

    private final PieceState pieceState;
    private final List<Creature> creatures = new ArrayList<>();
    private final Map<String, List<TerrainTile>> handTiles = new LinkedHashMap<>();
    private final Map<String, Boat> extraBoats = new LinkedHashMap<>();

    private int availableSharks = 6;
    private int availableWhales = 5;
    private int availableBoats  = 4; // bateaux supplémentaires (12 - 8 distribués)

    private boolean gameOver = false;
    private Player  winner   = null;

    public GameState(PieceState pieceState) {
        this.pieceState = Objects.requireNonNull(pieceState, "L'état des pions est obligatoire.");
        for (Player p : pieceState.getPlayers()) {
            handTiles.put(p.getId(), new ArrayList<>());
        }
    }

    // ── Accesseur ─────────────────────────────────────────────────────────────
    public PieceState getPieceState() { return pieceState; }

    // ── Créatures ─────────────────────────────────────────────────────────────
    public void addCreature(Creature c) {
        creatures.add(Objects.requireNonNull(c));
    }

    public List<Creature> getCreatures() {
        return Collections.unmodifiableList(creatures);
    }

    public List<Creature> getCreaturesAt(HexCoordinate coord) {
        return creatures.stream().filter(c -> c.getPosition().equals(coord)).toList();
    }

    public List<Creature> getCreaturesOfType(CreatureType type) {
        return creatures.stream().filter(c -> c.getType() == type).toList();
    }

    public void removeCreature(Creature c) {
        creatures.remove(c);
        switch (c.getType()) {
            case REQUIN  -> availableSharks++;
            case BALEINE -> availableWhales++;
            default -> { }
        }
    }

    public int getAvailableSharks() { return availableSharks; }
    public int getAvailableWhales() { return availableWhales; }
    public int getAvailableBoats()  { return availableBoats;  }

    public boolean takeSharkFromStock() {
        if (availableSharks <= 0) return false;
        availableSharks--; return true;
    }
    public boolean takeWhaleFromStock() {
        if (availableWhales <= 0) return false;
        availableWhales--; return true;
    }
    public boolean takeBoatFromStock() {
        if (availableBoats <= 0) return false;
        availableBoats--; return true;
    }

    // ── Bateaux bonus ─────────────────────────────────────────────────────────
    public void registerExtraBoat(Boat boat) {
        Objects.requireNonNull(boat, "Le bateau est obligatoire.");
        extraBoats.put(boat.getId(), boat);
        pieceState.registerBoat(boat);
    }

    public List<Boat> getExtraBoatsAt(HexCoordinate coord) {
        Objects.requireNonNull(coord, "La coordonnée est obligatoire.");
        return extraBoats.values().stream()
                .filter(boat -> boat.getStatus() == BoatStatus.ON_SEA)
                .filter(boat -> boat.getCoordinate().orElse(null) != null)
                .filter(boat -> boat.getCoordinate().orElseThrow().equals(coord))
                .toList();
    }

    // ── Tuiles en main ────────────────────────────────────────────────────────
    public List<TerrainTile> getHandTiles(Player player) {
        return Collections.unmodifiableList(
                handTiles.getOrDefault(player.getId(), List.of()));
    }

    public void addHandTile(Player player, TerrainTile tile) {
        handTiles.computeIfAbsent(player.getId(), id -> new ArrayList<>()).add(tile);
    }

    public boolean removeHandTile(Player player, TerrainTile tile) {
        List<TerrainTile> list = handTiles.get(player.getId());
        return list != null && list.remove(tile);
    }

    public boolean hasHandTile(Player player, TileEffect effect) {
        return getHandTiles(player).stream()
                .anyMatch(tile -> tile.getHiddenEffect() == effect);
    }

    public boolean removeFirstHandTile(Player player, TileEffect effect) {
        List<TerrainTile> list = handTiles.get(player.getId());
        if (list == null) return false;
        for (TerrainTile tile : new ArrayList<>(list)) {
            if (tile.getHiddenEffect() == effect) {
                return list.remove(tile);
            }
        }
        return false;
    }

    // ── Fin de jeu ────────────────────────────────────────────────────────────
    public boolean isGameOver() { return gameOver; }

    public void triggerGameOver() {
        if (gameOver) {
            return;
        }

        // Le volcan engloutit tout ce qui n'est pas déjà sauvé.
        for (Explorer explorer : pieceState.getAllExplorers()) {
            if (explorer.getStatus() != ExplorerStatus.SAVED) {
                explorer.removeFromGame();
            }
        }

        for (Boat boat : pieceState.getAllBoats()) {
            if (boat.getStatus() == BoatStatus.ON_SEA) {
                boat.removeFromGame();
            }
        }

        this.gameOver = true;
        this.winner   = computeWinner();
    }

    public Player getWinner() { return winner; }

    public List<Player> getWinners() {
        List<ScoreEntry> scores = computeFinalScores();
        if (scores.isEmpty()) return List.of();

        ScoreEntry best = scores.get(0);
        return scores.stream()
                .filter(entry -> entry.totalScore() == best.totalScore()
                        && entry.savedCount() == best.savedCount())
                .map(ScoreEntry::player)
                .toList();
    }

    public boolean hasPerfectTieForFirstPlace() {
        return getWinners().size() > 1;
    }

    public int getScore(Player player) {
        return player.getExplorers().stream()
                .filter(e -> e.getStatus() == ExplorerStatus.SAVED)
                .mapToInt(Explorer::getTreasureValue)
                .sum();
    }

    public List<ScoreEntry> computeFinalScores() {
        List<ScoreEntry> list = new ArrayList<>();
        for (Player p : pieceState.getPlayers()) {
            long saved = p.getExplorers().stream()
                    .filter(e -> e.getStatus() == ExplorerStatus.SAVED).count();
            list.add(new ScoreEntry(p, getScore(p), (int) saved));
        }
        list.sort((a, b) -> b.totalScore() != a.totalScore()
                ? Integer.compare(b.totalScore(), a.totalScore())
                : Integer.compare(b.savedCount(), a.savedCount()));
        return Collections.unmodifiableList(list);
    }

    private Player computeWinner() {
        Player best = null; int bestScore = -1; int bestCount = -1;
        for (Player p : pieceState.getPlayers()) {
            int score = getScore(p);
            int count = (int) p.getExplorers().stream()
                    .filter(e -> e.getStatus() == ExplorerStatus.SAVED).count();
            if (score > bestScore || (score == bestScore && count > bestCount)) {
                bestScore = score; bestCount = count; best = p;
            }
        }
        return best;
    }

    public record ScoreEntry(Player player, int totalScore, int savedCount) {}
}
