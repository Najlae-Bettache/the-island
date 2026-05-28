package fr.iatic.theisland.setup;

import fr.iatic.theisland.model.Board;
import fr.iatic.theisland.model.PieceState;
import fr.iatic.theisland.model.Player;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;


class GameSetupFactoryTest {

    @Test
    void eachPlayerReceivesTenExplorersAndTwoBoats() {
        Board board = BoardFactory.createInitialBoard();
        PieceState state = GameSetupFactory.createFourPlayerPieceState(board);

        assertEquals(4, state.getPlayers().size());
        for (Player player : state.getPlayers()) {
            assertEquals(10, player.getExplorers().size());
            assertEquals(2, player.getSetupBoats().size());
        }
    }

    @Test
    void explorersHaveExpectedTreasureValues() {
        Board board = BoardFactory.createInitialBoard();
        PieceState state = GameSetupFactory.createTwoPlayerPieceState(board);

        List<Integer> sortedValues = state.getPlayers().get(0).getExplorers().stream()
                .map(explorer -> explorer.getTreasureValue())
                .sorted()
                .toList();

        assertEquals(List.of(1, 1, 1, 2, 2, 3, 3, 4, 5, 6), sortedValues);
    }
}
