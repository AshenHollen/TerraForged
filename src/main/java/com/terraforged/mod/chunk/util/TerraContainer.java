package com.terraforged.mod.chunk.util;

import com.terraforged.core.cell.Cell;
import com.terraforged.core.tile.chunk.ChunkReader;
import com.terraforged.core.util.PosIterator;
import com.terraforged.fm.GameContext;
import com.terraforged.mod.biome.provider.TerraBiomeProvider;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeContainer;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.chunk.IChunk;

public class TerraContainer extends BiomeContainer {

    private static final int BITS_WIDTH = (int) Math.round(Math.log(16.0D) / Math.log(2.0D)) - 2;
    private static final int ZOOM_VERT = (int) Math.round(Math.log(256.0D) / Math.log(2.0D)) - 2;
    public static final int BIOMES_3D_SIZE = 1 << BITS_WIDTH + BITS_WIDTH + ZOOM_VERT;
    public static final int BIOMES_2D_SIZE = 16 * 16;
    public static final int MASK_HORIZ = (1 << BITS_WIDTH) - 1;
    public static final int MASK_VERT = (1 << ZOOM_VERT) - 1;

    private final Biome[] biomes;
    private final Biome[] surface;

    public TerraContainer(Biome[] biomes, Biome[] surface, GameContext context) {
        super(context.biomes.getRegistry(), biomes);
        this.biomes = biomes;
        this.surface = surface;
    }

    public Biome getBiome(int x, int z) {
        x &= 15;
        z &= 15;
        return surface[z * 16 + x];
    }

    public Biome getFeatureBiome(ChunkReader chunkReader) {
        PosIterator iterator = PosIterator.area(0, 0, 16, 16);
        while (iterator.next()) {
            Cell cell = chunkReader.getCell(iterator.x(), iterator.z());
            if (cell.biomeType.isExtreme()) {
                return getBiome(iterator.x(), iterator.z());
            }
        }
        return getBiome(8, 8);
    }

    public BiomeContainer bakeBiomes(boolean convertToVanilla, GameContext context) {
        if (convertToVanilla) {
            Biome[] biomeArray = new Biome[biomes.length];
            for (int i = 0; i < biomes.length; i++) {
                Biome biome = biomes[i];
                biome = context.biomes.getRemap(biome);
                biomeArray[i] = biome;
            }
            return new BiomeContainer(context.biomes.getRegistry(), biomeArray);
        }
        return new BiomeContainer(context.biomes.getRegistry(), biomes);
    }

    public static TerraContainer getOrCreate(IChunk chunk, ChunkReader reader, TerraBiomeProvider biomeProvider) {
        BiomeContainer biomes = chunk.getBiomes();
        if (biomes instanceof TerraContainer) {
            return (TerraContainer) biomes;
        }

        TerraContainer container = TerraContainer.create(reader, biomeProvider);

        // replace/set the primer's biomes
        ((ChunkPrimer) chunk).setBiomes(container);

        return container;
    }

    public static TerraContainer create(ChunkReader chunkReader, TerraBiomeProvider biomeProvider) {
        Biome[] biomes2D = new Biome[BIOMES_2D_SIZE];
        Biome[] biomes3D = new Biome[BIOMES_3D_SIZE];
        PosIterator iterator = PosIterator.area(0, 0, 16, 16);
        while (iterator.next()) {
            int dx = iterator.x();
            int dz = iterator.z();
            int x = chunkReader.getBlockX() + dx;
            int z = chunkReader.getBlockZ() + dz;
            Biome biome = biomeProvider.getBiome(chunkReader.getCell(dx, dz), x, z);
            biomes2D[indexOf(dx, dz)] = biome;
            if ((dx & 3) == 0 && (dz & 3) == 0) {
                for (int dy = 0; dy < 64; dy++) {
                    biomes3D[indexOf(dx >> 2, dy, dz >> 2)] = biome;
                }
            }
        }
        return new TerraContainer(biomes3D, biomes2D, biomeProvider.getContext().gameContext);
    }

    private static int indexOf(int x, int z) {
        return (z << 4) + x;
    }

    public static int indexOf(int x, int y, int z) {
        x &= MASK_HORIZ;
        y = MathHelper.clamp(y, 0, MASK_VERT);
        z &= MASK_HORIZ;
        return y << BITS_WIDTH + BITS_WIDTH | z << BITS_WIDTH | x;
    }
}