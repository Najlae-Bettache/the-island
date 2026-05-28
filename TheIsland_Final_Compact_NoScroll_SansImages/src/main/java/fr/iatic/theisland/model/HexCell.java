package fr.iatic.theisland.model;

import java.util.Objects;
import java.util.Optional;

/**
 * Une case du plateau.
 */
public final class HexCell {

    private final HexCoordinate coordinate;
    private final BaseCellType baseCellType;
    private final boolean initialIslandSlot;
    private final boolean seaSerpentSpawn;
    private TerrainTile terrainTile;

    public HexCell(
            HexCoordinate coordinate,
            BaseCellType baseCellType,
            boolean initialIslandSlot,
            boolean seaSerpentSpawn
    ) {
        this.coordinate = Objects.requireNonNull(coordinate, "La coordonnée est obligatoire.");
        this.baseCellType = Objects.requireNonNull(baseCellType, "Le type de base est obligatoire.");
        this.initialIslandSlot = initialIslandSlot;
        this.seaSerpentSpawn = seaSerpentSpawn;

        if (baseCellType == BaseCellType.RESCUE_ISLAND && initialIslandSlot) {
            throw new IllegalArgumentException("Une case d'abri ne peut pas être un emplacement de tuile.");
        }
        if (baseCellType == BaseCellType.RESCUE_ISLAND && seaSerpentSpawn) {
            throw new IllegalArgumentException("Un serpent ne peut pas commencer sur un abri.");
        }
    }

    public HexCoordinate getCoordinate() {
        return coordinate;
    }

    public BaseCellType getBaseCellType() {
        return baseCellType;
    }

    public boolean isInitialIslandSlot() {
        return initialIslandSlot;
    }

    public boolean isSeaSerpentSpawn() {
        return seaSerpentSpawn;
    }

    public boolean isRescueIsland() {
        return baseCellType == BaseCellType.RESCUE_ISLAND;
    }

    public boolean hasTerrainTile() {
        return terrainTile != null;
    }

    public boolean isSea() {
        return baseCellType == BaseCellType.SEA && terrainTile == null;
    }

    public Optional<TerrainTile> getTerrainTile() {
        return Optional.ofNullable(terrainTile);
    }

    public void placeTerrainTile(TerrainTile tile) {
        Objects.requireNonNull(tile, "La tuile est obligatoire.");
        if (!initialIslandSlot) {
            throw new IllegalStateException("Cette case n'est pas un emplacement initial de l'île.");
        }
        if (isRescueIsland()) {
            throw new IllegalStateException("Impossible de placer une tuile sur un abri.");
        }
        if (terrainTile != null) {
            throw new IllegalStateException("Cette case contient déjà une tuile.");
        }
        terrainTile = tile;
    }

    public TerrainTile removeTerrainTile() {
        if (terrainTile == null) {
            throw new IllegalStateException("Aucune tuile à retirer sur cette case.");
        }
        TerrainTile removed = terrainTile;
        terrainTile = null;
        return removed;
    }

    public String getDiagnosticLabel() {
        if (hasTerrainTile()) {
            return terrainTile.getType().getLabel();
        }
        if (isRescueIsland()) {
            return "Abri";
        }
        if (isSeaSerpentSpawn()) {
            return "Mer — départ serpent";
        }
        return "Mer";
    }
}
