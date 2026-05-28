package fr.iatic.theisland.ui;

import fr.iatic.theisland.model.Board;
import fr.iatic.theisland.model.Boat;
import fr.iatic.theisland.model.Explorer;
import fr.iatic.theisland.model.ExplorerStatus;
import fr.iatic.theisland.model.GameState;
import fr.iatic.theisland.model.PieceState;
import fr.iatic.theisland.model.Player;
import fr.iatic.theisland.model.TerrainType;
import fr.iatic.theisland.model.TerrainTile;
import fr.iatic.theisland.model.TileEffect;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;


public final class InteractivePanel extends JPanel {

    private final InteractionController controller;
    private final JLabel phaseLabel;
    private final JLabel playerLabel;
    private final JLabel detailsLabel;
    private final JLabel statusLabel;
    private final InteractiveBoardPanel boardPanel;
    private final JPanel buttonsPanel;
    private final JEditorPane stateArea;

    public InteractivePanel(Board board, PieceState pieceState) {
        super(new BorderLayout(8, 8));
        setBackground(new Color(238, 244, 248));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        this.controller  = new InteractionController(pieceState);
        this.phaseLabel  = new JLabel();
        this.playerLabel = new JLabel();
        this.detailsLabel= new JLabel();
        this.statusLabel = new JLabel();
        this.buttonsPanel= new JPanel();
        this.stateArea   = new JEditorPane();

       
        this.boardPanel  = new InteractiveBoardPanel(
                board, pieceState, controller.getGameState(), controller, this::refreshTexts);

        add(createTopPanel(), BorderLayout.NORTH);
        JPanel center = new JPanel(new BorderLayout(8, 8));
        center.setOpaque(false);
        JScrollPane boardScroll = new JScrollPane(boardPanel);
        boardScroll.setBorder(BorderFactory.createLineBorder(new Color(124, 174, 196), 1));
        boardScroll.getVerticalScrollBar().setUnitIncrement(18);
        boardScroll.getHorizontalScrollBar().setUnitIncrement(18);
        boardScroll.setPreferredSize(new Dimension(720, 560));
        center.add(boardScroll, BorderLayout.CENTER);
        center.add(createSidePanel(), BorderLayout.EAST);
        add(center, BorderLayout.CENTER);
        refreshTexts();
    }

   

