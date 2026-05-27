package fr.iatic.theisland.movement;

import fr.iatic.theisland.model.Boat;
import fr.iatic.theisland.model.Explorer;
import fr.iatic.theisland.model.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Calcule quels joueurs contrôlent un bateau.
 */
public final class BoatControlService {

    public boolean canPlayerMoveBoat(Player player, Boat boat) {
        Objects.requireNonNull(player, "Le joueur est obligatoire.");
        Objects.requireNonNull(boat, "Le bateau est obligatoire.");

        if (boat.isEmpty()) {
            return true;
        }

        Map<Player, Integer> counts = new HashMap<>();
        for (Explorer passenger : boat.getPassengers()) {
            counts.merge(passenger.getOwner(), 1, Integer::sum);
        }

        int highestCount = counts.values().stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);

        return counts.getOrDefault(player, 0) == highestCount;
    }
}
