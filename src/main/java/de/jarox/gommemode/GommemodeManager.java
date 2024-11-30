package de.jarox.gommemode;

import de.jarox.gommemode.entity.GommeEntity;
import de.jarox.gommemode.util.ParticleSpawner;
import lombok.Getter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
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
    private final MinecraftClient client = MinecraftClient.getInstance();
    @Getter
    private boolean active;
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

    public boolean isSoundPlaying() {
        return this.currentSound != null && this.client.getSoundManager().isPlaying(this.currentSound);
    }

    public void toggleActive(ClientPlayerEntity player, ClientWorld world) {
        if (this.active) {
            this.deactivate();
        } else {
            this.activate(player, world);
        }
    }

    public void activate(ClientPlayerEntity player, ClientWorld world) {
        if (this.active) return;
        this.active = true;
        start(player, world);
    }

    public void deactivate() {
        if (!this.active) return;
        this.active = false;
        stop();
    }

    public void start(PlayerEntity player, ClientWorld world) {
        Vec3d lookPos = player.raycast(5, 0, false).getPos();
        BlockPos pos = new BlockPos((int) lookPos.x, (int) lookPos.y, (int) lookPos.z);

        currentSound = new PositionedSoundInstance(
                GommeMode.GOMMEMODE_SOUND_EVENT,
                SoundCategory.MASTER,
                1f,
                1f,
                Random.create(),
                pos);

        this.client.getSoundManager().play(currentSound);

        ParticleSpawner.spawnSphere(world, lookPos.add(0, 0.8, 0), 2, ParticleTypes.FIREWORK, 0.2);

        this.gomme = new GommeEntity(GommeMode.GOMME_ENTITY_TYPE, world);
        world.addEntity(this.gomme);
        this.gomme.setPosition(lookPos);
    }

    public void stop() {
        this.client.getSoundManager().stop(currentSound);
        gomme.remove(Entity.RemovalReason.KILLED);
    }
}
