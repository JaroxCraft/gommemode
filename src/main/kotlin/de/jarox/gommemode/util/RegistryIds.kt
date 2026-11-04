package de.jarox.gommemode.util

import net.minecraft.core.Registry
import net.minecraft.core.particles.ParticleType
import net.minecraft.core.particles.SimpleParticleType
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items

/**
 * Looks up the item and particle ids stored in the config (e.g. `minecraft:diamond_sword`)
 * in the game registries. Ids are trimmed before the lookup.
 *
 * Only registry entries are returned, never item stacks, so these functions also work outside
 * a world (e.g. in the config screen), where item components are not bound yet.
 */
object RegistryIds {
    /** The item for [id], or `null` for blank ids, unknown ids and air. */
    fun item(id: String): Item? {
        if (id.isBlank()) return null
        return lookup(BuiltInRegistries.ITEM, id)?.takeIf { it != Items.AIR }
    }

    /** Whether [id] names air, which cannot be held. */
    fun isAir(id: String): Boolean = Identifier.tryParse(id.trim()) == BuiltInRegistries.ITEM.getKey(Items.AIR)

    /** The particle type for [id], or `null` if the id is unknown. */
    fun particleType(id: String): ParticleType<*>? = lookup(BuiltInRegistries.PARTICLE_TYPE, id)

    /** The particle for [id] if it can be spawned without extra parameters, otherwise `null`. */
    fun simpleParticle(id: String): SimpleParticleType? = particleType(id) as? SimpleParticleType

    /** All item ids except air, sorted. */
    fun itemIds(): List<String> =
        BuiltInRegistries.ITEM
            .keySet()
            .filter { it != BuiltInRegistries.ITEM.getKey(Items.AIR) }
            .map(Identifier::toString)
            .sorted()

    /** All ids of particles that can be spawned without extra parameters, sorted. */
    fun simpleParticleIds(): List<String> =
        BuiltInRegistries.PARTICLE_TYPE
            .keySet()
            .filter { BuiltInRegistries.PARTICLE_TYPE.getValue(it) is SimpleParticleType }
            .map(Identifier::toString)
            .sorted()

    /** The entry of [registry] for [id], or `null` if the id is invalid or unknown. */
    private fun <T : Any> lookup(
        registry: Registry<T>,
        id: String,
    ): T? = Identifier.tryParse(id.trim())?.let { registry.getOptional(it).orElse(null) }
}
