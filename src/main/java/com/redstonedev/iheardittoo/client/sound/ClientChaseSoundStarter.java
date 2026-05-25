package com.redstonedev.iheardittoo.client.sound;

import com.redstonedev.iheardittoo.entity.MaternalWraithEntity;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ClientChaseSoundStarter {
    private ClientChaseSoundStarter() {}
    public static void start(MaternalWraithEntity entity) {
        Minecraft.getInstance().getSoundManager().play(new MaternalWraithChaseSoundInstance(entity));
    }
}
