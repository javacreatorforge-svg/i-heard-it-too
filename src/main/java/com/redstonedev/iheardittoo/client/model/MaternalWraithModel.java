package com.redstonedev.iheardittoo.client.model;

import com.redstonedev.iheardittoo.IHeardItToo;
import com.redstonedev.iheardittoo.entity.MaternalWraithEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class MaternalWraithModel extends AnimatedGeoModel<MaternalWraithEntity> {
    private static final ResourceLocation MODEL =
            new ResourceLocation(IHeardItToo.MODID, "geo/maternal_wraith.geo.json");
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(IHeardItToo.MODID, "textures/entity/maternal_wraith.png");
    private static final ResourceLocation ANIMATIONS =
            new ResourceLocation(IHeardItToo.MODID, "animations/maternal_wraith.animation.json");

    @Override public ResourceLocation getModelResource(MaternalWraithEntity e)     { return MODEL; }
    @Override public ResourceLocation getTextureResource(MaternalWraithEntity e)   { return TEXTURE; }
    @Override public ResourceLocation getAnimationResource(MaternalWraithEntity e) { return ANIMATIONS; }
}
