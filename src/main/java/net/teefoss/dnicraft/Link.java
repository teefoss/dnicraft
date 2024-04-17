package net.teefoss.dnicraft;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.dimension.v1.FabricDimensions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import org.apache.logging.log4j.core.jmx.Server;

public class Link {

    public static ServerWorld destinationWorld;
    public static TeleportTarget target;
    private static PlayerEntity user;

    public static void prepare(ItemStack stack, World world, PlayerEntity _user) {
        user = _user;

        NbtCompound tag = stack.getNbt();
        double x = tag.getDouble("x");
        double y = tag.getDouble("y");
        double z = tag.getDouble("z");
        String namespace = tag.getString("namespace");
        String path = tag.getString("path");

        RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, new Identifier(namespace, path));
        destinationWorld = world.getServer().getWorld(worldKey);

        target = new TeleportTarget(new Vec3d(x, y, z), Vec3d.ZERO, 0.0f, 0.0f);
    }

    public static class RemoteCallables {
        @Environment(EnvType.CLIENT)
        public static void transportPlayer(PlayerEntity player) {
            FabricDimensions.teleport(user, destinationWorld, target);
        }
    }
}
