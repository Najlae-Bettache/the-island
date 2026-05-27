package fr.iatic.theisland.ui;

import fr.iatic.theisland.model.Board;
import fr.iatic.theisland.model.PieceState;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Font;

/**
 * Fenêtre principale du jeu complet.
 */
public final class MainFrame extends JFrame {

    public MainFrame(Board board, PieceState pieceState) {
        super("The Island");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(new Color(238, 244, 248));

        add(createHeader(board), BorderLayout.NORTH);
        add(new InteractivePanel(board, pieceState), BorderLayout.CENTER);

        setMinimumSize(new Dimension(1280, 780));
        setSize(1360, 820);
        setLocationRelativeTo(null);
        
    }

    private JPanel createHeader(Board board) {
        JPanel header = new GradientHeaderPanel();
        header.setLayout(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JLabel title = new JLabel("The Island");
        title.setForeground(Color.WHITE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 26f));

        header.add(title, BorderLayout.CENTER);
        return header;
    }

    private static final class GradientHeaderPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setPaint(new GradientPaint(
                    0, 0, new Color(14, 80, 118),
                    getWidth(), getHeight(), new Color(31, 154, 168)
            ));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
            super.paintComponent(graphics);
        }

        GradientHeaderPanel() {
            setOpaque(false);
        }
    }
}
