package fr.iatic.theisland.model;

/**
 * Type permanent de la case lorsque aucune tuile de terrain n'est posée.
 */
public enum BaseCellType {
    SEA("Mer"),
    RESCUE_ISLAND("Abri");

    private final String label;

    BaseCellType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
