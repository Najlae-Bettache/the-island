package fr.iatic.theisland.setup;

import fr.iatic.theisland.model.TerrainTile;
import fr.iatic.theisland.model.TerrainType;
import fr.iatic.theisland.model.TileEffect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;


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

        
        if (volcanoRank >= mountainIndices.size() - 2) {
            Random random = new Random();
            int targetRank = random.nextInt(Math.max(1, mountainIndices.size() / 2));
            Collections.swap(tiles, volcanoIndex, mountainIndices.get(targetRank));
        }
    }

   
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

       
        int[] preferred = {0, 1, 2, 3, 4, 5, 7, 8, 10, 12, 15, 18, 21, 24, 27, 31, 34, 37, 38, 39};

        for (int position : preferred) {
            if (position < tiles.size()
                    && tiles.get(position).getType() == TerrainType.MOUNTAIN
                    && position != volcanoIndex) {
                Collections.swap(tiles, volcanoIndex, position);
                return tiles;
            }
        }

        
        int target = mountainPositions.get(Math.max(0, mountainPositions.size() / 3));
        if (target != volcanoIndex) {
            Collections.swap(tiles, volcanoIndex, target);
        }
        return tiles;
    }


   
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
