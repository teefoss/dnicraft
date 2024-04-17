package net.teefoss.dnicraft.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Lifecycle;
import net.fabricmc.fabric.api.dimension.v1.FabricDimensions;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.world.*;
import net.minecraft.world.biome.*;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.BiomeSources;
import net.minecraft.world.biome.source.FixedBiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import net.minecraft.world.biome.source.util.VanillaBiomeParameters;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.dimension.DimensionTypes;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.*;
import net.minecraft.world.gen.densityfunction.DensityFunctions;
import net.minecraft.world.gen.feature.DefaultBiomeFeatures;
import net.minecraft.world.gen.feature.MiscPlacedFeatures;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.feature.VegetationPlacedFeatures;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.surfacebuilder.VanillaSurfaceRules;
import net.teefoss.dnicraft.Age;
import net.teefoss.dnicraft.Ages;
import net.teefoss.dnicraft.DniCraft;
import net.teefoss.dnicraft.DniDimensions;
import qouteall.dimlib.api.DimensionAPI;
import qouteall.dimlib.ducks.IMappedRegistry;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

public class TestStick extends Item {
    public TestStick(Item.Settings settings) {
        super(settings);
    }

    private void createWorld(ItemStack stack, MinecraftServer server, UUID uuid) {

//        UUID uuid = UUID.randomUUID();

        NbtCompound tag = stack.getOrCreateNbt();
        tag.putUuid("dimension", uuid);
        stack.setNbt(tag);

        Identifier identifier = new Identifier(DniCraft.MOD_ID, uuid.toString());

        DynamicRegistryManager.Immutable registryAccess = server.getRegistryManager();

        // DimensionType
        Registry<DimensionType> dimensionTypeRegistry = registryAccess.get(RegistryKeys.DIMENSION_TYPE);
//        RegistryEntry<DimensionType> dimensionTypeEntry = dimensionTypeRegistry.getEntry(DniDimensions.DIM_TYPE_KEY).orElseThrow();

        DimensionType dimensionType = new DimensionType(
            OptionalLong.of(6000),
            true,
            false, // hasCeiling
            false, // ultrawarm
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
        RegistryEntry<ChunkGeneratorSettings> oldSettingsEntry = settingsRegistry.getEntry(ChunkGeneratorSettings.OVERWORLD).orElseThrow();
        ChunkGeneratorSettings oldSettings = oldSettingsEntry.value();
        ChunkGeneratorSettings newSettings = new ChunkGeneratorSettings(
            oldSettings.generationShapeConfig(),
            Blocks.CRAFTING_TABLE.getDefaultState(),
            Blocks.GLASS.getDefaultState(),
            oldSettings.noiseRouter(),
            oldSettings.surfaceRule(),
            (new VanillaBiomeParameters()).getSpawnSuitabilityNoises(),
            63,
            false,
            true,
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

        RegistryEntry<PlacedFeature> birchFeature = placedFeatures.getEntry(MiscPlacedFeatures.ICE_SPIKE).orElseThrow();
        RegistryEntry<PlacedFeature> mangroveFeature = placedFeatures.getEntry(MiscPlacedFeatures.FOREST_ROCK).orElseThrow();
        generationSettingsBuilder.feature(GenerationStep.Feature.VEGETAL_DECORATION, birchFeature);
        generationSettingsBuilder.feature(GenerationStep.Feature.VEGETAL_DECORATION, mangroveFeature);

        // Feature type <-> PlacedFeature types
        // GenerationSettings.Builder needs GenerationStep.Feature.? String

//        System.out.printf("placed feature toString: %s\n", VegetationPlacedFeatures.TREES_BIRCH.toString());
//        System.out.printf("placed feature value toString: %s\n", VegetationPlacedFeatures.TREES_BIRCH.getValue().toString()); // <- This one
//        System.out.printf("placed feature value getPath: %s\n", VegetationPlacedFeatures.TREES_BIRCH.getValue().getPath());

        // It works! DON"T TOUCH IT
        // TODO: test mobs (Spawn Settings)
        Biome biome = new Biome.Builder()
            .precipitation(true)
            .downfall(0.0f)
            .generationSettings(generationSettingsBuilder.build())
            .effects(new BiomeEffects.Builder()
                .fogColor(0)
                .foliageColor(0xff00ff00)
                .grassColor(0xffffaaaa)
                .skyColor(0xffffffff)
                .waterColor(0xff00ffff)
                .waterFogColor(0)
                .build()
            )
            .temperature(0.0f)
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

//        Identifier biomeIdentifier = new Identifier(DniCraft.MOD_ID, "biome");

        RegistryEntry<Biome> biomeRegistryEntry = biomeRegistry.getEntry(RegistryKey.of(RegistryKeys.BIOME, identifier)).orElseThrow();

        // Putting it all together!
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

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (world.isClient) {
            return TypedActionResult.success(stack);
        }

        if (stack.hasNbt()) {
            UUID uuid = stack.getNbt().getUuid("dimension");
            Identifier identifier = new Identifier(DniCraft.MOD_ID, uuid.toString());
            RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, identifier);

            Vec3d position = new Vec3d(0, 200, 0);
            TeleportTarget target = new TeleportTarget(position, Vec3d.ZERO, 0.0f, 0.0f);

            if ( !DimensionAPI.dimensionExistsInRegistry(world.getServer(), identifier) ) {
//                createWorld(stack, world.getServer(), uuid);
                Ages.registerAgeDynamically(world.getServer(), uuid);
            }

            ServerWorld dest = world.getServer().getWorld(worldKey);
            FabricDimensions.teleport(user, dest, target);
        } else {
            Age age = new Age();
            System.out.println("randomize age:");
            age.randomize();
            age.uuid = UUID.randomUUID();

            NbtCompound tag = stack.getOrCreateNbt();
            tag.putUuid("dimension", age.uuid);
            stack.setNbt(tag);

            Ages.add(age);
            System.out.println("register age:");
            Ages.registerAgeDynamically(world.getServer(), age.uuid);
        }

        return TypedActionResult.success(stack);
    }
}
