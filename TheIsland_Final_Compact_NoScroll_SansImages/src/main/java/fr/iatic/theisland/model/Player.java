package fr.iatic.theisland.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Joueur humain de la partie.
 */
public final class Player {

    private final String id;
    private final String name;
    private final PlayerColor color;
    private final List<Explorer> explorers = new ArrayList<>();
    private final List<Boat> setupBoats = new ArrayList<>();

    public Player(String id, String name, PlayerColor color) {
        this.id = requireNonBlank(id, "L'identifiant du joueur est obligatoire.");
        this.name = requireNonBlank(name, "Le nom du joueur est obligatoire.");
        this.color = Objects.requireNonNull(color, "La couleur du joueur est obligatoire.");
    }

    private String requireNonBlank(String value, String message) {
        Objects.requireNonNull(value, message);
        if (value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    public void addExplorer(Explorer explorer) {
        explorers.add(Objects.requireNonNull(explorer, "L'explorateur est obligatoire."));
    }

    public void addSetupBoat(Boat boat) {
        setupBoats.add(Objects.requireNonNull(boat, "Le bateau est obligatoire."));
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public PlayerColor getColor() {
        return color;
    }

    public List<Explorer> getExplorers() {
        return Collections.unmodifiableList(explorers);
    }

    public List<Boat> getSetupBoats() {
        return Collections.unmodifiableList(setupBoats);
    }

    @Override
    public String toString() {
        return name + " (" + color.getLabel() + ")";
    }
}
