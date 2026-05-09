package de.jarox.gommemode.util

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.world.phys.Vec3
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Spawns particles in a spherical pattern around the given center point.
 *
 * @param level The client world to spawn particles in
 * @param center The center of the sphere
 * @param radius The radius of the sphere
 * @particleType The type of particle to spawn
 * @stepSize Angular step size in radians; smaller values produce denser particle distributions
 */
fun spawnSphere(
    level: ClientLevel,
    center: Vec3,
    radius: Double,
    particleType: ParticleOptions,
    stepSize: Double,
) {
    require(stepSize.isFinite() && stepSize > 0.0) { "stepSize must be finite and > 0, was: $stepSize" }
    val phiSteps = (PI / stepSize).toInt().coerceAtLeast(1)
    val thetaSteps = (2 * PI / stepSize).toInt().coerceAtLeast(1)

    for (phiIndex in 0..phiSteps) {
        val phi = phiIndex * stepSize
        for (thetaIndex in 0..thetaSteps) {
            val theta = thetaIndex * stepSize

            val x = center.x + radius * sin(phi) * cos(theta)
            val y = center.y + radius * sin(phi) * sin(theta)
            val z = center.z + radius * cos(phi)

            level.addParticle(particleType, x, y, z, 0.0, 0.0, 0.0)
        }
    }
}
