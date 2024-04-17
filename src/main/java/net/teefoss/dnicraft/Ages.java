package net.teefoss.dnicraft;

import com.mojang.serialization.Lifecycle;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeEffects;
import net.minecraft.world.biome.GenerationSettings;
import net.minecraft.world.biome.SpawnSettings;
import net.minecraft.world.biome.source.FixedBiomeSource;
import net.minecraft.world.biome.source.util.VanillaBiomeParameters;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.dimension.DimensionTypes;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import net.minecraft.world.gen.feature.PlacedFeature;
import qouteall.dimlib.api.DimensionAPI;
import qouteall.dimlib.ducks.IMappedRegistry;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

// https://github.com/McJtyMods/RFToolsDimensions/issues/126

public class Ages {
    private static final Map<UUID, Age> map = new HashMap<>();

    private static Path getDataFilePath() {
        return FabricLoader.getInstance().getConfigDir().resolve("dnicraft-dimensions.dat");
    }

    // A .dat file with NBT format is stored in the config folder.
    // All entries are NBT compounds of dimension properties with a key of the dimension's UUID.
    // This function simply loads the map.
    public static void load() {
        System.out.println("Ages.load()");
        Path path = getDataFilePath();

        // Get the NBT, or create the data file if it doesn't exist.
        NbtCompound tag;
        try {
            File file = new File(path.toString());
            if ( !file.exists() ) {
                System.out.println("Data file does not exist, creating it...");
                file.createNewFile();
                tag = new NbtCompound();
                tag.putInt("dummy", 0); // So there's something there(?)
                NbtIo.write(tag, path);
                return;
            } else {
                tag = NbtIo.read(getDataFilePath());
            }
        } catch (IOException e) {
            System.out.println("Could not read/write data file!");
            throw new RuntimeException(e);
        }

        // Load the map from the NBT.
        System.out.println("Read into map:");
        map.clear();
        Set<String> keys = tag.getKeys();
        for ( String key : keys ) {
            if ( key.equals("dummy") ) {
                continue;
            }
            System.out.printf("  loading dimension %s\n", key);
            NbtCompound ageTag = tag.getCompound(key);
            if ( ageTag == null ) {
                System.out.println("  null!");
            }
            Age age = new Age(ageTag);
            map.put(UUID.fromString(key), age);
        }
    }

    public static void add(Age age) {
        System.out.println("Ages.add()");
        map.put(age.uuid, age);

        // Also add to .dat file:
        Path dataFilePath = getDataFilePath();
        NbtCompound tag;
        try {
            // The file should exist at this point.
            tag = NbtIo.read(dataFilePath);
        } catch (IOException e) {
            System.out.println("Could not read data file!");
            throw new RuntimeException(e);
        }

        if ( tag == null ) {
            System.out.println("(tag is null)");
            tag = new NbtCompound();
        }

        NbtCompound ageTag = age.makeTag();
        tag.put(age.uuid.toString(), ageTag);

        try {
            NbtIo.write(tag, dataFilePath);
        } catch (IOException e) {
            System.out.println("Could not write NBT to data file!");
            throw new RuntimeException(e);
        }
    }

    // Register all ages in map.
    public static void registerAllAges(MinecraftServer server) {
        System.out.println("registerAllAges()");
        for ( UUID uuid : map.keySet() ) {
            registerAgeDynamically(server, uuid);
        }
    }

