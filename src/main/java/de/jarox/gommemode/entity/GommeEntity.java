package de.jarox.gommemode.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class GommeEntity extends LivingEntity {

    private float danceTicks = 0f;

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

        danceTicks += 0.1f;

        float danceHeight = MathHelper.sin(danceTicks) * 0.1f;
        float danceHorizontal = MathHelper.cos(danceTicks * 0.5f) * 0.05f;

        this.setPosition(
                this.getX() + danceHorizontal,
                this.getY() + danceHeight,
                this.getZ() + danceHorizontal
        );

        this.setYaw(this.getYaw() + 5f);
        this.setHeadYaw(this.getHeadYaw() + 5f);
    }
}
