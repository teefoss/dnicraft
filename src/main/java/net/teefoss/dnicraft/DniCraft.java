package net.teefoss.dnicraft;

import com.mojang.authlib.minecraft.client.MinecraftClient;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qouteall.dimlib.DynamicDimensionsImpl;
import qouteall.dimlib.api.DimensionAPI;

import java.nio.file.Path;

public class DniCraft implements ModInitializer {
	public static final String MOD_ID = "dnicraft";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger("dnicraft");

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		System.out.println("~~~~~ Mod onInitialize() ~~~~~");

		Ages.load();

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			System.out.println("~~~~~ Server Started Event ~~~~~");
		});

		DimensionAPI.SERVER_DIMENSIONS_LOAD_EVENT.register(server -> {
			System.out.println("~~~~~ Dimension Load Event ~~~~~");
			Ages.registerAllAges(server);
		});

		DniContent.register();
	}
}