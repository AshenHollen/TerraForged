package com.terraforged.mod.biome.map.set;

import com.google.gson.JsonElement;
import com.terraforged.core.cell.Cell;
import com.terraforged.mod.biome.map.defaults.DefaultBiome;
import com.terraforged.mod.biome.provider.BiomeHelper;
import com.terraforged.mod.util.ListUtils;
import com.terraforged.n2d.util.NoiseUtil;
import net.minecraft.world.biome.Biome;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public abstract class BiomeSet {

    private static final Biome[] EMPTY = new Biome[0];

    protected final Biome[][] biomes;
    protected final DefaultBiome defaultBiome;

    public BiomeSet(Biome[][] biomes, DefaultBiome defaultBiome) {
        this.biomes = biomes;
        this.defaultBiome = defaultBiome;
    }

    public int getSize(int index) {
        return biomes[index].length;
    }

    public int getSize(Cell cell) {
        return biomes[getIndex(cell)].length;
    }

    public Biome[] getSet(int index) {
        return biomes[index];
    }

    public Biome[] getSet(Cell cell) {
        return biomes[getIndex(cell)];
    }

    public Biome getBiome(Cell cell) {
        Biome[] set = biomes[getIndex(cell)];
        if (set.length == 0) {
            return defaultBiome.getDefaultBiome(cell);
        }

        int maxIndex = set.length - 1;
        int index = NoiseUtil.round(maxIndex * cell.biomeIdentity);

        // shouldn't happen but safety check the bounds
        if (index < 0 || index >= set.length) {
            return defaultBiome.getDefaultBiome(cell);
        }

        return set[index];
    }

    public abstract int getIndex(Cell cell);

    public abstract JsonElement toJson();

    protected static Biome[][] collect(Map<? extends Enum<?>, List<Biome>> map, int size, Function<Enum<?>, Integer> indexer) {
        Biome[][] biomes = new Biome[size][];
        for (Enum<?> type : map.keySet()) {
            int index = indexer.apply(type);
            if (index < 0 || index >= size) {
                continue;
            }
            List<Biome> list = map.getOrDefault(type, Collections.emptyList());
            list = ListUtils.minimize(list);
            list.sort(Comparator.comparing(BiomeHelper::getId));
            biomes[index] = list.toArray(new Biome[0]);
        }
        for (int i = 0; i < size; i++) {
            if (biomes[i] == null) {
                biomes[i] = EMPTY;
            }
        }
        return biomes;
    }
}
