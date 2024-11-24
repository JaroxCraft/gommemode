package de.jarox.gommemode.util;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.util.math.Vec3d;

public final class ParticleSpawner {

    public static void spawnSphere(ClientWorld world, Vec3d center, double radius, ParticleEffect particle, double particleDensity) {
        for (double phi = 0; phi <= Math.PI; phi += particleDensity) {
            for (double theta = 0; theta <= 2 * Math.PI; theta += particleDensity) {
                double x = center.x + radius * Math.sin(phi) * Math.cos(theta);
                double y = center.y + radius * Math.sin(phi) * Math.sin(theta);
                double z = center.z + radius * Math.cos(phi);
                world.addParticle(particle, x, y, z, 0d, 0d, 0d);
            }
        }
    }
}
