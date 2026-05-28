package fr.iatic.theisland.ui;

import fr.iatic.theisland.model.PieceState;
import fr.iatic.theisland.model.Player;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.Font;


public final class LegendPanel extends JPanel {

    public LegendPanel(PieceState state) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(18, 12, 18, 18));
        setPreferredSize(new Dimension(300, 820));

        JLabel title = new JLabel("Démonstration");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        add(title);

        add(new JLabel(" "));
        add(new JLabel("A = abri"));
        add(new JLabel("S = départ serpent"));
        add(new JLabel("P / F / M = tuile"));
        add(new JLabel("B = bateau"));

        add(new JLabel(" "));
        JLabel playersTitle = new JLabel("Joueurs");
        playersTitle.setFont(playersTitle.getFont().deriveFont(Font.BOLD, 16f));
        add(playersTitle);

        for (Player player : state.getPlayers()) {
            add(new JLabel("• " + player));
        }

        add(new JLabel(" "));
        JLabel implementationTitle = new JLabel("Règles mission 2 prêtes");
        implementationTitle.setFont(implementationTitle.getFont().deriveFont(Font.BOLD, 16f));
        add(implementationTitle);

        add(new JLabel("• placement explorateurs"));
        add(new JLabel("• placement bateaux"));
        add(new JLabel("• mouvements explorateurs"));
        add(new JLabel("• nageurs"));
        add(new JLabel("• contrôle des bateaux"));
        add(new JLabel("• sauvetage"));
    }
}
