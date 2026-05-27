package fr.iatic.theisland.model;

/**
 * Effet caché d'une tuile de terrain, révélé lors du retrait.
 * Vert = immédiat, Rouge = à jouer en main.
 */
public enum TileEffect {
    NONE,
    // Contour vert – immédiats
    SHARK_APPEARS,
    WHALE_APPEARS,
    BOAT_APPEARS,
    WHIRLPOOL,
    VOLCANO,
    // Contour rouge – conservés en main
    DOLPHIN,
    WIND,
    MOVE_SERPENT,
    MOVE_SHARK,
    MOVE_WHALE,
    CANCEL_SHARK,
    CANCEL_WHALE;

    public boolean isImmediate() {
        return this == SHARK_APPEARS || this == WHALE_APPEARS || this == BOAT_APPEARS
                || this == WHIRLPOOL || this == VOLCANO;
    }

    public boolean isHandEffect() {
        return this == DOLPHIN || this == WIND || this == MOVE_SERPENT
                || this == MOVE_SHARK || this == MOVE_WHALE
                || this == CANCEL_SHARK || this == CANCEL_WHALE;
    }
}
