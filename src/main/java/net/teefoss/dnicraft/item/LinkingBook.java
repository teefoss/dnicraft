package net.teefoss.dnicraft.item;

import net.fabricmc.fabric.api.dimension.v1.FabricDimensions;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.teefoss.dnicraft.DniBookScreen;
import net.teefoss.dnicraft.Link;
import org.apache.logging.log4j.core.jmx.Server;
import org.jetbrains.annotations.Nullable;
import qouteall.dimlib.api.DimensionAPI;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class LinkingBook extends Item {

    public LinkingBook(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        String text;

        if ( stack.hasNbt() ) {
            NbtCompound tag = stack.getNbt();
            String namespace = tag.getString("namespace");
            String path = tag.getString("path");
            RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, new Identifier(namespace, path));

            if ( worldKey.equals(World.OVERWORLD) ) {
                text = "to the Overworld"; // TODO: translatable
            } else if ( worldKey.equals(World.NETHER) ) {
                text = "to the Nether";
            } else { // TODO: the End
                text = path;
            }
        } else {
            text = "Unlinked";
        }

        tooltip.add(
            Text.literal(text)
                .formatted(Formatting.GRAY)
        );
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {

        ItemStack stack = user.getStackInHand(hand);

        if ( world.isClient ) {
            return TypedActionResult.success(stack, false);
        }

        if ( stack.hasNbt() ) {
            Link.prepare(stack, world, user);

            DniBookScreen.onCommandExecuted(
                (ServerPlayerEntity)user,
                Link.destinationWorld,
                new Vec3d(
                    Link.target.position.x,
                    Link.target.position.y + user.getEyeHeight(user.getPose()),
                    Link.target.position.z
                ),
                stack
            );
        } else {
            // This is an unlinked book, record the link information
            NbtCompound tag = stack.getOrCreateNbt();
            tag.putUuid("bookID", UUID.randomUUID()); // TODO: unneeded?
            tag.putDouble("x", user.getX());
            tag.putDouble("y", user.getY());
            tag.putDouble("z", user.getZ());
            tag.putInt("facing", user.getHorizontalFacing().getHorizontal());
            tag.putString("namespace", world.getRegistryKey().getValue().getNamespace());
            tag.putString("path", world.getRegistryKey().getValue().getPath());
            stack.setNbt(tag);
            System.out.println("Wrote NBT to unlinked book");
        }

        return TypedActionResult.success(stack, false);
    }
}
