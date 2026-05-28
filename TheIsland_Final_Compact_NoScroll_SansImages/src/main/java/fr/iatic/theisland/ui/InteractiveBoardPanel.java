package fr.iatic.theisland.ui;

import fr.iatic.theisland.model.Board;
import fr.iatic.theisland.model.Boat;
import fr.iatic.theisland.model.Creature;
import fr.iatic.theisland.model.CreatureType;
import fr.iatic.theisland.model.Explorer;
import fr.iatic.theisland.model.GameState;
import fr.iatic.theisland.model.HexCell;
import fr.iatic.theisland.model.HexCoordinate;
import fr.iatic.theisland.model.PieceState;
import fr.iatic.theisland.model.TerrainType;

import javax.swing.JPanel;
import javax.swing.ToolTipManager;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


public final class InteractiveBoardPanel extends JPanel {

    
    private static final int HEX_RADIUS      = 20;
    private static final int HEX_HEIGHT      = (int) Math.round(Math.sqrt(3) * HEX_RADIUS);
    private static final int HORIZONTAL_STEP = (int) Math.round(1.5 * HEX_RADIUS);
    private static final int VERTICAL_STEP   = HEX_HEIGHT;
    private static final int MARGIN_X        = 30;
    private static final int MARGIN_Y        = 28;

    private final Board                    board;
    private final PieceState               pieceState;
    private final GameState                gameState;    
    private final InteractionController    controller;
    private final Runnable                 onStateChanged;
    private final Map<Polygon, HexCell>    polygonCellMap = new HashMap<>();

