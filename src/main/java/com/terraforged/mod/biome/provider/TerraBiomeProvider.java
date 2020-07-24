/*
 *
 * MIT License
 *
 * Copyright (c) 2020 TerraForged
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.terraforged.mod.biome.provider;

import com.google.common.collect.Sets;
import com.terraforged.core.cell.Cell;
import com.terraforged.core.concurrent.Resource;
import com.terraforged.mod.biome.map.BiomeMap;
import com.terraforged.mod.biome.modifier.BiomeModifierManager;
import com.terraforged.mod.chunk.TerraContext;
import com.terraforged.mod.util.setup.SetupHooks;
import com.terraforged.world.heightmap.WorldLookup;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeManager;
import net.minecraft.world.biome.ColumnFuzzedBiomeMagnifier;
import net.minecraft.world.biome.provider.BiomeProvider;

import java.util.List;
import java.util.Random;
import java.util.Set;

public class TerraBiomeProvider extends BiomeProvider {

    private final long seed;
    private final BiomeMap biomeMap;
    private final TerraContext context;
    private final WorldLookup worldLookup;
    private final BiomeModifierManager modifierManager;

    public TerraBiomeProvider(TerraContext context) {
        super(BiomeHelper.getAllBiomes());
        this.context = context;
        this.seed = context.terraSettings.world.seed;
        this.biomeMap = BiomeHelper.createBiomeMap();
        this.worldLookup = new WorldLookup(context.factory, context);
        this.modifierManager = SetupHooks.setup(new BiomeModifierManager(context, biomeMap), context.copy());
    }

    public Resource<Cell> lookupPos(int x, int z) {
        return getWorldLookup().getCell(x, z);
    }

    public Biome getBiome(int x, int z) {
        try (Resource<Cell> resource = getWorldLookup().getCell(x, z, true)) {
            return getBiome(resource.get(), x, z);
        }
    }

    @Override
    public Biome getNoiseBiome(int x, int y, int z) {
        x = (x << 2);
        z = (z << 2);
        try (Resource<Cell> cell = lookupPos(x, z)) {
            return getBiome(cell.get(), x, z);
        }
    }

    @Override
    public Set<Biome> getBiomes(int centerX, int centerY, int centerZ, int radius) {
        int minX = centerX - radius >> 2;
        int minZ = centerZ - radius >> 2;
        int maxX = centerX + radius >> 2;
        int maxZ = centerZ + radius >> 2;
        int rangeX = maxX - minX + 1;
        int rangeZ = maxZ - minZ + 1;
        Set<Biome> set = Sets.newHashSet();
        Cell cell = new Cell();
        for(int dz = 0; dz < rangeZ; ++dz) {
            for(int dx = 0; dx < rangeX; ++dx) {
                int x = (minX + dx) << 2;
                int z = (minZ + dz) << 2;
                worldLookup.applyCell(cell, x, z);
                Biome biome = getBiome(cell, x, z);
                set.add(biome);
            }
        }

        return set;
    }

    @Override
    public BlockPos func_225531_a_(int centerX, int centerY, int centerZ, int range, List<Biome> biomes, Random random) {
        int minX = centerX - range >> 2;
        int minZ = centerZ - range >> 2;
        int maxX = centerX + range >> 2;
        int maxZ = centerZ + range >> 2;
        int rangeX = maxX - minX + 1;
        int rangeZ = maxZ - minZ + 1;
        int y = centerY >> 2;
        BlockPos blockpos = null;
        int attempts = 0;

        Cell cell = new Cell();
        for(int dz = 0; dz < rangeZ; ++dz) {
            for(int dx = 0; dx < rangeX; ++dx) {
                int x = (minX + dx) << 2;
                int z = (minZ + dz) << 2;
                worldLookup.applyCell(cell, x, z);
                if (biomes.contains(getBiome(cell, x, z))) {
                    if (blockpos == null || random.nextInt(attempts + 1) == 0) {
                        blockpos = new BlockPos(x, y, z);
                    }
                    ++attempts;
                }
            }
        }
        return blockpos;
    }

    public Biome getSurfaceBiome(int x, int z, BiomeManager.IBiomeReader reader) {
        return ColumnFuzzedBiomeMagnifier.INSTANCE.getBiome(seed, x, 0, z, reader);
    }

    public WorldLookup getWorldLookup() {
        return worldLookup;
    }

    public TerraContext getContext() {
        return context;
    }

    public BiomeModifierManager getModifierManager() {
        return modifierManager;
    }

    public Biome getBiome(Cell cell, int x, int z) {
        Biome biome = biomeMap.provideBiome(cell, context.levels);
        if (modifierManager.hasModifiers(cell, context.levels)) {
            return modifierManager.modify(biome, cell, x, z);
        }
        return biome;
    }

    public boolean canSpawnAt(Cell cell) {
        return cell.terrain != context.terrain.ocean && cell.terrain != context.terrain.deepOcean;
    }
}
