package fr.iatic.theisland.model;

import java.util.Objects;

/**
 * Tuile de terrain placée sur l'île.
 * Chaque tuile possède un effet caché révélé lors du retrait (mission 3).
 */
public final class TerrainTile {

    private final TerrainType type;
    private final TileEffect hiddenEffect;

    /** Constructeur complet (missions 3 et 4). */
    public TerrainTile(TerrainType type, TileEffect hiddenEffect) {
        this.type = Objects.requireNonNull(type, "Le type de terrain est obligatoire.");
        this.hiddenEffect = Objects.requireNonNull(hiddenEffect, "L'effet caché est obligatoire.");
    }

    /** Constructeur de compatibilité (missions 1 et 2) : effet NONE. */
    public TerrainTile(TerrainType type) {
        this(type, TileEffect.NONE);
    }

    public TerrainType getType() {
        return type;
    }

    public TileEffect getHiddenEffect() {
        return hiddenEffect;
    }
}
