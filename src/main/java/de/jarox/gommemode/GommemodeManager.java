package de.jarox.gommemode;

import de.jarox.gommemode.entity.GommeEntity;
import de.jarox.gommemode.util.ParticleSpawner;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

public class GommemodeManager {
    private static GommemodeManager instance;
    private SoundInstance currentSound;
    private GommeEntity gomme;

    private GommemodeManager() {
    }

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
        MinecraftClient client = MinecraftClient.getInstance();

        Vec3d lookPos = player.raycast(5, 0, false).getPos();
        BlockPos pos = new BlockPos((int) lookPos.x, (int) lookPos.y, (int) lookPos.z);

        currentSound = new PositionedSoundInstance(
                GommeMode.GOMMEMODE_SOUND_EVENT,
                SoundCategory.MASTER,
                1f,
                1f,
                Random.create(),
                pos);
        client.getSoundManager().play(currentSound);

        ParticleSpawner.spawnSphere(world, lookPos.add(0, 0.8, 0), 2, ParticleTypes.FIREWORK, 0.2);

        gomme = new GommeEntity(GommeMode.GOMME_ENTITY_TYPE, world);
        world.addEntity(gomme);
        gomme.setPosition(lookPos);
    }

    public void stop() {
        MinecraftClient.getInstance().getSoundManager().stop(currentSound);
        gomme.remove(Entity.RemovalReason.KILLED);
    }
}
