package de.jarox.gommemode.entity

import com.mojang.blaze3d.vertex.PoseStack
import de.jarox.gommemode.Gommemode
import de.jarox.gommemode.config.ConfigManager
import net.minecraft.client.model.HumanoidModel
import net.minecraft.client.model.geom.ModelLayers
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.HumanoidMobRenderer
import net.minecraft.client.renderer.entity.LivingEntityRenderer
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer
import net.minecraft.client.renderer.entity.state.HumanoidRenderState
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.HumanoidArm
import net.minecraft.world.phys.Vec3

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

    /** A new, empty [HumanoidRenderState] for this renderer to fill. */
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
        val mainIsRight = config.mainArm == HumanoidArm.RIGHT
        renderState.rightArmPose = if (mainIsRight) config.mainArmPose else offArmPose
        renderState.leftArmPose = if (mainIsRight) offArmPose else config.mainArmPose

        // Vanilla places the name tag above the 1.8 block hitbox, but the unshrunk player model is 2 blocks tall,
        // so the bigger the scale, the deeper the tag sinks into the head. The tag sits above the model instead;
        // the attachment is unscaled because submitNameDisplay scales the whole tag.
        if (renderState.nameTagAttachment != null) renderState.nameTagAttachment = Vec3(0.0, MODEL_HEIGHT, 0.0)
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

    /**
     * Draws the name tag scaled with the entity: its height above the head, its gap to the head and its text size.
     */
    override fun submitNameDisplay(
        renderState: HumanoidRenderState,
        poseStack: PoseStack,
        submitNodeCollector: SubmitNodeCollector,
        camera: CameraRenderState,
    ) {
        poseStack.pushPose()
        poseStack.scale(renderState.scale, renderState.scale, renderState.scale)
        super.submitNameDisplay(renderState, poseStack, submitNodeCollector, camera)
        poseStack.popPose()
    }

    /** The configured name tag text, or the entity name if the text is blank. */
    override fun getNameTag(entity: GommeEntity): Component {
        val text = ConfigManager.config.nameTagText
        return if (text.isBlank()) entity.displayName else Component.literal(text)
    }

    /** The GommeHD skin; the same for every render state. */
    override fun getTextureLocation(renderState: HumanoidRenderState): Identifier = gommeTexture

    private companion object {
        /** Height of the player model at scale 1 (32 pixels; vanilla players shrink it to 0.9375, Gomme does not). */
        const val MODEL_HEIGHT: Double = 2.0
    }
}
