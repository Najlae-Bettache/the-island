package fr.iatic.theisland.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * État des pions liés aux missions 1 et 2 :
 * joueurs, explorateurs et bateaux.
 */
public final class PieceState {

    private final Board board;
    private final List<Player> players;
    private final Map<String, Explorer> explorersById = new LinkedHashMap<>();
    private final Map<String, Boat> boatsById = new LinkedHashMap<>();

    public PieceState(Board board, List<Player> players) {
        this.board = Objects.requireNonNull(board, "Le plateau est obligatoire.");
        this.players = List.copyOf(Objects.requireNonNull(players, "La liste des joueurs est obligatoire."));

        if (players.isEmpty()) {
            throw new IllegalArgumentException("Il faut au moins un joueur.");
        }

        for (Player player : players) {
            for (Explorer explorer : player.getExplorers()) {
                explorersById.put(explorer.getId(), explorer);
            }
            for (Boat boat : player.getSetupBoats()) {
                boatsById.put(boat.getId(), boat);
            }
        }
    }

    public Board getBoard() {
        return board;
    }

    public List<Player> getPlayers() {
        return Collections.unmodifiableList(players);
    }

    public List<Explorer> getAllExplorers() {
        return Collections.unmodifiableList(new ArrayList<>(explorersById.values()));
    }

    public List<Boat> getAllBoats() {
        return Collections.unmodifiableList(new ArrayList<>(boatsById.values()));
    }

    public void registerBoat(Boat boat) {
        Objects.requireNonNull(boat, "Le bateau est obligatoire.");
        boatsById.put(boat.getId(), boat);
    }

    public Optional<Boat> findBoatById(String boatId) {
        return Optional.ofNullable(boatsById.get(boatId));
    }

    public List<Explorer> getLandExplorersAt(HexCoordinate coordinate) {
        Objects.requireNonNull(coordinate, "La coordonnée est obligatoire.");
        return explorersById.values().stream()
                .filter(explorer -> explorer.getStatus() == ExplorerStatus.ON_LAND)
                .filter(explorer -> explorer.getCoordinate().orElseThrow().equals(coordinate))
                .toList();
    }

    public List<Explorer> getSwimmersAt(HexCoordinate coordinate) {
        Objects.requireNonNull(coordinate, "La coordonnée est obligatoire.");
        return explorersById.values().stream()
                .filter(explorer -> explorer.getStatus() == ExplorerStatus.SWIMMER)
                .filter(explorer -> explorer.getCoordinate().orElseThrow().equals(coordinate))
                .toList();
    }

    public List<Boat> getBoatsAt(HexCoordinate coordinate) {
        Objects.requireNonNull(coordinate, "La coordonnée est obligatoire.");
        return boatsById.values().stream()
                .filter(boat -> boat.getStatus() == BoatStatus.ON_SEA)
                .filter(boat -> boat.getCoordinate().orElseThrow().equals(coordinate))
                .toList();
    }

    public boolean hasAnyLandExplorerAt(HexCoordinate coordinate) {
        return !getLandExplorersAt(coordinate).isEmpty();
    }

    public boolean hasAnyBoatAt(HexCoordinate coordinate) {
        return !getBoatsAt(coordinate).isEmpty();
    }

    public long countPlacedExplorers() {
        return explorersById.values().stream()
                .filter(explorer -> explorer.getStatus() != ExplorerStatus.UNPLACED)
                .count();
    }

    public long countPlacedBoats() {
        return boatsById.values().stream()
                .filter(boat -> boat.getStatus() != BoatStatus.UNPLACED)
                .count();
    }

    public long countSwimmers() {
        return explorersById.values().stream()
                .filter(explorer -> explorer.getStatus() == ExplorerStatus.SWIMMER)
                .count();
    }

    public long countSavedExplorers() {
        return explorersById.values().stream()
                .filter(explorer -> explorer.getStatus() == ExplorerStatus.SAVED)
                .count();
    }
}
