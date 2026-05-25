package com.redstonedev.iheardittoo.client.renderer;

import com.redstonedev.iheardittoo.client.model.MaternalWraithModel;
import com.redstonedev.iheardittoo.entity.MaternalWraithEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;

@OnlyIn(Dist.CLIENT)
public class MaternalWraithRenderer extends GeoEntityRenderer<MaternalWraithEntity> {
    public MaternalWraithRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new MaternalWraithModel());
        this.shadowRadius = 0.4F;
    }
}
