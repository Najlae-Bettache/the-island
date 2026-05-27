package fr.iatic.theisland.model;

/**
 * Types de tuiles de terrain.
 */
public enum TerrainType {
    BEACH("Plage"),
    FOREST("Forêt"),
    MOUNTAIN("Montagne");

    private final String label;

    TerrainType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
