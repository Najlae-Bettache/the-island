package fr.iatic.theisland.movement;

import fr.iatic.theisland.model.Explorer;
import fr.iatic.theisland.model.Player;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Contexte des déplacements d'un joueur sur son tour.
 * Le gestionnaire de tour pourra créer un nouveau contexte à chaque début de phase de déplacement.
 */
public final class MovementContext {

    public static final int DEFAULT_MOVEMENT_POINTS = 3;

    private final Player activePlayer;
    private int remainingMovementPoints;
    private final Set<String> swimmersThatAlreadyMovedThroughSea = new HashSet<>();

    public MovementContext(Player activePlayer) {
        this(activePlayer, DEFAULT_MOVEMENT_POINTS);
    }

    public MovementContext(Player activePlayer, int movementPoints) {
        this.activePlayer = Objects.requireNonNull(activePlayer, "Le joueur actif est obligatoire.");
        if (movementPoints < 0) {
            throw new IllegalArgumentException("Le nombre de déplacements ne peut pas être négatif.");
        }
        this.remainingMovementPoints = movementPoints;
    }

    public Player getActivePlayer() {
        return activePlayer;
    }

    public int getRemainingMovementPoints() {
        return remainingMovementPoints;
    }

    public boolean hasAtLeastOneMovementPoint() {
        return remainingMovementPoints >= 1;
    }

    public boolean tryConsumeOneMovementPoint() {
        if (!hasAtLeastOneMovementPoint()) {
            return false;
        }
        remainingMovementPoints--;
        return true;
    }

    public boolean hasAlreadyMovedThroughSea(Explorer explorer) {
        Objects.requireNonNull(explorer, "L'explorateur est obligatoire.");
        return swimmersThatAlreadyMovedThroughSea.contains(explorer.getId());
    }

    public void markSeaMovementUsed(Explorer explorer) {
        Objects.requireNonNull(explorer, "L'explorateur est obligatoire.");
        swimmersThatAlreadyMovedThroughSea.add(explorer.getId());
    }
}
