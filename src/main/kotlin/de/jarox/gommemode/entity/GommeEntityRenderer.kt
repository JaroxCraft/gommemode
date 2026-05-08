package de.jarox.gommemode.entity

import de.jarox.gommemode.Gommemode
import net.minecraft.client.model.geom.ModelLayers
import net.minecraft.client.model.player.PlayerModel
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.LivingEntityRenderer
import net.minecraft.client.renderer.entity.state.AvatarRenderState
import net.minecraft.core.ClientAsset
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.player.PlayerModelType
import net.minecraft.world.entity.player.PlayerSkin

class GommeEntityRenderer(ctx: EntityRendererProvider.Context) :
    LivingEntityRenderer<GommeEntity, AvatarRenderState, PlayerModel>(
        ctx,
        PlayerModel(ctx.bakeLayer(ModelLayers.PLAYER), false),
        0.5f
    ) {
    override fun createRenderState(): AvatarRenderState = AvatarRenderState()

    override fun extractRenderState(entity: GommeEntity, state: AvatarRenderState, partialTick: Float) {
        super.extractRenderState(entity, state, partialTick)
        val id = Identifier.fromNamespaceAndPath(Gommemode.MOD_ID, "entity/gommehd")
        val tex = ClientAsset.ResourceTexture(id)
        state.skin = PlayerSkin.insecure(tex, tex, tex, PlayerModelType.WIDE)
    }

    override fun getTextureLocation(state: AvatarRenderState): Identifier =
        state.skin.body().id()
}
