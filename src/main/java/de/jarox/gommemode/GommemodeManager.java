package de.jarox.gommemode;

import de.jarox.gommemode.util.ParticleSpawner;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;

public class GommemodeManager {
    private static GommemodeManager instance;
    private SoundInstance currentSound;

    private GommemodeManager() {}

    public static GommemodeManager getInstance() {
        if (instance == null) {
            instance = new GommemodeManager();
        }
        return instance;
    }

    public boolean isActive() {
        return MinecraftClient.getInstance().getSoundManager().isPlaying(currentSound);
    }

    public void start(PlayerEntity player, ClientWorld world) {
        BlockPos pos = player.getBlockPos();
        MinecraftClient client = MinecraftClient.getInstance();

        currentSound = new PositionedSoundInstance(
                GommeMode.GOMMEMODE_SOUND_EVENT,
                SoundCategory.MASTER,
                1f,
                1f,
                Random.create(),
                pos);
        client.getSoundManager().play(currentSound);

        world.addParticle(ParticleTypes.ELDER_GUARDIAN, pos.getX(), pos.getY(), pos.getZ(), 0, 0, 0);
        ParticleSpawner.spawnSphere(world, Vec3d.of(pos), 3, ParticleTypes.ENCHANT, 0.1);

        TntEntity tntEntity = new TntEntity(EntityType.TNT, world);
        world.createExplosion(
                tntEntity,
                Explosion.createDamageSource(world, tntEntity),
                null,
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                4.0F,
                true,
                World.ExplosionSourceType.TNT
        );
    }

    public void stop() {
        MinecraftClient.getInstance().getSoundManager().stop(currentSound);
    }
}