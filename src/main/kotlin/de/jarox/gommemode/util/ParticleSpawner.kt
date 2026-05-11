package de.jarox.gommemode.util

import de.jarox.gommemode.Gommemode
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

    val maxSteps = 360
    val particleBudget = 10000

    val phiStepsRaw = (PI / stepSize).toInt().coerceAtLeast(1)
    val thetaStepsRaw = (2 * PI / stepSize).toInt().coerceAtLeast(1)

    var phiSteps = minOf(phiStepsRaw, maxSteps)
    var thetaSteps = minOf(thetaStepsRaw, maxSteps)

    var totalParticles = (phiSteps + 1) * (thetaSteps + 1)
    if (totalParticles > particleBudget) {
        val scale = kotlin.math.sqrt(particleBudget.toDouble() / totalParticles)
        phiSteps = (phiSteps * scale).toInt().coerceAtLeast(1)
        thetaSteps = (thetaSteps * scale).toInt().coerceAtLeast(1)
        totalParticles = (phiSteps + 1) * (thetaSteps + 1)
    }

    Gommemode.LOGGER.debug("Spawning {} particles in sphere at ({}, {}, {})", totalParticles, center.x, center.y, center.z)

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
