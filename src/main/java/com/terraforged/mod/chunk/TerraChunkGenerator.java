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

package com.terraforged.mod.chunk;

import com.terraforged.api.biome.surface.SurfaceManager;
import com.terraforged.api.chunk.column.ColumnDecorator;
import com.terraforged.api.material.layer.LayerManager;
import com.terraforged.core.cell.Cell;
import com.terraforged.core.tile.Size;
import com.terraforged.core.tile.Tile;
import com.terraforged.core.tile.chunk.ChunkReader;
import com.terraforged.core.tile.gen.TileCache;
import com.terraforged.fm.FeatureManager;
import com.terraforged.fm.data.DataManager;
import com.terraforged.fm.structure.StructureManager;
import com.terraforged.mod.biome.provider.TerraBiomeProvider;
import com.terraforged.mod.chunk.generator.BiomeGenerator;
import com.terraforged.mod.chunk.generator.FeatureGenerator;
import com.terraforged.mod.chunk.generator.Generator;
import com.terraforged.mod.chunk.generator.MobGenerator;
import com.terraforged.mod.chunk.generator.StructureGenerator;
import com.terraforged.mod.chunk.generator.SurfaceGenerator;
import com.terraforged.mod.chunk.generator.TerrainCarver;
import com.terraforged.mod.chunk.generator.TerrainGenerator;
import com.terraforged.mod.feature.BlockDataManager;
import com.terraforged.mod.material.Materials;
import com.terraforged.mod.material.geology.GeoManager;
import com.terraforged.mod.util.setup.SetupHooks;
import net.minecraft.entity.EntityClassification;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeManager;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.GenerationSettings;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.gen.WorldGenRegion;
import net.minecraft.world.gen.feature.template.TemplateManager;
import net.minecraft.world.server.ServerWorld;

import java.util.List;

public class TerraChunkGenerator extends ChunkGenerator<GenerationSettings> {

    private final TerraContext context;
    private final TerraBiomeProvider biomeProvider;

    private final Generator.Mobs mobGenerator;
    private final Generator.Biomes biomeGenerator;
    private final Generator.Carvers terrainCarver;
    private final Generator.Terrain terrainGenerator;
    private final Generator.Surfaces surfaceGenerator;
    private final Generator.Features featureGenerator;
    private final Generator.Structures structureGenerator;

    private final GeoManager geologyManager;
    private final FeatureManager featureManager;
    private final StructureManager structureManager;
    private final SurfaceManager surfaceManager;
    private final BlockDataManager blockDataManager;
    private final List<ColumnDecorator> baseDecorators;
    private final List<ColumnDecorator> postProcessors;

    private final TileCache tileCache;

    public TerraChunkGenerator(TerraContext context, TerraBiomeProvider biomeProvider, GenerationSettings settings) {
        super(context.world, biomeProvider, settings);
        this.context = context;
        this.biomeProvider = biomeProvider;
        this.mobGenerator = new MobGenerator(this);
        this.biomeGenerator = new BiomeGenerator(this);
        this.terrainCarver = new TerrainCarver(this);
        this.terrainGenerator = new TerrainGenerator(this);
        this.surfaceGenerator = new SurfaceGenerator(this);
        this.featureGenerator = new FeatureGenerator(this);
        this.structureGenerator = new StructureGenerator(this);

        this.surfaceManager = TerraSetupFactory.createSurfaceManager(context);
        this.structureManager = TerraSetupFactory.createStructureManager(context);
        this.geologyManager = TerraSetupFactory.createGeologyManager(context);
        this.baseDecorators = TerraSetupFactory.createBaseDecorators(geologyManager, context);
        this.postProcessors = TerraSetupFactory.createFeatureDecorators(context);
        this.tileCache = context.cache;

        try (DataManager data = TerraSetupFactory.createDataManager()) {
            FeatureManager.initData(data);
            this.featureManager = TerraSetupFactory.createFeatureManager(data, context);
            this.blockDataManager = TerraSetupFactory.createBlockDataManager(data, context);
            FeatureManager.clearData();
        }

        SetupHooks.setup(getLayerManager(), context.copy());
        SetupHooks.setup(baseDecorators, postProcessors, context.copy());
    }

    @Override
    public final void generateStructures(BiomeManager biomes, IChunk chunk, ChunkGenerator<?> generator, TemplateManager templates) {
        structureGenerator.generateStructureStarts(biomes, chunk, templates);
    }

