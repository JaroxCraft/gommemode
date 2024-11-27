package de.jarox.gommemode.entity;

import de.jarox.gommemode.GommeMode;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.util.Identifier;

public class GommeEntityRenderer extends LivingEntityRenderer<GommeEntity, GommeEntityModel> {

    public GommeEntityRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new GommeEntityModel(ctx.getPart(EntityModelLayers.PLAYER)), 0.5f);
    }

    @Override
    public Identifier getTexture(GommeEntity entity) {
        return Identifier.of(GommeMode.MOD_ID, "textures/entity/gommehd.png");
    }
}