    public InteractiveBoardPanel(
            Board board,
            PieceState pieceState,
            GameState gameState,
            InteractionController controller,
            Runnable onStateChanged
    ) {
        this.board          = board;
        this.pieceState     = pieceState;
        this.gameState      = gameState;
        this.controller     = controller;
        this.onStateChanged = onStateChanged;

        setBackground(new Color(225, 237, 244));
        setPreferredSize(new Dimension(650, 520));
        ToolTipManager.sharedInstance().registerComponent(this);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                findCellAt(event.getPoint()).ifPresent(cell -> {
                    controller.handleCellClick(cell.getCoordinate());
                    repaint();
                    onStateChanged.run();
                });
            }
        });
    }

    
    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        polygonCellMap.clear();

        for (HexCell cell : board.getAllCells()) {
            Polygon hexagon = createHexagon(cell.getCoordinate());
            polygonCellMap.put(hexagon, cell);

            drawCellFill(g2, hexagon, cell);
            drawCellDecoration(g2, hexagon, cell);
            drawBorder(g2, hexagon, cell);
            drawTileRemovalHighlight(g2, hexagon, cell); 
            drawSelectionHighlight(g2, hexagon, cell.getCoordinate());
            drawSeaSerpentStartMarker(g2, hexagon, cell);
            drawBoats(g2, hexagon, cell.getCoordinate());
            drawExplorers(g2, hexagon, cell.getCoordinate());
            drawCreatures(g2, hexagon, cell.getCoordinate());
        }

        g2.dispose();
    }

   

    @Override
    public String getToolTipText(MouseEvent event) {
        return findCellAt(event.getPoint())
                .map(cell -> {
                    HexCoordinate coordinate = cell.getCoordinate();
                    return coordinate + " — " + cell.getDiagnosticLabel()
                            + " | explorateurs sur terre : " + pieceState.getLandExplorersAt(coordinate).size()
                            + " | nageurs : " + pieceState.getSwimmersAt(coordinate).size()
                            + " | bateaux : " + pieceState.getBoatsAt(coordinate).size()
                            + " | créatures : " + gameState.getCreaturesAt(coordinate).size();
                })
                .orElse(null);
    }

    private Optional<HexCell> findCellAt(Point point) {
        for (Map.Entry<Polygon, HexCell> entry : polygonCellMap.entrySet()) {
            if (entry.getKey().contains(point)) return Optional.of(entry.getValue());
        }
        return Optional.empty();
    }

    private Polygon createHexagon(HexCoordinate coordinate) {
        int centerX = MARGIN_X + coordinate.getColumn() * HORIZONTAL_STEP;
        int centerY = MARGIN_Y + coordinate.getRow() * VERTICAL_STEP
                + (coordinate.getColumn() % 2 == 0 ? 0 : VERTICAL_STEP / 2);
        Polygon polygon = new Polygon();
        for (int side = 0; side < 6; side++) {
            double angle = Math.toRadians(60 * side);
            polygon.addPoint(
                    (int) Math.round(centerX + HEX_RADIUS * Math.cos(angle)),
                    (int) Math.round(centerY + HEX_RADIUS * Math.sin(angle))
            );
        }
        return polygon;
    }

    private void drawCellFill(Graphics2D g2, Polygon hexagon, HexCell cell) {
        Color top;
        Color bottom;
        if (cell.isRescueIsland()) {
            top = new Color(255, 232, 178);
            bottom = new Color(235, 198, 124);
        } else if (cell.hasTerrainTile()) {
            TerrainType type = cell.getTerrainTile().orElseThrow().getType();
            switch (type) {
                case BEACH -> {
                    top = new Color(255, 224, 135);
                    bottom = new Color(229, 188, 91);
                }
                case FOREST -> {
                    top = new Color(86, 174, 93);
                    bottom = new Color(47, 127, 65);
                }
                case MOUNTAIN -> {
                    top = new Color(178, 178, 172);
                    bottom = new Color(119, 119, 116);
                }
                default -> {
                    top = Color.GRAY;
                    bottom = Color.DARK_GRAY;
                }
            }
        } else {
            top = new Color(93, 194, 230);
            bottom = new Color(44, 150, 203);
        }

        g2.setPaint(new GradientPaint(
                hexagon.getBounds().x, hexagon.getBounds().y, top,
                hexagon.getBounds().x, hexagon.getBounds().y + hexagon.getBounds().height, bottom
        ));
        g2.fillPolygon(hexagon);
    }

    private void drawCellDecoration(Graphics2D g2, Polygon polygon, HexCell cell) {
        int x = centerX(polygon);
        int y = centerY(polygon);

        if (cell.isRescueIsland()) {
            drawRescueIcon(g2, x, y);
            return;
        }

        if (!cell.hasTerrainTile()) {
            drawSeaDecoration(g2, x, y);
            return;
        }

        TerrainType type = cell.getTerrainTile().orElseThrow().getType();
        switch (type) {
            case BEACH -> drawBeachDecoration(g2, x, y);
            case FOREST -> drawForestDecoration(g2, x, y);
            case MOUNTAIN -> drawMountainDecoration(g2, x, y);
        }
    }

    private void drawSeaDecoration(Graphics2D g2, int x, int y) {
        g2.setColor(new Color(255, 255, 255, 70));
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawArc(x - 16, y - 5, 12, 7, 0, 180);
        g2.drawArc(x - 5,  y - 5, 12, 7, 0, 180);
        g2.drawArc(x + 6,  y - 5, 12, 7, 0, 180);
    }

    private void drawBeachDecoration(Graphics2D g2, int x, int y) {
        g2.setColor(new Color(172, 132, 55, 95));
        g2.fillOval(x - 10, y - 8, 4, 4);
        g2.fillOval(x + 6, y - 2, 4, 4);
        g2.fillOval(x - 2, y + 7, 4, 4);
    }

    private void drawForestDecoration(Graphics2D g2, int x, int y) {
        drawTree(g2, x - 9, y + 5, 0.85);
        drawTree(g2, x + 7, y + 4, 0.85);
        drawTree(g2, x, y - 4, 1.0);
    }

    private void drawTree(Graphics2D g2, int x, int y, double scale) {
        int w = (int) Math.round(10 * scale);
        int h = (int) Math.round(14 * scale);
        Polygon crown = new Polygon(
                new int[] {x, x - w / 2, x + w / 2},
                new int[] {y - h, y, y},
                3
        );
        g2.setColor(new Color(18, 91, 45, 160));
        g2.fillPolygon(crown);
        g2.setColor(new Color(87, 55, 30, 160));
        g2.fillRect(x - 1, y - 1, 3, 5);
    }

    private void drawMountainDecoration(Graphics2D g2, int x, int y) {
        Polygon back = new Polygon(
                new int[] {x - 18, x - 7, x + 4},
                new int[] {y + 10, y - 12, y + 10},
                3
        );
        Polygon front = new Polygon(
                new int[] {x - 4, x + 10, x + 20},
                new int[] {y + 10, y - 14, y + 10},
                3
        );
        g2.setColor(new Color(75, 75, 75, 120));
        g2.fillPolygon(back);
        g2.fillPolygon(front);
        g2.setColor(new Color(240, 240, 240, 180));
        g2.drawLine(x - 7, y - 12, x - 3, y - 3);
        g2.drawLine(x + 10, y - 14, x + 14, y - 3);
    }

    private void drawRescueIcon(Graphics2D g2, int x, int y) {
        g2.setColor(new Color(61, 139, 76, 170));
        g2.fillOval(x - 14, y + 6, 28, 8);
        g2.setColor(new Color(130, 83, 38, 160));
        g2.fillRect(x - 2, y - 8, 4, 16);
        g2.setColor(new Color(42, 122, 70, 180));
        g2.fillArc(x - 16, y - 20, 20, 18, 300, 150);
        g2.fillArc(x - 4, y - 20, 20, 18, 40, 150);
        g2.setColor(new Color(160, 71, 43, 180));
        Polygon flag = new Polygon(
                new int[] {x + 4, x + 17, x + 4},
                new int[] {y - 15, y - 10, y - 5},
                3
        );
        g2.fillPolygon(flag);
    }

    private void drawBorder(Graphics2D g2, Polygon hexagon, HexCell cell) {
        Stroke previous = g2.getStroke();
        if (cell.isInitialIslandSlot()) {
            g2.setStroke(new BasicStroke(3.2f));
            g2.setColor(Color.BLACK);
        } else if (cell.isRescueIsland()) {
            g2.setStroke(new BasicStroke(2.5f));
            g2.setColor(new Color(102, 72, 37));
        } else {
            g2.setStroke(new BasicStroke(1.15f));
            g2.setColor(new Color(44, 64, 74));
        }
        g2.drawPolygon(hexagon);
        g2.setStroke(previous);
    }

    private void drawSelectionHighlight(Graphics2D g2, Polygon hexagon, HexCoordinate coordinate) {
        boolean selected = controller.getSelectedExplorer()
                .flatMap(Explorer::getCoordinate).map(coordinate::equals).orElse(false);
        selected = selected || controller.getSelectedBoat()
                .flatMap(Boat::getCoordinate).map(coordinate::equals).orElse(false);
        selected = selected || controller.getSelectedPassenger()
                .flatMap(Explorer::getBoatId).flatMap(pieceState::findBoatById)
                .flatMap(Boat::getCoordinate).map(coordinate::equals).orElse(false);
        // Sélection créature
        selected = selected || controller.getSelectedCreature()
                .map(Creature::getPosition).map(coordinate::equals).orElse(false);

        if (selected) {
            Stroke previous = g2.getStroke();
            g2.setStroke(new BasicStroke(5.0f));
            g2.setColor(new Color(255, 132, 0));
            g2.drawPolygon(hexagon);
            g2.setStroke(previous);
        }
    }

    private void drawSeaSerpentStartMarker(Graphics2D g2, Polygon polygon, HexCell cell) {
        if (!cell.isSeaSerpentSpawn()) return;
        // Ne montrer le marqueur que si aucun serpent n'est déjà présent
        if (gameState.getCreaturesAt(cell.getCoordinate()).stream()
                .anyMatch(c -> c.getType() == CreatureType.SERPENT)) return;
        int x = centerX(polygon), y = centerY(polygon);
        g2.setColor(new Color(111, 57, 129));
        g2.setStroke(new BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawArc(x - 15, y - 11, 18, 18, 20, 240);
        g2.drawArc(x - 2,  y - 2,  18, 18, 205, 230);
        g2.setStroke(new BasicStroke(1.0f));
    }

    private void drawBoats(Graphics2D g2, Polygon polygon, HexCoordinate coordinate) {
        List<Boat> boats = pieceState.getBoatsAt(coordinate);
        List<Boat> extra = gameState.getExtraBoatsAt(coordinate);
        if (boats.isEmpty() && extra.isEmpty()) return;
        Boat boat = boats.isEmpty() ? extra.get(0) : boats.get(0);

        int x = centerX(polygon);
        int y = centerY(polygon) + 14;

        // Coque
        Polygon hull = new Polygon(
                new int[] {x - 18, x + 18, x + 13, x - 13},
                new int[] {y - 2,  y - 2,  y + 9,  y + 9},
                4
        );
        g2.setColor(new Color(82, 48, 24));
        g2.fillPolygon(hull);
        g2.setColor(Color.BLACK);
        g2.drawPolygon(hull);

        // Mât et voile pour donner un vrai aspect de bateau
        g2.setColor(new Color(60, 39, 23));
        g2.setStroke(new BasicStroke(2f));
        g2.drawLine(x - 2, y - 4, x - 2, y - 20);

        Polygon sail = new Polygon(
                new int[] {x, x + 13, x},
                new int[] {y - 19, y - 10, y - 6},
                3
        );
        g2.setColor(new Color(255, 255, 245));
        g2.fillPolygon(sail);
        g2.setColor(new Color(44, 64, 74));
        g2.drawPolygon(sail);

        // Nombre de passagers
        if (!boat.isEmpty()) {
            drawBadge(g2, x + 14, y - 14, String.valueOf(boat.getPassengers().size()),
                    new Color(255, 255, 255), new Color(40, 40, 40));
        }
    }

    private void drawExplorers(Graphics2D g2, Polygon polygon, HexCoordinate coordinate) {
        List<Explorer> landExplorers = pieceState.getLandExplorersAt(coordinate);
        List<Explorer> swimmers = pieceState.getSwimmersAt(coordinate);

        if (!landExplorers.isEmpty()) {
            drawExplorerMarker(g2, polygon, landExplorers.get(0), -14, 12, false);
            if (landExplorers.size() > 1) {
                drawBadge(g2, centerX(polygon) - 5, centerY(polygon) + 4,
                        String.valueOf(landExplorers.size()), Color.WHITE, Color.BLACK);
            }
        }

        if (!swimmers.isEmpty()) {
            drawExplorerMarker(g2, polygon, swimmers.get(0), 14, 12, true);
            if (swimmers.size() > 1) {
                drawBadge(g2, centerX(polygon) + 31, centerY(polygon) + 4,
                        String.valueOf(swimmers.size()), Color.WHITE, Color.BLACK);
            }
        }
    }

    private void drawExplorerMarker(Graphics2D g2, Polygon polygon, Explorer explorer,
                                     int offsetX, int offsetY, boolean swimmer) {
        int x = centerX(polygon) + offsetX;
        int y = centerY(polygon) + offsetY;

        Color color = explorer.getOwner().getColor().getAwtColor();

        // Petit pion type "meeple" au lieu d'un simple rond
        g2.setColor(color);
        g2.fillOval(x - 6, y - 12, 12, 12);
        g2.fillRoundRect(x - 7, y - 1, 14, 15, 6, 6);
        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(swimmer ? 2.4f : 1.4f));
        g2.drawOval(x - 6, y - 12, 12, 12);
        g2.drawRoundRect(x - 7, y - 1, 14, 15, 6, 6);
        g2.setStroke(new BasicStroke(1.0f));

        if (swimmer) {
            g2.setColor(new Color(16, 77, 120));
            int waveY = y + 18;
            g2.drawArc(x - 8, waveY, 8, 5, 0, 180);
            g2.drawArc(x,      waveY, 8, 5, 0, 180);
        }
    }

    private void drawBadge(Graphics2D g2, int x, int y, String text, Color bg, Color fg) {
        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillOval(x - 7, y - 7, 16, 16);
        g2.setColor(bg);
        g2.fillOval(x - 8, y - 8, 16, 16);
        g2.setColor(fg);
        g2.setFont(new Font("SansSerif", Font.BOLD, 10));
        g2.drawString(text, x - 3, y + 4);
    }

    private int centerX(Polygon polygon) { return polygon.getBounds().x + polygon.getBounds().width  / 2; }
    private int centerY(Polygon polygon) { return polygon.getBounds().y + polygon.getBounds().height / 2; }

    
    private void drawTileRemovalHighlight(Graphics2D g2, Polygon hexagon, HexCell cell) {
        if (controller.getPhase() != DemoPhase.TILE_REMOVAL) return;
        if (!cell.hasTerrainTile()) return;
        if (!canRemoveVisually(cell)) return;
        Stroke previous = g2.getStroke();
        g2.setStroke(new BasicStroke(4.5f));
        g2.setColor(new Color(220, 50, 50, 200));
        g2.drawPolygon(hexagon);
        g2.setStroke(new BasicStroke(2f));
        g2.setColor(new Color(255, 120, 80, 110));
        g2.drawPolygon(hexagon);
        g2.setStroke(previous);
    }

    
    private void drawCreatures(Graphics2D g2, Polygon polygon, HexCoordinate coordinate) {
        List<Creature> creatures = gameState.getCreaturesAt(coordinate);
        if (creatures.isEmpty()) return;
        int cx = centerX(polygon);
        int cy = centerY(polygon) - 12;
        for (int i = 0; i < Math.min(creatures.size(), 3); i++) {
            drawCreatureSymbol(g2, creatures.get(i).getType(), cx - 8 + i * 8, cy + i * 4);
        }
    }

    private void drawCreatureSymbol(Graphics2D g2, CreatureType type, int x, int y) {
        switch (type) {
            case SERPENT -> drawSerpent(g2, x + 7, y + 6);
            case REQUIN -> drawShark(g2, x + 7, y + 6);
            case BALEINE -> drawWhale(g2, x + 7, y + 6);
        }
    }

    private void drawSerpent(Graphics2D g2, int x, int y) {
        g2.setColor(new Color(82, 22, 120));
        g2.setStroke(new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawArc(x - 8, y - 6, 12, 12, 20, 250);
        g2.drawArc(x - 1, y - 1, 13, 13, 200, 245);
        g2.fillOval(x + 6, y - 4, 5, 5);
        g2.setStroke(new BasicStroke(1.0f));
    }

    private void drawShark(Graphics2D g2, int x, int y) {
        g2.setColor(new Color(23, 73, 176));
        g2.fillOval(x - 10, y - 7, 20, 14);
        g2.setColor(new Color(230, 245, 255));
        Polygon fin = new Polygon(
                new int[] {x - 2, x + 4, x + 10},
                new int[] {y - 8, y - 18, y - 7},
                3
        );
        g2.fillPolygon(fin);
        g2.setColor(Color.WHITE);
        g2.fillOval(x + 4, y - 3, 3, 3);
        g2.setColor(new Color(10, 35, 90));
        g2.drawOval(x - 10, y - 7, 20, 14);
    }

    private void drawWhale(Graphics2D g2, int x, int y) {
        g2.setColor(new Color(23, 143, 178));
        g2.fillOval(x - 11, y - 7, 20, 14);
        Polygon tail = new Polygon(
                new int[] {x + 8, x + 17, x + 17},
                new int[] {y, y - 8, y + 8},
                3
        );
        g2.fillPolygon(tail);
        g2.setColor(Color.WHITE);
        g2.fillOval(x - 5, y - 2, 3, 3);
        g2.setColor(new Color(7, 82, 112));
        g2.drawOval(x - 11, y - 7, 20, 14);
        g2.drawPolygon(tail);
    }

   
    private boolean canRemoveVisually(HexCell cell) {
        if (!cell.hasTerrainTile()) return false;
        Board b = pieceState.getBoard();
        boolean adjSea = b.getNeighbors(cell.getCoordinate()).stream()
                .map(b::getCell).anyMatch(HexCell::isSea);
        if (!adjSea) return false;
        TerrainType t = cell.getTerrainTile().orElseThrow().getType();
        if (t == TerrainType.FOREST   && b.countPlacedTerrainTilesOfType(TerrainType.BEACH)  > 0) return false;
        if (t == TerrainType.MOUNTAIN && (b.countPlacedTerrainTilesOfType(TerrainType.BEACH)  > 0
                                       || b.countPlacedTerrainTilesOfType(TerrainType.FOREST) > 0)) return false;
        return true;
    }
}
