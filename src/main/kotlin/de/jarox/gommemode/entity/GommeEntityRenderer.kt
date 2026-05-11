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
        Gommemode.LOGGER.debug("Registering GommeEntityRenderer with item-in-hand and custom head layers")
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

    /**
     * Create a new render state holder for this renderer.
     *
     * @return A new, default-initialized {@link HumanoidRenderState} instance.
     */
    override fun createRenderState(): HumanoidRenderState = HumanoidRenderState()

    /**
     * Populate the humanoid render state for the given entity at the specified partial tick.
     *
     * Updates `renderState` with humanoid-specific animation, equipment and model information,
     * and sets the right arm pose to `HumanoidModel.ArmPose.ITEM`.
     *
     * @param entity The GommeEntity being rendered.
     * @param renderState The mutable HumanoidRenderState to populate.
     * @param partialTick Fractional tick time used for interpolation of animations.
     */
    override fun extractRenderState(
        entity: GommeEntity,
        renderState: HumanoidRenderState,
        partialTick: Float,
    ) {
        super.extractRenderState(entity, renderState, partialTick)
        HumanoidMobRenderer.extractHumanoidRenderState(entity, renderState, partialTick, itemModelResolver)
        renderState.rightArmPose = HumanoidModel.ArmPose.ITEM
    }

    /**
     * Selects the texture to use for rendering the entity.
     *
     * @param renderState The current humanoid render state (unused; texture is constant).
     * @return The Identifier of the texture to use for rendering.
     */
    override fun getTextureLocation(renderState: HumanoidRenderState): Identifier = gommeTexture
}
