package fr.iatic.theisland.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Bateau pouvant accueillir jusqu'à trois explorateurs.
 */
public final class Boat {

    public static final int MAX_PASSENGERS = 3;

    private final String id;
    private final Player setupOwner;
    private final List<Explorer> passengers = new ArrayList<>();
    private BoatStatus status;
    private HexCoordinate coordinate;

    public Boat(String id, Player setupOwner) {
        this.id = requireNonBlank(id, "L'identifiant du bateau est obligatoire.");
        this.setupOwner = Objects.requireNonNull(setupOwner, "Le joueur ayant reçu le bateau est obligatoire.");
        this.status = BoatStatus.UNPLACED;
    }

    private String requireNonBlank(String value, String message) {
        Objects.requireNonNull(value, message);
        if (value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    public void placeAt(HexCoordinate coordinate) {
        Objects.requireNonNull(coordinate, "La coordonnée est obligatoire.");
        if (status != BoatStatus.UNPLACED) {
            throw new IllegalStateException("Ce bateau est déjà placé.");
        }
        this.status = BoatStatus.ON_SEA;
        this.coordinate = coordinate;
    }

    public void moveTo(HexCoordinate coordinate) {
        Objects.requireNonNull(coordinate, "La coordonnée est obligatoire.");
        if (status != BoatStatus.ON_SEA) {
            throw new IllegalStateException("Seul un bateau sur mer peut se déplacer.");
        }
        this.coordinate = coordinate;
    }

    public void removeFromGame() {
        this.status = BoatStatus.REMOVED;
        this.coordinate = null;
        passengers.clear();
    }

    public boolean hasFreeSeat() {
        return passengers.size() < MAX_PASSENGERS;
    }

    public boolean isEmpty() {
        return passengers.isEmpty();
    }

    public void addPassenger(Explorer explorer) {
        Objects.requireNonNull(explorer, "L'explorateur est obligatoire.");
        if (!hasFreeSeat()) {
            throw new IllegalStateException("Le bateau est complet.");
        }
        if (passengers.contains(explorer)) {
            throw new IllegalStateException("Cet explorateur est déjà dans ce bateau.");
        }
        passengers.add(explorer);
    }

    public void removePassenger(Explorer explorer) {
        Objects.requireNonNull(explorer, "L'explorateur est obligatoire.");
        if (!passengers.remove(explorer)) {
            throw new IllegalStateException("Cet explorateur n'est pas dans ce bateau.");
        }
    }

    public String getId() {
        return id;
    }

    public Player getSetupOwner() {
        return setupOwner;
    }

    public BoatStatus getStatus() {
        return status;
    }

    public Optional<HexCoordinate> getCoordinate() {
        return Optional.ofNullable(coordinate);
    }

    public List<Explorer> getPassengers() {
        return Collections.unmodifiableList(passengers);
    }
}
