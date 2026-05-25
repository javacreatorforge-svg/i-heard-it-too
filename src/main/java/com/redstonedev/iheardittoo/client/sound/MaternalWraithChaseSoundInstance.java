package com.redstonedev.iheardittoo.client.sound;

import com.redstonedev.iheardittoo.entity.MaternalWraithEntity;
import com.redstonedev.iheardittoo.init.ModSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MaternalWraithChaseSoundInstance extends AbstractTickableSoundInstance {
    private final MaternalWraithEntity wraith;

    public MaternalWraithChaseSoundInstance(MaternalWraithEntity entity) {
        super(ModSounds.CHASE_THEME.get(), SoundSource.HOSTILE, RandomSource.create());
        this.wraith = entity;
        this.looping = true;
        this.delay = 0;
        this.volume = 1.0F;
        this.pitch = 1.0F;
        this.attenuation = Attenuation.LINEAR;
        this.x = entity.getX();
        this.y = entity.getY();
        this.z = entity.getZ();
    }

    @Override
    public void tick() {
        if (wraith.isRemoved() || !wraith.isAlive() || !wraith.isAggressiveMode()) {
            this.stop();
            return;
        }
        this.x = wraith.getX();
        this.y = wraith.getY();
        this.z = wraith.getZ();
    }
}
