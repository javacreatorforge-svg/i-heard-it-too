package com.redstonedev.iheardittoo;

import com.mojang.logging.LogUtils;
import com.redstonedev.iheardittoo.client.ClientSetup;
import com.redstonedev.iheardittoo.entity.MaternalWraithEntity;
import com.redstonedev.iheardittoo.event.ForgeEvents;
import com.redstonedev.iheardittoo.init.ModEntities;
import com.redstonedev.iheardittoo.init.ModItems;
import com.redstonedev.iheardittoo.init.ModSounds;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import software.bernie.geckolib3.GeckoLib;

@Mod(IHeardItToo.MODID)
public class IHeardItToo {
    public static final String MODID = "i_heard_it_too";
    public static final Logger LOGGER = LogUtils.getLogger();

    public IHeardItToo() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        GeckoLib.initialize();

        ModEntities.ENTITIES.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModSounds.SOUND_EVENTS.register(modBus);

        modBus.addListener(this::commonSetup);
        modBus.addListener(this::clientSetup);
        modBus.addListener(this::entityAttributes);

        MinecraftForge.EVENT_BUS.register(new ForgeEvents());
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("I Heard It Too - common setup");
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        ClientSetup.onClientSetup(event);
    }

    private void entityAttributes(final EntityAttributeCreationEvent event) {
        event.put(ModEntities.MATERNAL_WRAITH.get(), MaternalWraithEntity.createAttributes().build());
    }
}
