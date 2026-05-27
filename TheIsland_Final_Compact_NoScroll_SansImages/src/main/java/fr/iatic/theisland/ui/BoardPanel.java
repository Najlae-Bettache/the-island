package fr.iatic.theisland.ui;

import fr.iatic.theisland.model.Board;
import fr.iatic.theisland.model.Boat;
import fr.iatic.theisland.model.Explorer;
import fr.iatic.theisland.model.HexCell;
import fr.iatic.theisland.model.HexCoordinate;
import fr.iatic.theisland.model.PieceState;
import fr.iatic.theisland.model.TerrainType;

import javax.swing.JPanel;
import javax.swing.ToolTipManager;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Affichage Swing du plateau et des pions de démonstration.
 * Cette version retire les lettres visibles sur les cases.
 */
public final class BoardPanel extends JPanel {

    private static final int HEX_RADIUS = 33;
    private static final int HEX_HEIGHT = (int) Math.round(Math.sqrt(3) * HEX_RADIUS);
    private static final int HORIZONTAL_STEP = (int) Math.round(1.5 * HEX_RADIUS);
    private static final int VERTICAL_STEP = HEX_HEIGHT;
    private static final int MARGIN_X = 64;
    private static final int MARGIN_Y = 62;

    private final Board board;
    private final PieceState pieceState;
    private final Map<Polygon, HexCell> polygonCellMap = new HashMap<>();

    public BoardPanel(Board board, PieceState pieceState) {
        this.board = board;
        this.pieceState = pieceState;
        setPreferredSize(new Dimension(1040, 980));
        ToolTipManager.sharedInstance().registerComponent(this);
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

            g2.setColor(getFillColor(cell));
            g2.fillPolygon(hexagon);

            drawBorder(g2, hexagon, cell);
            drawBoats(g2, hexagon, cell.getCoordinate());
            drawExplorers(g2, hexagon, cell.getCoordinate());
        }

        g2.dispose();
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        Point point = event.getPoint();
        for (Map.Entry<Polygon, HexCell> entry : polygonCellMap.entrySet()) {
            if (entry.getKey().contains(point)) {
                HexCell cell = entry.getValue();
                HexCoordinate coordinate = cell.getCoordinate();
                int landExplorers = pieceState.getLandExplorersAt(coordinate).size();
                int swimmers = pieceState.getSwimmersAt(coordinate).size();
                int boats = pieceState.getBoatsAt(coordinate).size();

                return coordinate + " — " + cell.getDiagnosticLabel()
                        + " | explorateurs sur terre : " + landExplorers
                        + " | nageurs : " + swimmers
                        + " | bateaux : " + boats;
            }
        }
        return null;
    }

    private Polygon createHexagon(HexCoordinate coordinate) {
        int centerX = MARGIN_X + coordinate.getColumn() * HORIZONTAL_STEP;
        int centerY = MARGIN_Y + coordinate.getRow() * VERTICAL_STEP
                + (coordinate.getColumn() % 2 == 0 ? 0 : VERTICAL_STEP / 2);

        Polygon polygon = new Polygon();
        for (int side = 0; side < 6; side++) {
            double angle = Math.toRadians(60 * side);
            int x = (int) Math.round(centerX + HEX_RADIUS * Math.cos(angle));
            int y = (int) Math.round(centerY + HEX_RADIUS * Math.sin(angle));
            polygon.addPoint(x, y);
        }
        return polygon;
    }

    private Color getFillColor(HexCell cell) {
        if (cell.isRescueIsland()) {
            return new Color(244, 224, 168);
        }

        if (!cell.hasTerrainTile()) {
            return new Color(97, 176, 220);
        }

        TerrainType type = cell.getTerrainTile().orElseThrow().getType();
        return switch (type) {
            case BEACH -> new Color(230, 205, 129);
            case FOREST -> new Color(83, 155, 86);
            case MOUNTAIN -> new Color(155, 155, 155);
        };
    }

    private void drawBorder(Graphics2D g2, Polygon hexagon, HexCell cell) {
        Stroke previousStroke = g2.getStroke();

        if (cell.isInitialIslandSlot()) {
            g2.setStroke(new BasicStroke(3.0f));
            g2.setColor(Color.BLACK);
        } else if (cell.isRescueIsland()) {
            g2.setStroke(new BasicStroke(2.2f));
            g2.setColor(new Color(92, 66, 32));
        } else {
            g2.setStroke(new BasicStroke(1.1f));
            g2.setColor(Color.DARK_GRAY);
        }

        g2.drawPolygon(hexagon);
        g2.setStroke(previousStroke);
    }

    private void drawBoats(Graphics2D g2, Polygon polygon, HexCoordinate coordinate) {
        List<Boat> boats = pieceState.getBoatsAt(coordinate);
        if (boats.isEmpty()) {
            return;
        }

        int centerX = polygon.getBounds().x + polygon.getBounds().width / 2;
        int centerY = polygon.getBounds().y + polygon.getBounds().height / 2 + 6;

        int width = 28;
        int height = 13;
        g2.setColor(new Color(125, 82, 42));
        g2.fillRoundRect(centerX - width / 2, centerY - height / 2, width, height, 7, 7);
        g2.setColor(Color.BLACK);
        g2.drawRoundRect(centerX - width / 2, centerY - height / 2, width, height, 7, 7);
    }

    private void drawExplorers(Graphics2D g2, Polygon polygon, HexCoordinate coordinate) {
        List<Explorer> landExplorers = pieceState.getLandExplorersAt(coordinate);
        List<Explorer> swimmers = pieceState.getSwimmersAt(coordinate);

        if (!landExplorers.isEmpty()) {
            Explorer explorer = landExplorers.get(0);
            drawExplorerMarker(g2, polygon, explorer, -15, 10, false);
        }

        if (!swimmers.isEmpty()) {
            Explorer swimmer = swimmers.get(0);
            drawExplorerMarker(g2, polygon, swimmer, 15, 10, true);
        }
    }

    private void drawExplorerMarker(
            Graphics2D g2,
            Polygon polygon,
            Explorer explorer,
            int offsetX,
            int offsetY,
            boolean swimmer
    ) {
        int centerX = polygon.getBounds().x + polygon.getBounds().width / 2 + offsetX;
        int centerY = polygon.getBounds().y + polygon.getBounds().height / 2 + offsetY;
        int diameter = 17;

        g2.setColor(explorer.getOwner().getColor().getAwtColor());
        g2.fillOval(centerX - diameter / 2, centerY - diameter / 2, diameter, diameter);

        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(swimmer ? 2.0f : 1.2f));
        g2.drawOval(centerX - diameter / 2, centerY - diameter / 2, diameter, diameter);
        g2.setStroke(new BasicStroke(1.0f));

        if (swimmer) {
            int waveY = centerY + diameter / 2 + 2;
            g2.drawArc(centerX - 6, waveY, 6, 4, 0, 180);
            g2.drawArc(centerX, waveY, 6, 4, 0, 180);
        }
    }
}
