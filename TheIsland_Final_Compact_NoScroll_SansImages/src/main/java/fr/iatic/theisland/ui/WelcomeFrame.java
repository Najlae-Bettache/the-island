
package fr.iatic.theisland.ui;

import fr.iatic.theisland.model.Board;
import fr.iatic.theisland.model.PieceState;
import fr.iatic.theisland.setup.BoardFactory;
import fr.iatic.theisland.setup.GameSetupFactory;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;

/**
 * Écran d'accueil du jeu.
 */
public final class WelcomeFrame extends JFrame {

    public WelcomeFrame() {
        super("The Island");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(920, 650);
        setMinimumSize(new Dimension(820, 560));
        setLocationRelativeTo(null);
        setContentPane(createContent());
    }

    private JPanel createContent() {
        JPanel root = new IslandMenuPanel();
        root.setLayout(new BorderLayout());
        root.setBorder(BorderFactory.createEmptyBorder(55, 80, 55, 80));

        JPanel center = new JPanel(new BorderLayout(0, 28));
        center.setOpaque(false);

        JLabel title = new JLabel("THE ISLAND", SwingConstants.CENTER);
        title.setForeground(Color.WHITE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 64f));

        JLabel subtitle = new JLabel("Sauvez vos explorateurs avant l'éruption du volcan", SwingConstants.CENTER);
        subtitle.setForeground(new Color(226, 246, 255));
        subtitle.setFont(subtitle.getFont().deriveFont(Font.BOLD, 20f));

        JPanel titles = new JPanel(new GridLayout(2, 1, 0, 8));
        titles.setOpaque(false);
        titles.add(title);
        titles.add(subtitle);

        JPanel buttons = new JPanel(new GridLayout(3, 1, 0, 16));
        buttons.setOpaque(false);
        buttons.setBorder(BorderFactory.createEmptyBorder(30, 240, 90, 240));

        JButton newGame = createMenuButton("Nouvelle partie");
        JButton rules = createMenuButton("Règles du jeu");
        JButton quit = createMenuButton("Quitter");

        newGame.addActionListener(e -> startNewGame());
        rules.addActionListener(e -> showRules());
        quit.addActionListener(e -> dispose());

        buttons.add(newGame);
        buttons.add(rules);
        buttons.add(quit);

        center.add(titles, BorderLayout.NORTH);
        center.add(buttons, BorderLayout.CENTER);

        root.add(center, BorderLayout.CENTER);
        return root;
    }

    private JButton createMenuButton(String text) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setFont(button.getFont().deriveFont(Font.BOLD, 20f));
        button.setForeground(new Color(12, 76, 95));
        button.setBackground(new Color(238, 250, 252));
        button.setMargin(new Insets(18, 24, 18, 24));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(178, 225, 232), 2),
                BorderFactory.createEmptyBorder(8, 18, 8, 18)
        ));
        return button;
    }

    private void startNewGame() {
        Board board = BoardFactory.createInitialBoard();
        PieceState pieceState = GameSetupFactory.createFourPlayerPieceState(board);

        MainFrame game = new MainFrame(board, pieceState);
        game.setVisible(true);
        dispose();
    }

    private void showRules() {
        String rules = """
                But du jeu
                Sauver un maximum d'explorateurs en les amenant sur les abris.

                Déroulement d'un tour
                1. Jouer une tuile de la main ou passer.
                2. Faire jusqu'à 3 déplacements.
                3. Retirer une tuile de terrain.
                4. Lancer le dé de créature.
                5. Déplacer la créature indiquée.

                Déplacements
                - Un bateau contient maximum 3 explorateurs.
                - Un nageur se déplace d'une seule case de mer.
                - Un bateau vide peut être déplacé par n'importe quel joueur.
                - Un bateau occupé est contrôlé par majorité.
                - Un explorateur sauvé compte seulement s'il arrive sur un abri.

                Créatures
                - Requin : élimine les nageurs.
                - Serpent : élimine nageurs et bateaux occupés.
                - Baleine : chavire les bateaux occupés.

                Fin de partie
                Le volcan met fin à la partie.
                Les scores sont calculés avec les explorateurs sauvés.
                """;

        JOptionPane.showMessageDialog(this, rules, "Règles du jeu", JOptionPane.INFORMATION_MESSAGE);
    }

    private static final class IslandMenuPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();

            g2.setPaint(new GradientPaint(
                    0, 0, new Color(12, 72, 106),
                    getWidth(), getHeight(), new Color(33, 159, 154)
            ));
            g2.fillRect(0, 0, getWidth(), getHeight());

            // Soleil discret
            g2.setColor(new Color(255, 224, 130, 95));
            g2.fillOval(getWidth() - 175, 70, 92, 92);

            // Petit bateau décoratif
            int bx = 110;
            int by = getHeight() - 185;
            g2.setColor(new Color(255, 255, 255, 180));
            g2.fillPolygon(new int[] {bx, bx + 34, bx}, new int[] {by - 70, by - 45, by - 20}, 3);
            g2.setColor(new Color(90, 55, 28, 180));
            g2.fillPolygon(new int[] {bx - 28, bx + 48, bx + 34, bx - 14}, new int[] {by, by, by + 16, by + 16}, 4);

            // Île stylisée
            int islandY = getHeight() - 135;
            g2.setColor(new Color(231, 200, 118, 170));
            g2.fillOval(getWidth() / 2 - 210, islandY, 420, 82);

            g2.setColor(new Color(61, 148, 77, 180));
            g2.fillOval(getWidth() / 2 - 120, islandY - 22, 240, 70);

            // Volcan stylisé
            int vx = getWidth() / 2;
            int vy = islandY - 20;
            g2.setColor(new Color(95, 85, 82, 190));
            g2.fillPolygon(
                    new int[] {vx - 55, vx, vx + 55},
                    new int[] {vy + 55, vy - 42, vy + 55},
                    3
            );
            g2.setColor(new Color(235, 87, 54, 210));
            g2.fillOval(vx - 15, vy - 45, 30, 18);

            // Vagues
            g2.setColor(new Color(255, 255, 255, 65));
            for (int i = 0; i < 7; i++) {
                int x = 80 + i * 115;
                int y = getHeight() - 85 + (i % 2) * 18;
                g2.drawArc(x, y, 34, 14, 0, 180);
                g2.drawArc(x + 30, y, 34, 14, 0, 180);
            }

            g2.dispose();
        }
    }
}
