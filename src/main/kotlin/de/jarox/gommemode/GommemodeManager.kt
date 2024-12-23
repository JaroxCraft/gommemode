package de.jarox.gommemode

import de.jarox.gommemode.entity.GommeEntity
import de.jarox.gommemode.util.spawnSphere
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.client.sound.PositionedSoundInstance
import net.minecraft.client.sound.SoundInstance
import net.minecraft.client.world.ClientWorld
import net.minecraft.entity.Entity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.particle.ParticleTypes
import net.minecraft.sound.SoundCategory
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.random.Random

object GommemodeManager {
    private val client: MinecraftClient = MinecraftClient.getInstance()
    private var currentSound: SoundInstance? = null
    private var gomme: GommeEntity? = null

    var active = false
        private set
        get() = currentSound != null && client.soundManager.isPlaying(currentSound)

    fun toggleActive(player: ClientPlayerEntity, world: ClientWorld) {
        if (active) deactivate() else activate(player, world)
    }

    private fun activate(player: ClientPlayerEntity, world: ClientWorld) {
        if (active) return
        active = true
        start(player, world)
    }

    private fun deactivate() {
        if (!active) return
        active = false
        stop()
    }

    private fun start(player: PlayerEntity, world: ClientWorld) {
        val lookPos = player.raycast(5.0, 0f, false).pos
        val pos = BlockPos(lookPos.x.toInt(), lookPos.y.toInt(), lookPos.z.toInt())

        currentSound = PositionedSoundInstance(
            GommeMode.GOMMEMODE_SOUND_EVENT,
            SoundCategory.MASTER,
            1f,
            1f,
            Random.create(),
            pos
        )

        client.soundManager.play(currentSound)
        spawnSphere(world, lookPos.add(0.0, 0.8, 0.0), 2.0, ParticleTypes.FIREWORK, 0.2)

        gomme = GommeEntity(GommeMode.GOMME_ENTITY_TYPE, world).apply {
            setPosition(lookPos)
            world.addEntity(this)
        }
    }

    private fun stop() {
        client.soundManager.stop(currentSound)
        gomme?.remove(Entity.RemovalReason.KILLED)
    }
}
