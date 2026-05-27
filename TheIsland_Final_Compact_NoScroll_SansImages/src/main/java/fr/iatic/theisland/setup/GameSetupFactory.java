package fr.iatic.theisland.setup;

import fr.iatic.theisland.model.Board;
import fr.iatic.theisland.model.Boat;
import fr.iatic.theisland.model.Explorer;
import fr.iatic.theisland.model.PieceState;
import fr.iatic.theisland.model.Player;
import fr.iatic.theisland.model.PlayerColor;

import java.util.ArrayList;
import java.util.List;

/**
 * Crée les joueurs, explorateurs et bateaux nécessaires à la partie.
 */
public final class GameSetupFactory {

    private static final List<Integer> TREASURE_VALUES = List.of(1, 1, 1, 2, 2, 3, 3, 4, 5, 6);

    private GameSetupFactory() {
        // Classe utilitaire.
    }

    public static PieceState createFourPlayerPieceState(Board board) {
        return createPieceState(board, List.of(
                new PlayerDefinition("P1", "Joueur 1", PlayerColor.RED),
                new PlayerDefinition("P2", "Joueur 2", PlayerColor.BLUE),
                new PlayerDefinition("P3", "Joueur 3", PlayerColor.GREEN),
                new PlayerDefinition("P4", "Joueur 4", PlayerColor.YELLOW)
        ));
    }

    public static PieceState createTwoPlayerPieceState(Board board) {
        return createPieceState(board, List.of(
                new PlayerDefinition("P1", "Joueur 1", PlayerColor.RED),
                new PlayerDefinition("P2", "Joueur 2", PlayerColor.BLUE)
        ));
    }

    public static PieceState createPieceState(Board board, List<PlayerDefinition> definitions) {
        if (definitions == null || definitions.isEmpty()) {
            throw new IllegalArgumentException("Il faut définir au moins un joueur.");
        }

        List<Player> players = new ArrayList<>();
        for (PlayerDefinition definition : definitions) {
            Player player = new Player(definition.id(), definition.name(), definition.color());
            createExplorersFor(player);
            createSetupBoatsFor(player);
            players.add(player);
        }
        return new PieceState(board, players);
    }

    private static void createExplorersFor(Player player) {
        int index = 1;
        for (Integer treasureValue : TREASURE_VALUES) {
            String explorerId = player.getId() + "-E" + index;
            Explorer explorer = new Explorer(explorerId, player, treasureValue);
            player.addExplorer(explorer);
            index++;
        }
    }

    private static void createSetupBoatsFor(Player player) {
        for (int index = 1; index <= 2; index++) {
            String boatId = player.getId() + "-B" + index;
            Boat boat = new Boat(boatId, player);
            player.addSetupBoat(boat);
        }
    }

    public record PlayerDefinition(String id, String name, PlayerColor color) {
        public PlayerDefinition {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("L'identifiant du joueur est obligatoire.");
            }
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Le nom du joueur est obligatoire.");
            }
            if (color == null) {
                throw new IllegalArgumentException("La couleur du joueur est obligatoire.");
            }
        }
    }
}
