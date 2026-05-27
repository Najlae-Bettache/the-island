package fr.iatic.theisland.setup;

import fr.iatic.theisland.model.TerrainTile;
import fr.iatic.theisland.model.TerrainType;
import fr.iatic.theisland.model.TileEffect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Fabrique les 40 tuiles de terrain avec effets cachés distribués selon les règles du jeu.
 */
public final class TerrainTileFactory {

    private TerrainTileFactory() { }

    public static List<TerrainTile> createShuffledTerrainTiles() {
        List<TerrainTile> tiles = new ArrayList<>(40);
        tiles.addAll(beachTiles());
        tiles.addAll(forestTiles());
        tiles.addAll(mountainTiles());

        Collections.shuffle(tiles);
        avoidVolcanoAsVeryLastMountain(tiles);

        return tiles;
    }

    private static void avoidVolcanoAsVeryLastMountain(List<TerrainTile> tiles) {
        List<Integer> mountainIndices = new ArrayList<>();
        int volcanoIndex = -1;

        for (int i = 0; i < tiles.size(); i++) {
            TerrainTile tile = tiles.get(i);
            if (tile.getType() == TerrainType.MOUNTAIN) {
                mountainIndices.add(i);
                if (tile.getHiddenEffect() == TileEffect.VOLCANO) {
                    volcanoIndex = i;
                }
            }
        }

        if (mountainIndices.isEmpty() || volcanoIndex < 0) {
            return;
        }

        int volcanoRank = mountainIndices.indexOf(volcanoIndex);

        // Conserve l'aléatoire, mais évite une démo frustrante où le volcan est
        // presque toujours découvert tout à la fin de la phase montagne.
        if (volcanoRank >= mountainIndices.size() - 2) {
            Random random = new Random();
            int targetRank = random.nextInt(Math.max(1, mountainIndices.size() / 2));
            Collections.swap(tiles, volcanoIndex, mountainIndices.get(targetRank));
        }
    }

    /**
     * Variante de mélange pour la version graphique : le volcan reste aléatoire,
     * mais il n'est pas placé dans une montagne trop centrale/inaccessible.
     * Cela évite qu'il apparaisse presque toujours comme toute dernière tuile.
     */
    public static List<TerrainTile> createShuffledTerrainTilesWithEarlyVolcano() {
        List<TerrainTile> tiles = createShuffledTerrainTiles();

        int volcanoIndex = -1;
        List<Integer> mountainPositions = new ArrayList<>();

        for (int i = 0; i < tiles.size(); i++) {
            TerrainTile tile = tiles.get(i);
            if (tile.getType() == TerrainType.MOUNTAIN) {
                mountainPositions.add(i);
                if (tile.getHiddenEffect() == TileEffect.VOLCANO) {
                    volcanoIndex = i;
                }
            }
        }

        if (volcanoIndex < 0 || mountainPositions.isEmpty()) {
            return tiles;
        }

        // Positions de la zone centrale qui sont plus souvent accessibles au début de la phase montagne.
        // Le volcan reste caché sous une montagne, mais il ne bloque pas toujours la fin du test.
        int[] preferred = {0, 1, 2, 3, 4, 5, 7, 8, 10, 12, 15, 18, 21, 24, 27, 31, 34, 37, 38, 39};

        for (int position : preferred) {
            if (position < tiles.size()
                    && tiles.get(position).getType() == TerrainType.MOUNTAIN
                    && position != volcanoIndex) {
                Collections.swap(tiles, volcanoIndex, position);
                return tiles;
            }
        }

        // Sécurité : si aucune position préférée n'est une montagne, on place le volcan
        // dans la première moitié des montagnes.
        int target = mountainPositions.get(Math.max(0, mountainPositions.size() / 3));
        if (target != volcanoIndex) {
            Collections.swap(tiles, volcanoIndex, target);
        }
        return tiles;
    }


    /** 16 tuiles Plage. */
    private static List<TerrainTile> beachTiles() {
        List<TerrainTile> t = new ArrayList<>(16);
        add(t, TerrainType.BEACH, TileEffect.SHARK_APPEARS, 3);
        add(t, TerrainType.BEACH, TileEffect.WHALE_APPEARS, 3);
        add(t, TerrainType.BEACH, TileEffect.BOAT_APPEARS,  2);
        add(t, TerrainType.BEACH, TileEffect.DOLPHIN,       3);
        add(t, TerrainType.BEACH, TileEffect.MOVE_SERPENT,  1);
        add(t, TerrainType.BEACH, TileEffect.CANCEL_SHARK,  1);
        add(t, TerrainType.BEACH, TileEffect.CANCEL_WHALE,  1);
        add(t, TerrainType.BEACH, TileEffect.WHIRLPOOL,     1);
        add(t, TerrainType.BEACH, TileEffect.NONE,          1);
        return t;
    }

    /** 16 tuiles Forêt. */
    private static List<TerrainTile> forestTiles() {
        List<TerrainTile> t = new ArrayList<>(16);
        add(t, TerrainType.FOREST, TileEffect.SHARK_APPEARS, 2);
        add(t, TerrainType.FOREST, TileEffect.WHALE_APPEARS, 2);
        add(t, TerrainType.FOREST, TileEffect.BOAT_APPEARS,  3);
        add(t, TerrainType.FOREST, TileEffect.DOLPHIN,       1);
        add(t, TerrainType.FOREST, TileEffect.WIND,          1);
        add(t, TerrainType.FOREST, TileEffect.MOVE_SHARK,    1);
        add(t, TerrainType.FOREST, TileEffect.MOVE_WHALE,    1);
        add(t, TerrainType.FOREST, TileEffect.CANCEL_SHARK,  1);
        add(t, TerrainType.FOREST, TileEffect.CANCEL_WHALE,  1);
        add(t, TerrainType.FOREST, TileEffect.WHIRLPOOL,     1);
        add(t, TerrainType.FOREST, TileEffect.NONE,          2);
        return t;
    }

    /** 8 tuiles Montagne. */
    private static List<TerrainTile> mountainTiles() {
        List<TerrainTile> t = new ArrayList<>(8);
        add(t, TerrainType.MOUNTAIN, TileEffect.SHARK_APPEARS, 1);
        add(t, TerrainType.MOUNTAIN, TileEffect.WHIRLPOOL,     2);
        add(t, TerrainType.MOUNTAIN, TileEffect.VOLCANO,       1);
        add(t, TerrainType.MOUNTAIN, TileEffect.CANCEL_SHARK,  1);
        add(t, TerrainType.MOUNTAIN, TileEffect.CANCEL_WHALE,  1);
        add(t, TerrainType.MOUNTAIN, TileEffect.NONE,          2);
        return t;
    }

    private static void add(List<TerrainTile> list, TerrainType type, TileEffect effect, int n) {
        for (int i = 0; i < n; i++) list.add(new TerrainTile(type, effect));
    }
}
