package com.redstonedev.iheardittoo.init;

import com.redstonedev.iheardittoo.IHeardItToo;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, IHeardItToo.MODID);

    public static final RegistryObject<ForgeSpawnEggItem> MATERNAL_WRAITH_SPAWN_EGG =
            ITEMS.register("maternal_wraith_spawn_egg",
                    () -> new ForgeSpawnEggItem(
                            ModEntities.MATERNAL_WRAITH,
                            0xD4C4B0,  // pale skin
                            0x1F1F1F,  // deep black eyes
                            new Item.Properties().tab(CreativeModeTab.TAB_MISC)));
}
