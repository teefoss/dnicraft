package net.teefoss.dnicraft;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;
import net.minecraft.registry.*;
import net.minecraft.util.Identifier;
import net.minecraft.world.dimension.DimensionType;

import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;

public class DniCraftDataGenerator extends FabricDynamicRegistryProvider implements DataGeneratorEntrypoint {
	public DniCraftDataGenerator(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
		super(output, registriesFuture);
	}

	@Override
	public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {

	}

	@Override
	protected void configure(RegistryWrapper.WrapperLookup registries, Entries entries) {
		entries.addAll(registries.getWrapperOrThrow(RegistryKeys.DIMENSION_TYPE));
	}

	@Override
	public String getName() {
		return "Data Generator";
	}

	private static void bootstrapDimensionType(Registerable<DimensionType> context) {
		OptionalLong[] times = {
			null,
			OptionalLong.of(23500),
			OptionalLong.of(6000),
			OptionalLong.of(13500),
			OptionalLong.of(18000)
		};

		for ( int time = 0; time < times.length; time++ ) {
			for ( int sky = 0; sky < 2; sky++ ) {
				for ( int warm = 0; warm < 2; warm++ ) {

					String name = "";
//					name += time.

//					RegistryKey<DimensionType> key = RegistryKey.of(
//						RegistryKeys.DIMENSION_TYPE,
//						new Identifier(DniCraft.MOD_ID, name)
//					);
//					context.register()
				}
			}
		}
	}
}