    private JPanel createTopPanel() {
        JPanel top = new JPanel(new BorderLayout(10, 10));
        top.setBackground(Color.WHITE);
        top.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(207, 224, 233), 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        JPanel labels = new JPanel();
        labels.setOpaque(false);
        labels.setLayout(new BoxLayout(labels, BoxLayout.Y_AXIS));
        phaseLabel.setFont(phaseLabel.getFont().deriveFont(Font.BOLD, 15f));
        phaseLabel.setForeground(new Color(21, 74, 101));
        playerLabel.setFont(playerLabel.getFont().deriveFont(Font.BOLD, 14f));
        playerLabel.setForeground(new Color(29, 48, 62));
        detailsLabel.setFont(detailsLabel.getFont().deriveFont(Font.PLAIN, 13f));
        detailsLabel.setForeground(new Color(53, 69, 80));
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, 13f));
        statusLabel.setForeground(new Color(84, 89, 96));
        labels.add(phaseLabel);
        labels.add(playerLabel);
        labels.add(detailsLabel);
        labels.add(statusLabel);
        buttonsPanel.setOpaque(false);
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));
        top.add(labels, BorderLayout.CENTER);
        top.add(buttonsPanel, BorderLayout.EAST);
        return top;
    }

    private JPanel createSidePanel() {
        JPanel side = new JPanel(new BorderLayout(10, 10));
        side.setPreferredSize(new Dimension(340, 620));
        side.setBackground(Color.WHITE);
        side.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(207, 224, 233), 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        JLabel title = new JLabel("Infos de partie");
        title.setForeground(new Color(21, 74, 101));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 17f));

        stateArea.setEditable(false);
        stateArea.setContentType("text/html");
        stateArea.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        stateArea.setFont(new Font("SansSerif", Font.PLAIN, 12));
        stateArea.setBackground(new Color(250, 253, 255));
        stateArea.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        JScrollPane stateScroll = new JScrollPane(stateArea);
        stateScroll.setBorder(BorderFactory.createLineBorder(new Color(221, 232, 238), 1));

        side.add(title, BorderLayout.NORTH);
        side.add(stateScroll, BorderLayout.CENTER);
        return side;
    }

 

    private void refreshTexts() {
        phaseLabel.setText("Phase : " + describePhase(controller.getPhase()));
        playerLabel.setText("Joueur actif : " + controller.getActivePlayer());

        String details = "";
        Explorer ep = controller.getNextUnplacedExplorerPreview();
        Boat     bp = controller.getNextUnplacedBoatPreview();
        if (ep != null) details = "Prochain explorateur à placer : " + ep.getId()
                + " | valeur à mémoriser : " + ep.getTreasureValue() + ".";
        else if (bp != null) details = "Prochain bateau à placer : " + bp.getId() + ".";
        else if (controller.getPhase() == DemoPhase.MOVEMENT)
            details = "Déplacements restants : " + controller.getMovementContext().getRemainingMovementPoints() + ".";
        else if (controller.getPhase() == DemoPhase.CREATURE_DICE
                || controller.getPhase() == DemoPhase.CREATURE_MOVE)
            details = controller.getCurrentDiceResult()
                    .map(d -> "Dé : " + switch (d) {
                        case SERPENT -> "Serpent de mer";
                        case REQUIN  -> "Requin";
                        case BALEINE -> "Baleine";
                    }).orElse("Lancez le dé !");
        else if (controller.getPhase() == DemoPhase.PLAY_HAND_TILE) {
            int n = controller.getGameState().getHandTiles(controller.getActivePlayer()).size();
            details = n == 0 ? "Aucune tuile en main." : n + " tuile(s) en main.";
        }

        detailsLabel.setText(details);
        statusLabel.setText("Message : " + controller.getStatusMessage());

        rebuildButtons();
        rebuildStateArea();
        boardPanel.repaint();
        revalidate();
        repaint();
    }

    private String describePhase(DemoPhase p) {
        return switch (p) {
            case SETUP_EXPLORERS -> "Placement des explorateurs";
            case SETUP_BOATS     -> "Placement des bateaux";
            case PLAY_HAND_TILE  -> "1/4 · Jouer une tuile de la main";
            case MOVEMENT        -> "2/4 · Déplacements des pions";
            case TILE_REMOVAL    -> "3/4 · Retirer une tuile de terrain";
            case CREATURE_DICE   -> "4/4 · Lancer le dé de créature";
            case CREATURE_MOVE   -> "4/4b · Déplacer la créature";
            case GAME_OVER       -> "Fin de partie — Scores finaux";
        };
    }

   
    private void rebuildButtons() {
        buttonsPanel.removeAll();
        DemoPhase phase = controller.getPhase();

        addBtn("Aide rapide", e -> showQuickHelp());

        // Placement auto (missions 1-2 : inchangé)
        if (phase == DemoPhase.SETUP_EXPLORERS || phase == DemoPhase.SETUP_BOATS) {
            addBtn("Placement automatique", e -> { controller.finishSetupAutomatically(); refreshTexts(); });
        }

        // Mission 3 : jouer une tuile rouge gardée en main
        if (phase == DemoPhase.PLAY_HAND_TILE) {
            java.util.List<TerrainTile> handTiles =
                    controller.getGameState().getHandTiles(controller.getActivePlayer());

            for (TerrainTile tile : handTiles) {
                String label = "Jouer : " + controller.labelHandTile(tile);
                if (tile.getHiddenEffect() == TileEffect.CANCEL_SHARK
                        || tile.getHiddenEffect() == TileEffect.CANCEL_WHALE) {
                    label = "Défense auto : " + controller.labelHandTile(tile);
                }
                addBtn(label, e -> { controller.playHandTile(tile); refreshTexts(); });
            }

            addBtn("Passer (pas de tuile)", e -> { controller.skipHandTilePhase(); refreshTexts(); });
        }

       
        if (phase == DemoPhase.MOVEMENT) {
            addBtn("Terminer les déplacements", e -> { controller.nextPlayer(); refreshTexts(); });
            if (controller.getSelectionKind() == SelectionKind.PASSENGER) {
                addBtn("Sauter dans l'eau", e -> { controller.jumpSelectedPassengerIntoWater(); refreshTexts(); });
            }
        }

       
        if (phase == DemoPhase.TILE_REMOVAL) {
            JButton info = new JButton("Cliquez une tuile surlignée");
            info.setEnabled(false);
            buttonsPanel.add(info);
        }

      
        if (phase == DemoPhase.CREATURE_DICE) {
            addBtn("Lancer le dé", e -> { controller.rollCreatureDice(); refreshTexts(); });
        }

    
        if (phase == DemoPhase.CREATURE_MOVE) {
            addBtn("Passer le déplacement", e -> { controller.skipCreatureMove(); refreshTexts(); });
        }

       
        if (phase != DemoPhase.GAME_OVER) {
            addBtn("Annuler sélection", e -> { controller.cancelSelection(); refreshTexts(); });
        }

        buttonsPanel.revalidate();
        buttonsPanel.repaint();
    }

    private void addBtn(String label, java.awt.event.ActionListener listener) {
        JButton btn = new JButton(label);
        btn.setAlignmentX(Component.RIGHT_ALIGNMENT);
        btn.setFocusPainted(false);
        btn.setMargin(new Insets(8, 14, 8, 14));
        btn.setBackground(new Color(236, 248, 252));
        btn.setForeground(new Color(20, 74, 99));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(155, 197, 214), 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        btn.addActionListener(listener);
        buttonsPanel.add(btn);
        buttonsPanel.add(javax.swing.Box.createVerticalStrut(6));
    }

    private void showQuickHelp() {
        String message = """
                Déroulement d'un tour :
                1. Jouer une tuile rouge de la main, ou passer.
                2. Faire jusqu'à 3 déplacements.
                3. Retirer une tuile de terrain surlignée.
                4. Lancer le dé de créature.
                5. Déplacer la créature indiquée, si possible.

                Rappels :
                - Un bateau contient maximum 3 explorateurs.
                - Un nageur se déplace d'une seule case de mer.
                - Le bateau va seulement sur la mer.
                - Pour sauver un explorateur, le passager débarque sur un abri adjacent.
                - Le volcan déclenche la fin de partie et les scores.
                - En cas d'égalité : on compare le nombre d'explorateurs sauvés.
                """;
        JOptionPane.showMessageDialog(this, message, "Aide rapide", JOptionPane.INFORMATION_MESSAGE);
    }

    
    private void rebuildStateArea() {
    PieceState state = controller.getState();
    GameState  gs    = controller.getGameState();
    Board      board = state.getBoard();

    StringBuilder html = new StringBuilder();
    html.append("<html><body style='font-family:SansSerif; background:#FCFEFF; color:#1F3440; margin:6px;'>");

    html.append("<div style='font-size:14px; font-weight:bold; color:#0E506F; margin-bottom:8px;'>Résumé</div>");
    html.append("<table width='100%' cellspacing='4' cellpadding='0'>");
    html.append("<tr>");
    html.append(simpleCard("Explorateurs", String.valueOf(state.countPlacedExplorers())));
    html.append(simpleCard("Bateaux", String.valueOf(state.countPlacedBoats())));
    html.append("</tr><tr>");
    html.append(simpleCard("Nageurs", String.valueOf(state.countSwimmers())));
    html.append(simpleCard("Sauvés", String.valueOf(state.countSavedExplorers())));
    html.append("</tr></table>");

    html.append(sectionTitle("Partie"));
    html.append("<div style='padding:7px; background:#F6FBFD; border:1px solid #D7E7EF; border-radius:8px;'>");
    html.append("Tuiles restantes : <b>").append(board.countPlacedTerrainTiles()).append("</b><br>");
    html.append("🏖 Plages : <b>").append(board.countPlacedTerrainTilesOfType(TerrainType.BEACH)).append("</b>");
    html.append("&nbsp;&nbsp; 🌲 Forêts : <b>").append(board.countPlacedTerrainTilesOfType(TerrainType.FOREST)).append("</b>");
    html.append("&nbsp;&nbsp; ⛰ Montagnes : <b>").append(board.countPlacedTerrainTilesOfType(TerrainType.MOUNTAIN)).append("</b><br><br>");
    long serpents = gs.getCreatures().stream().filter(c -> c.getType() == fr.iatic.theisland.model.CreatureType.SERPENT).count();
    long requins = gs.getCreatures().stream().filter(c -> c.getType() == fr.iatic.theisland.model.CreatureType.REQUIN).count();
    long baleines = gs.getCreatures().stream().filter(c -> c.getType() == fr.iatic.theisland.model.CreatureType.BALEINE).count();
    html.append("Créatures en jeu : <b>").append(gs.getCreatures().size()).append("</b><br>");
    html.append("🐍 ").append(serpents).append(" &nbsp; 🦈 ").append(requins).append(" &nbsp; 🐋 ").append(baleines).append("<br>");
    html.append("Stock : 🦈 ").append(gs.getAvailableSharks()).append(" &nbsp; 🐋 ").append(gs.getAvailableWhales());
    html.append("</div>");

    html.append(sectionTitle("Joueurs"));
    for (Player player : state.getPlayers()) {
        boolean active = player == controller.getActivePlayer();
        String bg = active ? "#E7F7F0" : "#FFFFFF";
        String border = active ? "#2EA36C" : "#D7E7EF";
        String titleColor = active ? "#0B6B4B" : "#0E506F";

        html.append("<div style='margin:6px 0; padding:7px; background:")
                .append(bg)
                .append("; border:1px solid ")
                .append(border)
                .append("; border-radius:8px;'>");
        html.append("<div style='font-weight:bold; font-size:14px; color:")
                .append(titleColor)
                .append(";'>")
                .append(active ? "▶ " : "")
                .append(player.getName())
                .append("</div>");
        html.append("<div style='margin-top:5px; line-height:1.5;'>");
        html.append("✅ Sauvés : <b>").append(count(player, ExplorerStatus.SAVED)).append("</b>");
        html.append("&nbsp;&nbsp; 🌊 Nageurs : <b>").append(count(player, ExplorerStatus.SWIMMER)).append("</b><br>");
        html.append("🏝 Île : <b>").append(count(player, ExplorerStatus.ON_LAND)).append("</b>");
        html.append("&nbsp;&nbsp; ⛵ Bateau : <b>").append(count(player, ExplorerStatus.IN_BOAT)).append("</b>");
        html.append("&nbsp;&nbsp; 🃏 Main : <b>").append(gs.getHandTiles(player).size()).append("</b>");
        if (controller.getPhase() == DemoPhase.GAME_OVER) {
            html.append("&nbsp;&nbsp; ⭐ Score : <b>").append(gs.getScore(player)).append("</b>");
        }
        html.append("</div></div>");
    }

    if (controller.getPhase() == DemoPhase.GAME_OVER) {
        html.append(sectionTitle("Scores finaux"));
        java.util.List<Player> winners = gs.getWinners();
        boolean perfectTie = gs.hasPerfectTieForFirstPlace();

        html.append("<div style='padding:7px; background:#FFF7E0; border:1px solid #E9C46A; border-radius:8px;'>");
        for (GameState.ScoreEntry e : gs.computeFinalScores()) {
            boolean isWinner = winners.contains(e.player());
            html.append("<div style='margin:5px 0;'>")
                    .append(isWinner ? "🏆 " : "• ")
                    .append("<b>").append(e.player().getName()).append("</b>")
                    .append(" : ")
                    .append(e.totalScore()).append(" pts");
            if (e.savedCount() > 0) {
                html.append(" (").append(e.savedCount()).append(" sauvés)");
            }
            html.append("</div>");
        }
        if (perfectTie) {
            html.append("<br><b>Égalité parfaite</b>");
        } else if (!winners.isEmpty()) {
            html.append("<br><b>Gagnant : </b>").append(winners.get(0).getName());
        }
        html.append("</div>");
    }

    html.append(sectionTitle("Rappels"));
    html.append("<div style='padding:7px; background:#F4FBFD; border:1px solid #D7E7EF; border-radius:8px; line-height:1.45;'>");
    html.append("• 3 déplacements par tour<br>");
    html.append("• retrait : plage → forêt → montagne<br>");
    html.append("• requin/serpent : tue les nageurs<br>");
    html.append("• baleine : chavire les bateaux occupés<br>");
    html.append("• volcan : fin immédiate");
    html.append("</div>");

    html.append("</body></html>");

    stateArea.setText(html.toString());
    stateArea.setCaretPosition(0);
}

private String simpleCard(String label, String value) {
    return "<td style='background:#FFFFFF; border:1px solid #D7E7EF; border-radius:8px; width:50%; padding:7px;'>"
            + "<div style='font-size:12px; color:#607D8B;'>" + label + "</div>"
            + "<div style='font-size:18px; font-weight:bold; color:#0E506F;'>" + value + "</div>"
            + "</td>";
}

private String card(String label, String value) {
        return simpleCard(label, value);
    }

    private String sectionTitle(String title) {
        return "<div style='font-size:14px; font-weight:bold; color:#0E506F; margin-top:12px; margin-bottom:8px;'>"
                + title + "</div>";
    }

    private String labelCreature(fr.iatic.theisland.model.CreatureType type) {
        return switch (type) {
            case SERPENT -> "Serpent";
            case REQUIN -> "Requin";
            case BALEINE -> "Baleine";
        };
    }

    private String fmt(String label, long val) {
        return String.format("%-32s %3d%n", label, val);
    }

    private long count(Player player, ExplorerStatus status) {
        return player.getExplorers().stream().filter(e -> e.getStatus() == status).count();
    }
}
