package de.jarox.gommemode.entity

import de.jarox.gommemode.Gommemode
import de.jarox.gommemode.config.ConfigManager
import net.minecraft.client.model.HumanoidModel
import net.minecraft.client.model.geom.ModelLayers
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.HumanoidMobRenderer
import net.minecraft.client.renderer.entity.LivingEntityRenderer
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer
import net.minecraft.client.renderer.entity.state.HumanoidRenderState
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.HumanoidArm

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
     * and applies the configured arm pose to the main arm (the off arm holds its item, if any).
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

        val config = ConfigManager.config
        // "Holding an item" without an item would just raise an empty hand, so the empty off hand hangs down.
        val offHandEmpty = entity.getItemBySlot(EquipmentSlot.OFFHAND).isEmpty
        val offArmPose =
            if (offHandEmpty && config.offArmPose == HumanoidModel.ArmPose.ITEM) HumanoidModel.ArmPose.EMPTY else config.offArmPose
        if (config.mainArm == HumanoidArm.RIGHT) {
            renderState.rightArmPose = config.mainArmPose
            renderState.leftArmPose = offArmPose
        } else {
            renderState.leftArmPose = config.mainArmPose
            renderState.rightArmPose = offArmPose
        }
    }

    /** The configured shadow radius, scaled with the entity. */
    override fun getShadowRadius(renderState: HumanoidRenderState): Float {
        val radius = ConfigManager.config.shadowRadius.toFloat()
        return radius * renderState.scale
    }

    /** Whether the name tag is enabled in the config, regardless of distance. */
    override fun shouldShowName(
        entity: GommeEntity,
        distanceToCameraSq: Double,
    ): Boolean = ConfigManager.config.showNameTag

    /** The configured name tag text, or the entity name if the text is blank. */
    override fun getNameTag(entity: GommeEntity): Component {
        val text = ConfigManager.config.nameTagText
        return if (text.isBlank()) entity.displayName else Component.literal(text)
    }

    /**
     * Selects the texture to use for rendering the entity.
     *
     * @param renderState The current humanoid render state (unused; texture is constant).
     * @return The Identifier of the texture to use for rendering.
     */
    override fun getTextureLocation(renderState: HumanoidRenderState): Identifier = gommeTexture
}
