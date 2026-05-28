package fr.iatic.theisland.model;

import java.util.Objects;
import java.util.Optional;

/**
 * Explorateur d'un joueur.
 */
public final class Explorer {

    private final String id;
    private final Player owner;
    private final int treasureValue;
    private ExplorerStatus status;
    private HexCoordinate coordinate;
    private String boatId;
    private boolean leftMainIsland;

    public Explorer(String id, Player owner, int treasureValue) {
        this.id = requireNonBlank(id, "L'identifiant de l'explorateur est obligatoire.");
        this.owner = Objects.requireNonNull(owner, "Le propriétaire est obligatoire.");
        if (treasureValue < 1 || treasureValue > 6) {
            throw new IllegalArgumentException("La valeur du trésor doit être comprise entre 1 et 6.");
        }
        this.treasureValue = treasureValue;
        this.status = ExplorerStatus.UNPLACED;
    }

    private String requireNonBlank(String value, String message) {
        Objects.requireNonNull(value, message);
        if (value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    public void placeOnLand(HexCoordinate coordinate) {
        Objects.requireNonNull(coordinate, "La coordonnée est obligatoire.");
        if (status != ExplorerStatus.UNPLACED) {
            throw new IllegalStateException("Cet explorateur est déjà placé.");
        }
        this.status = ExplorerStatus.ON_LAND;
        this.coordinate = coordinate;
        this.boatId = null;
    }

    public void moveOnLand(HexCoordinate coordinate) {
        Objects.requireNonNull(coordinate, "La coordonnée est obligatoire.");
        if (status != ExplorerStatus.ON_LAND) {
            throw new IllegalStateException("Seul un explorateur sur terre peut se déplacer sur terre.");
        }
        this.coordinate = coordinate;
    }

    public void boardBoat(String targetBoatId) {
        targetBoatId = requireNonBlank(targetBoatId, "L'identifiant du bateau est obligatoire.");
        if (status != ExplorerStatus.ON_LAND && status != ExplorerStatus.SWIMMER && status != ExplorerStatus.IN_BOAT) {
            throw new IllegalStateException("Cet explorateur ne peut pas monter dans un bateau.");
        }
        this.status = ExplorerStatus.IN_BOAT;
        this.boatId = targetBoatId;
        this.coordinate = null;
        this.leftMainIsland = true;
    }

    public void becomeSwimmer(HexCoordinate coordinate) {
        Objects.requireNonNull(coordinate, "La coordonnée est obligatoire.");
        if (status != ExplorerStatus.ON_LAND && status != ExplorerStatus.IN_BOAT) {
            throw new IllegalStateException("Cet explorateur ne peut pas devenir nageur depuis cet état.");
        }
        this.status = ExplorerStatus.SWIMMER;
        this.coordinate = coordinate;
        this.boatId = null;
        this.leftMainIsland = true;
    }

    public void moveAsSwimmer(HexCoordinate coordinate) {
        Objects.requireNonNull(coordinate, "La coordonnée est obligatoire.");
        if (status != ExplorerStatus.SWIMMER) {
            throw new IllegalStateException("Seul un nageur peut se déplacer en mer.");
        }
        this.coordinate = coordinate;
    }

    public void saveOnRescueIsland(HexCoordinate coordinate) {
        Objects.requireNonNull(coordinate, "La coordonnée est obligatoire.");
        if (status != ExplorerStatus.IN_BOAT) {
            throw new IllegalStateException("Seul un explorateur dans un bateau peut être sauvé ici.");
        }
        this.status = ExplorerStatus.SAVED;
        this.coordinate = coordinate;
        this.boatId = null;
    }

    public void removeFromGame() {
        this.status = ExplorerStatus.REMOVED;
        this.coordinate = null;
        this.boatId = null;
    }

    public String getId() {
        return id;
    }

    public Player getOwner() {
        return owner;
    }

    public int getTreasureValue() {
        return treasureValue;
    }

    public ExplorerStatus getStatus() {
        return status;
    }

    public Optional<HexCoordinate> getCoordinate() {
        return Optional.ofNullable(coordinate);
    }

    public Optional<String> getBoatId() {
        return Optional.ofNullable(boatId);
    }

    public boolean hasLeftMainIsland() {
        return leftMainIsland;
    }
}
