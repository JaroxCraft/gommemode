package de.jarox.gommemode;

import de.jarox.gommemode.entity.GommeEntity;
import de.jarox.gommemode.util.ParticleSpawner;
import lombok.Getter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.toast.ToastManager;
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
    private final ToastManager toastManager = this.client.getToastManager();
    private final SoundManager soundManager = this.client.getSoundManager();
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

    public void setActive(boolean active) {
        // value wasn't changed
        if (this.active == active) return;

        // turned off
        if (!active) {
            stop();
        }

        this.active = active;
    }

    public void updateActive() {
        this.setActive(soundManager.isPlaying(currentSound));
    }

    public void start(PlayerEntity player, ClientWorld world) {
        this.toastManager.add(new GommemodeToast(true));

        Vec3d lookPos = player.raycast(5, 0, false).getPos();
        BlockPos pos = new BlockPos((int) lookPos.x, (int) lookPos.y, (int) lookPos.z);

        currentSound = new PositionedSoundInstance(
                GommeMode.GOMMEMODE_SOUND_EVENT,
                SoundCategory.MASTER,
                1f,
                1f,
                Random.create(),
                pos);

        this.setActive(true);

        this.soundManager.play(currentSound);

        ParticleSpawner.spawnSphere(world, lookPos.add(0, 0.8, 0), 2, ParticleTypes.FIREWORK, 0.2);

        this.gomme = new GommeEntity(GommeMode.GOMME_ENTITY_TYPE, world);
        world.addEntity(this.gomme);
        this.gomme.setPosition(lookPos);
    }

    public void stop() {
        this.toastManager.add(new GommemodeToast(false));

        this.soundManager.stop(currentSound);
        gomme.remove(Entity.RemovalReason.KILLED);
    }
}
