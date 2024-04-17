package net.teefoss.dnicraft;

import com.mojang.serialization.Codec;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;

import java.util.stream.Stream;

public class AgeBiomeSource extends BiomeSource {

    @Override
    protected Codec<? extends BiomeSource> getCodec() {
        return null;
    }

    @Override
    protected Stream<RegistryEntry<Biome>> biomeStream() {
        return null;
    }

    @Override
    public RegistryEntry<Biome> getBiome(int x, int y, int z, MultiNoiseUtil.MultiNoiseSampler noise) {
        return null;
    }
}