    @Override
    public final void generateStructureStarts(IWorld world, IChunk chunk) {
        structureGenerator.generateStructureReferences(world, chunk);
    }

    @Override
    public final void generateBiomes(IChunk chunk) {
        biomeGenerator.generateBiomes(chunk);
    }

    @Override
    public final void makeBase(IWorld world, IChunk chunk) {
        terrainGenerator.generateTerrain(world, chunk);
    }

    @Override
    public final void generateSurface(WorldGenRegion world, IChunk chunk) {
        surfaceGenerator.generateSurface(world, chunk);
    }

    @Override
    public final void decorate(WorldGenRegion region) {
        featureGenerator.generateFeatures(region);
    }

    @Override
    public final void func_225550_a_(BiomeManager biomes, IChunk chunk, GenerationStage.Carving type) {
        terrainCarver.carveTerrain(biomes, chunk, type);
    }

    @Override
    public final void spawnMobs(WorldGenRegion region) {
        mobGenerator.generateMobs(region);
    }

    @Override
    public final void spawnMobs(ServerWorld worldIn, boolean hostile, boolean peaceful) {
        mobGenerator.tickSpawners(worldIn, hostile, peaceful);
    }

    @Override
    public final List<Biome.SpawnListEntry> getPossibleCreatures(EntityClassification type, BlockPos pos) {
        return mobGenerator.getSpawns(world, type, pos);
    }

    @Override
    public Biome getBiome(BiomeManager manager, BlockPos pos) {
        return super.getBiome(manager, pos);
    }

    @Override
    public final int func_222529_a(int x, int z, Heightmap.Type type) {
        int chunkX = Size.blockToChunk(x);
        int chunkZ = Size.blockToChunk(z);
        try (ChunkReader chunk = getChunkReader(chunkX, chunkZ)) {
            Cell cell = chunk.getCell(x, z);
            int level = context.levels.scale(cell.value) + 1;
            if (type == Heightmap.Type.OCEAN_FLOOR || type == Heightmap.Type.OCEAN_FLOOR_WG) {
                return level;
            }
            return Math.max(getSeaLevel(), level);
        }
    }

    @Override
    public TerraBiomeProvider getBiomeProvider() {
        return biomeProvider;
    }

    @Override
    public final int getMaxHeight() {
        return getContext().levels.worldHeight;
    }

    @Override
    public final int getSeaLevel() {
        return getContext().levels.waterLevel;
    }

    @Override
    public final int getGroundHeight() {
        return getContext().levels.groundLevel;
    }

    public final TerraContext getContext() {
        return context;
    }

    public final Materials getMaterials() {
        return context.materials;
    }

    public final FeatureManager getFeatureManager() {
        return featureManager;
    }

    public final StructureManager getStructureManager() {
        return structureManager;
    }

    public final GeoManager getGeologyManager() {
        return geologyManager;
    }

    public final LayerManager getLayerManager() {
        return context.materials.getLayerManager();
    }

    public final SurfaceManager getSurfaceManager() {
        return surfaceManager;
    }

    public final BlockDataManager getBlockDataManager() {
        return blockDataManager;
    }

    public final List<ColumnDecorator> getBaseDecorators() {
        return baseDecorators;
    }

    public final List<ColumnDecorator> getPostProcessors() {
        return postProcessors;
    }

    public final void queueChunk(int chunkX, int chunkZ) {
        int rx = tileCache.chunkToRegion(chunkX);
        int rz = tileCache.chunkToRegion(chunkZ);
        tileCache.queueRegion(rx, rz);
    }

    public final Tile getTile(int chunkX, int chunkZ) {
        int rx = tileCache.chunkToRegion(chunkX);
        int rz = tileCache.chunkToRegion(chunkZ);
        return tileCache.getRegion(rx, rz);
    }

    public final ChunkReader getChunkReader(int chunkX, int chunkZ) {
        return tileCache.getChunk(chunkX, chunkZ);
    }

    public static ChunkReader getChunk(IWorld world, ChunkGenerator<?> generator) {
        if (generator instanceof TerraChunkGenerator) {
            TerraChunkGenerator terra = (TerraChunkGenerator) generator;
            if (world instanceof IChunk) {
                IChunk chunk = (IChunk) world;
                return terra.getChunkReader(chunk.getPos().x, chunk.getPos().z);
            }

            if (world instanceof WorldGenRegion) {
                WorldGenRegion region = (WorldGenRegion) world;
                return terra.getChunkReader(region.getMainChunkX(), region.getMainChunkZ());
            }
        }
        return null;
    }
}
