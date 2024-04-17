package net.teefoss.dnicraft;

import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.dimension.DimensionTypes;
import net.teefoss.dnicraft.item.LinkingBook;
import net.teefoss.dnicraft.item.TestStick;

import java.util.Random;

public class DniContent {
    public static final Item LINKING_BOOK = registerItem("linking_book", new LinkingBook(new FabricItemSettings()));
    public static final Item TEST_STICK = registerItem("test_stick", new TestStick(new FabricItemSettings()));

//    public static final

    public static final ItemGroup DNICRAFT_GROUP = Registry.register(Registries.ITEM_GROUP,
        new Identifier(DniCraft.MOD_ID, "dnicraft"),
        FabricItemGroup.builder()
            .displayName(Text.translatable("itemgroup.dnicraft"))
            .icon(() -> new ItemStack(DniContent.LINKING_BOOK))
            .entries((displayContext, entries) -> {
                entries.add(LINKING_BOOK);
                entries.add(TEST_STICK);
            }).build());

    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, new Identifier(DniCraft.MOD_ID, name), item);
    }

    public static void register() {

    }
}
