package net.teefoss.dnicraft.datagen;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraft.registry.RegistryBuilder;

public class DataGeneration implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
//        FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();

//        pack.addProvider(WorldProvider::new);
    }

    @Override
    public void buildRegistry(RegistryBuilder registryBuilder) {
//        registryBuilder.addRegistry(RegistryKeys.DIMENSION_TYPE, DniDimensions::bootstrap);
    }
}