    public static void registerAgeDynamically(MinecraftServer server, UUID uuid) {

        Age age = map.get(uuid);
        if ( age == null ) {
            return;
        }

        Identifier identifier = new Identifier(DniCraft.MOD_ID, age.uuid.toString());

        DynamicRegistryManager.Immutable registryAccess = server.getRegistryManager();

        // DimensionType
        Registry<DimensionType> dimensionTypeRegistry = registryAccess.get(RegistryKeys.DIMENSION_TYPE);

        DimensionType dimensionType = new DimensionType(
            age.fixedTime ? OptionalLong.of(age.time) : OptionalLong.empty(),
            age.hasSun,
            age.terrain == Age.Terrain.CAVERNOUS, // hasCeiling
            age.temperature == Age.Extent.EXTREMELY_LOW, // ultrawarm
            true, // natural
            1.0f, // coordinateScale
            true, // bedWorks
            false,
            0,
            256,
            256,
            BlockTags.INFINIBURN_OVERWORLD,
            DimensionTypes.OVERWORLD_ID,
            0.0f,
            new DimensionType.MonsterSettings(false, false, UniformIntProvider.create(0, 0), 0)
        );

        ((IMappedRegistry) dimensionTypeRegistry).dimlib_setIsFrozen(false);
        ((SimpleRegistry<DimensionType>) dimensionTypeRegistry).add(
            RegistryKey.of(RegistryKeys.DIMENSION_TYPE, identifier),
            dimensionType,
            Lifecycle.stable()
        );
        ((IMappedRegistry) dimensionTypeRegistry).dimlib_setIsFrozen(true);

        RegistryEntry<DimensionType> dimensionTypeEntry = dimensionTypeRegistry.getEntry(RegistryKey.of(RegistryKeys.DIMENSION_TYPE, identifier)).orElseThrow();

        // ------------------------------------------------------
        // Noise Settings / ChunkGeneratorSettings

        Registry<ChunkGeneratorSettings> settingsRegistry = registryAccess.get(RegistryKeys.CHUNK_GENERATOR_SETTINGS);

        // Use preexisting settings as a template.
        RegistryKey<ChunkGeneratorSettings> key;
        if ( age.terrain == Age.Terrain.CAVERNOUS ) {
            key = ChunkGeneratorSettings.CAVES;
        } else if ( age.terrain == Age.Terrain.ERODED ) {
            key = ChunkGeneratorSettings.FLOATING_ISLANDS;
        } else {
            key = ChunkGeneratorSettings.OVERWORLD;
        }

        RegistryEntry<ChunkGeneratorSettings> oldEntry = settingsRegistry.getEntry(key).orElseThrow();
        ChunkGeneratorSettings oldSettings = oldEntry.value();
        ChunkGeneratorSettings newSettings = new ChunkGeneratorSettings(
            oldSettings.generationShapeConfig(),
            age.groundBlock,
            age.waterBlock,
            oldSettings.noiseRouter(),
            oldSettings.surfaceRule(),
            (new VanillaBiomeParameters()).getSpawnSuitabilityNoises(),
            age.oceanLevel,
            false,
            age.floodedCaves,
            true,
            false
        );

        ((IMappedRegistry) settingsRegistry).dimlib_setIsFrozen(false);
        ((SimpleRegistry<ChunkGeneratorSettings>) settingsRegistry).add(
            RegistryKey.of(RegistryKeys.CHUNK_GENERATOR_SETTINGS, identifier),
            newSettings,
            Lifecycle.stable()
        );
        ((IMappedRegistry) settingsRegistry).dimlib_setIsFrozen(true);

        RegistryEntry<ChunkGeneratorSettings> settingsEntry = settingsRegistry.getEntry(RegistryKey.of(RegistryKeys.CHUNK_GENERATOR_SETTINGS, identifier)).orElseThrow();

        // ------------------------------------------------------
        // Biome

        Registry<Biome> biomeRegistry = registryAccess.get(RegistryKeys.BIOME);

        Registry<PlacedFeature> placedFeatures = registryAccess.get(RegistryKeys.PLACED_FEATURE);
        GenerationSettings.Builder generationSettingsBuilder = new GenerationSettings.Builder();

        for ( Age.Feature feature : age.features ) {

        }
        // TODO: features
//        RegistryEntry<PlacedFeature> birchFeature = placedFeatures.getEntry(MiscPlacedFeatures.ICE_SPIKE).orElseThrow();
//        RegistryEntry<PlacedFeature> mangroveFeature = placedFeatures.getEntry(MiscPlacedFeatures.FOREST_ROCK).orElseThrow();
//        generationSettingsBuilder.feature(GenerationStep.Feature.VEGETAL_DECORATION, birchFeature);
//        generationSettingsBuilder.feature(GenerationStep.Feature.VEGETAL_DECORATION, mangroveFeature);

        // Feature type <-> PlacedFeature types
        // GenerationSettings.Builder needs GenerationStep.Feature.? String

//        System.out.printf("placed feature toString: %s\n", VegetationPlacedFeatures.TREES_BIRCH.toString());
//        System.out.printf("placed feature value toString: %s\n", VegetationPlacedFeatures.TREES_BIRCH.getValue().toString()); // <- This one
//        System.out.printf("placed feature value getPath: %s\n", VegetationPlacedFeatures.TREES_BIRCH.getValue().getPath());

        // TODO: test mobs (Spawn Settings)
        Biome biome = new Biome.Builder()
            .precipitation(age.hasPrecipitation)
            .downfall(0.0f) // TODO: calculate
            .generationSettings(generationSettingsBuilder.build())
            .effects(new BiomeEffects.Builder()
                .fogColor(0)
                .foliageColor(0xff00ff00)
                .grassColor(age.grassColor)
                .skyColor(age.skyColor)
                .waterColor(age.waterColor)
                .waterFogColor(0)
                .build()
            )
            .temperature(age.getTemperature())
            .spawnSettings(new SpawnSettings.Builder()
                // Mobs here
                .build()
            )
            .build();

        ((IMappedRegistry) biomeRegistry).dimlib_setIsFrozen(false);
        ((SimpleRegistry<Biome>) biomeRegistry).add(
            RegistryKey.of(RegistryKeys.BIOME, identifier),
            biome,
            Lifecycle.stable()
        );
        ((IMappedRegistry) biomeRegistry).dimlib_setIsFrozen(true);
        RegistryEntry<Biome> biomeRegistryEntry = biomeRegistry.getEntry(RegistryKey.of(RegistryKeys.BIOME, identifier)).orElseThrow();

        DimensionAPI.addDimension(
            server,
            identifier,
            new DimensionOptions(
                dimensionTypeEntry,
                new NoiseChunkGenerator(
                    new FixedBiomeSource(biomeRegistryEntry),
                    settingsEntry
                )
            )
        );
    }
}
