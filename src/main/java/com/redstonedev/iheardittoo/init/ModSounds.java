package com.redstonedev.iheardittoo.init;

import com.redstonedev.iheardittoo.IHeardItToo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, IHeardItToo.MODID);

    public static final RegistryObject<SoundEvent> SARAH_VOICE = register("sarah_voice");
    public static final RegistryObject<SoundEvent> CHASE_THEME = register("chase_theme");
    public static final RegistryObject<SoundEvent> CHASE_SARAH = register("chase_sarah");
    public static final RegistryObject<SoundEvent> MAD_SARAH   = register("mad_sarah");

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name,
                () -> new SoundEvent(new ResourceLocation(IHeardItToo.MODID, name)));
    }
}
