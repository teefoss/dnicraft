package net.teefoss.dnicraft;

import net.minecraft.registry.Registerable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.dimension.DimensionTypes;

import java.util.Random;

public class DniDimensions {
    public static final Identifier ID = new Identifier(DniCraft.MOD_ID, "dni_dim");
    public static final RegistryKey<DimensionType> DIM_TYPE_KEY = RegistryKey.of(RegistryKeys.DIMENSION_TYPE, ID);

//    public static void bootstrap(Registerable<DimensionType> context) {
//        Random random = new Random();
//        DimensionType dimensionType = new DimensionType(
//            random.nextBoolean() ? null : random.longs(0, 23999).findFirst(),
//            random.nextBoolean(), // hasSkylight
//            random.nextBoolean(), // hasCeiling
//            random.nextBoolean(), // ultraWarm
//            true, // natural
//            1.0f, // coordinate scale
//            true, // bedWorks
//            false, // respawnAnchorsWork
//            0,
//            256,
//            256,
//            BlockTags.INFINIBURN_OVERWORLD,
//            DimensionTypes.OVERWORLD_ID, // effectsLocation
//            1.0f, // ambientLight
//            new DimensionType.MonsterSettings(false, false, UniformIntProvider.create(0, 0), 0)
//        );
//
//        context.register(DIM_TYPE_KEY, dimensionType);
//    }
}
