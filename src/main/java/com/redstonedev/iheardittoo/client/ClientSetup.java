package com.redstonedev.iheardittoo.client;

import com.redstonedev.iheardittoo.client.renderer.MaternalWraithRenderer;
import com.redstonedev.iheardittoo.init.ModEntities;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public class ClientSetup {
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            EntityRenderers.register(ModEntities.MATERNAL_WRAITH.get(), MaternalWraithRenderer::new);
        });
    }
}
