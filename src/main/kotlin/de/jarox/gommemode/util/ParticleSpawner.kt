package de.jarox.gommemode.util

import net.minecraft.client.world.ClientWorld
import net.minecraft.particle.ParticleEffect
import net.minecraft.util.math.Vec3d
import kotlin.math.cos
import kotlin.math.sin

fun spawnSphere(
    world: ClientWorld,
    center: Vec3d,
    radius: Double,
    particle: ParticleEffect?,
    particleDensity: Double
) {
    var phi = 0.0
    while (phi <= Math.PI) {
        var theta = 0.0
        while (theta <= 2 * Math.PI) {
            val x: Double = center.x + radius * sin(phi) * cos(theta)
            val y: Double = center.y + radius * sin(phi) * sin(theta)
            val z: Double = center.z + radius * cos(phi)
            world.addParticle(particle, x, y, z, 0.0, 0.0, 0.0)
            theta += particleDensity
        }
        phi += particleDensity
    }
}
