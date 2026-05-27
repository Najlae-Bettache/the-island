package fr.iatic.theisland.model;

import java.awt.Color;

/**
 * Couleurs disponibles pour les joueurs.
 */
public enum PlayerColor {
    RED("Rouge", new Color(208, 67, 67)),
    BLUE("Bleu", new Color(62, 114, 201)),
    GREEN("Vert", new Color(68, 158, 92)),
    YELLOW("Jaune", new Color(218, 179, 54));

    private final String label;
    private final Color awtColor;

    PlayerColor(String label, Color awtColor) {
        this.label = label;
        this.awtColor = awtColor;
    }

    public String getLabel() {
        return label;
    }

    public Color getAwtColor() {
        return awtColor;
    }
}
