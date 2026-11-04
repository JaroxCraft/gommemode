package de.jarox.gommemode.entity

import de.jarox.gommemode.Gommemode
import de.jarox.gommemode.config.ConfigManager
import de.jarox.gommemode.config.LookTarget
import net.minecraft.client.Minecraft
import net.minecraft.commands.arguments.EntityAnchorArgument
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.HumanoidArm
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level

/**
 * A custom living entity that holds the configured items and faces the local player.
 *
 * Its appearance (held items, main arm, scale, glow) is read from the config every tick,
 * so changes made in the config screen apply to an already spawned entity.
 *
 * @param entityType The entity type for this entity
 * @param level The world this entity exists in
 */
class GommeEntity(
    entityType: EntityType<out LivingEntity>,
    level: Level,
) : LivingEntity(entityType, level) {
    private val mainHand = CachedItem()
    private val offHand = CachedItem()

    init {
        Gommemode.LOGGER.debug("GommeEntity created at ({})", position())
        applyScale()
    }

    /**
     * Provide the entity's equipped item for the given equipment slot.
     *
     * The main and off hand return the items configured in the config; all other slots are empty.
     */
    override fun getItemBySlot(slot: EquipmentSlot): ItemStack =
        when (slot) {
            EquipmentSlot.MAINHAND -> mainHand.get(ConfigManager.config.mainHandItem)
            EquipmentSlot.OFFHAND -> offHand.get(ConfigManager.config.offHandItem)
            else -> ItemStack.EMPTY
        }

    override fun setItemSlot(
        slot: EquipmentSlot,
        stack: ItemStack,
    ) {
        // Intentionally no-op: this entity's equipment is managed by getItemBySlot
    }

    /** The configured main arm. */
    override fun getMainArm(): HumanoidArm = ConfigManager.config.mainArm

    /** Whether the glowing outline is enabled in the config. */
    override fun isCurrentlyGlowing(): Boolean = ConfigManager.config.glowing

    /** The configured outline color. */
    override fun getTeamColor(): Int = ConfigManager.config.glowColor

    /** Applies the configured scale and turns towards the player every tick. */
    override fun baseTick() {
        super.baseTick()
        applyScale()
        faceLocalPlayer()
    }

    /** Sets the `minecraft:scale` attribute to the configured scale when it changed. */
    private fun applyScale() {
        val attribute = getAttribute(Attributes.SCALE) ?: return
        val scale = ConfigManager.config.scale
        if (attribute.baseValue != scale) attribute.baseValue = scale
    }

    /**
     * Rotates this entity to face the local player, if enabled in the config.
     * Only executes on the client side where the player reference is available.
     */
    private fun faceLocalPlayer() {
        if (!level().isClientSide) return
        val config = ConfigManager.config
        if (!config.lookAtPlayer) return

        val player = Minecraft.getInstance().player ?: return
        when (config.lookTarget) {
            LookTarget.FEET -> lookAt(EntityAnchorArgument.Anchor.FEET, player.position())
            LookTarget.EYES -> lookAt(EntityAnchorArgument.Anchor.EYES, player.eyePosition)
        }
    }

    /** Resolves an item id to an [ItemStack], re-resolving only when the id changes. */
    private class CachedItem {
        private var id: String? = null
        private var stack: ItemStack = ItemStack.EMPTY

        /** The stack for [newId], resolved again only when the id changed. */
        fun get(newId: String): ItemStack {
            if (newId != id) {
                id = newId
                stack = resolveItem(newId)
            }
            return stack
        }
    }

    companion object {
        /**
         * Resolves an item id such as `minecraft:diamond_sword` to a stack.
         * Blank or unknown ids yield an empty stack.
         */
        fun resolveItem(id: String): ItemStack = findItem(id)?.let(::ItemStack) ?: ItemStack.EMPTY

        /**
         * Looks up an item id such as `minecraft:diamond_sword` in the item registry.
         * Blank ids, unknown ids and air yield `null`.
         *
         * Unlike [resolveItem], this creates no [ItemStack], so it also works outside a world
         * (e.g. in the config screen), where item components are not bound yet.
         */
        fun findItem(id: String): Item? {
            if (id.isBlank()) return null
            val item =
                Identifier
                    .tryParse(id.trim())
                    ?.let { BuiltInRegistries.ITEM.getOptional(it).orElse(null) }
            return item?.takeIf { it != Items.AIR }
        }
    }
}
