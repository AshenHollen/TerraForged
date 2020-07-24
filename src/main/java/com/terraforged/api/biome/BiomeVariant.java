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

package com.terraforged.api.biome;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;

public abstract class BiomeVariant extends Biome {

    protected BiomeVariant(Builder biomeBuilder) {
        super(biomeBuilder);
    }

    // override to register a custom biome weight with Forge's BiomeManager (default is 10)
    public void registerWeights() {}

    @Override
    public int getGrassColor(double x, double z) {
        return getBase().getGrassColor(x, z);
    }

    @Override
    public int getFoliageColor() {
        return getBase().getFoliageColor();
    }

    @Override
    public int getSkyColor() {
        return getBase().getSkyColor();
    }

    public abstract Biome getBase();

    protected static float getTemperatureNoise(BlockPos pos) {
        float value = (float) (TEMPERATURE_NOISE.noiseAt(pos.getX() / 8F, pos.getZ() / 8F, false) * 4F);
        return (value + pos.getY() - 64F) * 0.05F / 30F;
    }
}
