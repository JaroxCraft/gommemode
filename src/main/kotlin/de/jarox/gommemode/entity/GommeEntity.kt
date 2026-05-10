package de.jarox.gommemode.entity

import net.minecraft.client.Minecraft
import net.minecraft.commands.arguments.EntityAnchorArgument
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.HumanoidArm
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level

/**
 * A custom living entity that always holds a diamond sword and faces the local player.
 *
 * @param entityType The entity type for this entity
 * @param level The world this entity exists in
 */
class GommeEntity(
    entityType: EntityType<out LivingEntity>,
    level: Level,
) : LivingEntity(entityType, level) {
    private val diamondSword: ItemStack = ItemStack(Items.DIAMOND_SWORD)

    /**
         * Provide the entity's equipped item for the given equipment slot.
         *
         * Only the main hand returns the entity's diamond sword; all other slots return `ItemStack.EMPTY`.
         *
         * @param slot The equipment slot to query.
         * @return `diamondSword` when `slot` is `EquipmentSlot.MAINHAND`, `ItemStack.EMPTY` otherwise.
         */
        override fun getItemBySlot(slot: EquipmentSlot): ItemStack =
        when (slot) {
            EquipmentSlot.MAINHAND -> diamondSword
            else -> ItemStack.EMPTY
        }

    override fun setItemSlot(
        slot: EquipmentSlot,
        stack: ItemStack,
    ) {
        // Intentionally no-op: this entity's equipment is managed by getItemBySlot
    }

    override fun getMainArm(): HumanoidArm = HumanoidArm.RIGHT

    override fun baseTick() {
        super.baseTick()
        faceLocalPlayer()
    }

    /**
     * Rotates this entity to face the local player's feet position.
     * Only executes on the client side where the player reference is available.
     */
    private fun faceLocalPlayer() {
        if (!level().isClientSide) return

        val player = Minecraft.getInstance().player ?: return
        lookAt(EntityAnchorArgument.Anchor.FEET, player.position())
    }
}
