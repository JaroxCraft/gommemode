package de.jarox.gommemode.entity

import net.minecraft.client.MinecraftClient
import net.minecraft.command.argument.EntityAnchorArgumentType
import net.minecraft.entity.EntityType
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.LivingEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.util.Arm
import net.minecraft.world.World

class GommeEntity(entityType: EntityType<out LivingEntity?>, world: World?) : LivingEntity(entityType, world) {
    override fun getArmorItems(): Iterable<ItemStack>? {
        return null
    }

    override fun getEquippedStack(slot: EquipmentSlot): ItemStack {
        return ItemStack(Items.DIAMOND_SWORD)
    }

    override fun equipStack(slot: EquipmentSlot, stack: ItemStack) {
    }

    override fun getMainArm(): Arm {
        return Arm.RIGHT
    }

    override fun baseTick() {
        super.baseTick()
        lookAtPlayer()
    }

    private fun lookAtPlayer() {
        if (world.isClient) {
            checkNotNull(MinecraftClient.getInstance().player)
            val playerPos = MinecraftClient.getInstance().player!!.pos
            this.lookAt(EntityAnchorArgumentType.EntityAnchor.FEET, playerPos)
        }
    }
}
