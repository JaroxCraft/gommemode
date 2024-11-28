package de.jarox.gommemode.entity;

import net.minecraft.client.MinecraftClient;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Arm;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class GommeEntity extends LivingEntity {

    public GommeEntity(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public Iterable<ItemStack> getArmorItems() {
        return null;
    }

    @Override
    public ItemStack getEquippedStack(EquipmentSlot slot) {
        return new ItemStack(Items.DIAMOND_SWORD);
    }

    @Override
    public void equipStack(EquipmentSlot slot, ItemStack stack) {
    }

    @Override
    public Arm getMainArm() {
        return Arm.RIGHT;
    }

    @Override
    public void baseTick() {
        super.baseTick();
        lookAtPlayer();
    }

    private void lookAtPlayer() {
        if (this.getWorld().isClient) {
            assert MinecraftClient.getInstance().player != null;
            Vec3d playerPos = MinecraftClient.getInstance().player.getPos();
            this.lookAt(EntityAnchorArgumentType.EntityAnchor.FEET, playerPos);
        }
    }
}
