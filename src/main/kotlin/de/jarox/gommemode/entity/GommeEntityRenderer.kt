package de.jarox.gommemode.entity

import de.jarox.gommemode.Gommemode
import net.minecraft.client.model.HumanoidModel
import net.minecraft.client.model.geom.ModelLayers
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.HumanoidMobRenderer
import net.minecraft.client.renderer.entity.LivingEntityRenderer
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer
import net.minecraft.client.renderer.entity.state.HumanoidRenderState
import net.minecraft.resources.Identifier

class GommeEntityRenderer(
    context: EntityRendererProvider.Context,
) : LivingEntityRenderer<GommeEntity, HumanoidRenderState, HumanoidModel<HumanoidRenderState>>(
        context,
        HumanoidModel(context.bakeLayer(ModelLayers.PLAYER)),
        0.5f,
    ) {
    private val gommeTexture: Identifier =
        Identifier.fromNamespaceAndPath(Gommemode.MOD_ID, "textures/entity/gommehd.png")

    init {
        addLayer(ItemInHandLayer(this))
        addLayer(
            CustomHeadLayer(
                this,
                context.modelSet,
                context.playerSkinRenderCache,
                CustomHeadLayer.Transforms.DEFAULT,
            ),
        )
    }

    override fun createRenderState(): HumanoidRenderState = HumanoidRenderState()

    override fun extractRenderState(
        entity: GommeEntity,
        renderState: HumanoidRenderState,
        partialTick: Float,
    ) {
        super.extractRenderState(entity, renderState, partialTick)
        HumanoidMobRenderer.extractHumanoidRenderState(entity, renderState, partialTick, itemModelResolver)
        renderState.rightArmPose = HumanoidModel.ArmPose.ITEM
    }

    override fun getTextureLocation(renderState: HumanoidRenderState): Identifier = gommeTexture
}
