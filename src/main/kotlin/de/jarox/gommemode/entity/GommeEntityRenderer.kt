package de.jarox.gommemode.entity

import de.jarox.gommemode.Gommemode
import net.minecraft.client.model.geom.ModelLayers
import net.minecraft.client.model.player.PlayerModel
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.LivingEntityRenderer
import net.minecraft.client.renderer.entity.state.AvatarRenderState
import net.minecraft.resources.Identifier

class GommeEntityRenderer(ctx: EntityRendererProvider.Context) :
    LivingEntityRenderer<GommeEntity, AvatarRenderState, PlayerModel>(
        ctx,
        PlayerModel(ctx.bakeLayer(ModelLayers.PLAYER), false),
        0.5f
    ) {
    override fun createRenderState(): AvatarRenderState = AvatarRenderState()

    override fun getTextureLocation(state: AvatarRenderState): Identifier =
        Identifier.fromNamespaceAndPath(Gommemode.MOD_ID, "textures/entity/gommehd.png")
}
