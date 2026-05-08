package de.jarox.gommemode.util

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.world.phys.Vec3
import kotlin.math.cos
import kotlin.math.sin

fun spawnSphere(
    world: ClientLevel,
    center: Vec3,
    radius: Double,
    particle: ParticleOptions,
    particleDensity: Double
) {
    var phi = 0.0
    while (phi <= Math.PI) {
        var theta = 0.0
        while (theta <= 2 * Math.PI) {
            val x = center.x + radius * sin(phi) * cos(theta)
            val y = center.y + radius * sin(phi) * sin(theta)
            val z = center.z + radius * cos(phi)
            world.addParticle(particle, x, y, z, 0.0, 0.0, 0.0)
            theta += particleDensity
        }
        phi += particleDensity
    }
}
