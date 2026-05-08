package de.jarox.gommemode

import de.jarox.gommemode.entity.GommeEntity
import de.jarox.gommemode.util.spawnSphere
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.LocalPlayer
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.client.resources.sounds.SoundInstance
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.sounds.SoundSource
import net.minecraft.util.RandomSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3

object GommemodeManager {
    private val client: Minecraft = Minecraft.getInstance()
    private var currentSound: SoundInstance? = null
    private var gomme: GommeEntity? = null
    private var lastToggle: Long = 0

    var active = false
        private set

    fun isPlaying(): Boolean {
        val sound = currentSound
        return sound != null && client.soundManager.isActive(sound)
    }

    fun toggleActive(player: LocalPlayer, world: ClientLevel) {
        if (lastToggle + (1 * 20) > world.gameTime) return
        if (active) deactivate() else activate(player, world)
        lastToggle = world.gameTime
    }

    private fun activate(player: LocalPlayer, world: ClientLevel) {
        if (active) return
        active = true
        start(player, world)
    }

    private fun deactivate() {
        if (!active) return
        active = false
        stop()
    }

    private fun start(player: Player, world: ClientLevel) {
        val lookPos = player.pick(5.0, 0f, false).location
        val pos = BlockPos(lookPos.x.toInt(), lookPos.y.toInt(), lookPos.z.toInt())

        currentSound = SimpleSoundInstance(
            Gommemode.GOMMEMODE_SOUND_EVENT.location(),
            SoundSource.MASTER,
            1f,
            1f,
            RandomSource.create(),
            false,
            0,
            SoundInstance.Attenuation.LINEAR,
            pos.x + 0.5,
            pos.y + 0.5,
            pos.z + 0.5,
            false
        )

        client.soundManager.play(currentSound!!)
        spawnSphere(world, lookPos.add(0.0, 0.8, 0.0), 2.0, ParticleTypes.FIREWORK, 0.2)

        gomme = GommeEntity(Gommemode.GOMME_ENTITY_TYPE, world).apply {
            setPos(lookPos.x, lookPos.y, lookPos.z)
            world.addEntity(this)
        }
    }

    private fun stop() {
        client.soundManager.stop(currentSound!!)
        gomme?.remove(Entity.RemovalReason.KILLED)
    }
}
