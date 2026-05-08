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

class GommeEntity(entityType: EntityType<out LivingEntity>, world: Level) : LivingEntity(entityType, world) {
    override fun getItemBySlot(slot: EquipmentSlot): ItemStack = ItemStack(Items.DIAMOND_SWORD)
    override fun setItemSlot(slot: EquipmentSlot, stack: ItemStack) {}
    override fun getMainArm(): HumanoidArm = HumanoidArm.RIGHT

    override fun baseTick() {
        super.baseTick()
        lookAtPlayer()
    }

    private fun lookAtPlayer() {
        if (level().isClientSide) {
            val player = Minecraft.getInstance().player ?: return
            this.lookAt(EntityAnchorArgument.Anchor.FEET, player.position())
        }
    }
}
