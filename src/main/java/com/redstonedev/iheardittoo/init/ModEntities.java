package com.redstonedev.iheardittoo.init;

import com.redstonedev.iheardittoo.IHeardItToo;
import com.redstonedev.iheardittoo.entity.MaternalWraithEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, IHeardItToo.MODID);

    public static final RegistryObject<EntityType<MaternalWraithEntity>> MATERNAL_WRAITH =
            ENTITIES.register("maternal_wraith", () -> EntityType.Builder
                    .<MaternalWraithEntity>of(MaternalWraithEntity::new, MobCategory.MONSTER)
                    .sized(0.8F, 2.0F)
                    .clientTrackingRange(12)
                    .build(new ResourceLocation(IHeardItToo.MODID, "maternal_wraith").toString()));
}
