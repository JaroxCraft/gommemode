package de.jarox.gommemode.entity

import de.jarox.gommemode.Gommemode
import net.minecraft.client.render.entity.EntityRendererFactory
import net.minecraft.client.render.entity.LivingEntityRenderer
import net.minecraft.client.render.entity.model.EntityModelLayers
import net.minecraft.client.render.entity.model.PlayerEntityModel
import net.minecraft.util.Identifier

class GommeEntityRenderer(ctx: EntityRendererFactory.Context) :
    LivingEntityRenderer<GommeEntity?, PlayerEntityModel<GommeEntity?>?>(
        ctx,
        PlayerEntityModel<GommeEntity?>(ctx.getPart(EntityModelLayers.PLAYER), false),
        0.5f
    ) {
    override fun getTexture(entity: GommeEntity?): Identifier {
        return Identifier.of(Gommemode.MOD_ID, "textures/entity/gommehd.png")
    }
}
