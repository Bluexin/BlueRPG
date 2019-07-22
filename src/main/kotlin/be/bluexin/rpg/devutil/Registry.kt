/*
 * Copyright (C) 2019.  Arnaud 'Bluexin' Solé
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package be.bluexin.rpg.devutil

import be.bluexin.rpg.BlueRPG
import net.minecraft.util.ResourceLocation
import net.minecraftforge.registries.*

interface IdAwareForgeRegistryModifiable<V : IForgeRegistryEntry<V>> : IForgeRegistryModifiable<V> {
    fun getId(value: V): Int
    fun getId(key: ResourceLocation): Int
    fun getValue(id: Int): V?
}

/*private*/ class ForgeRegistryWrapper<V : IForgeRegistryEntry<V>>(private val registry: ForgeRegistry<V>) :
    IdAwareForgeRegistryModifiable<V>, IForgeRegistryModifiable<V> by registry {
    override fun getId(value: V): Int = registry.getID(value)
    override fun getId(key: ResourceLocation): Int = registry.getID(key)
    override fun getValue(id: Int): V? = registry.getValue(id)
}

// TODO: allow for callbacks
inline fun <reified V : IForgeRegistryEntry<V>> buildRegistry(name: String): IdAwareForgeRegistryModifiable<V> =
    ForgeRegistryWrapper(
        RegistryBuilder<V>()
            .setName(ResourceLocation(BlueRPG.MODID, name))
            .setType(V::class.java)
            .allowModification()
            .create() as ForgeRegistry<V>
    )

operator fun <V : IForgeRegistryEntry<V>> IForgeRegistry<V>.get(key: ResourceLocation) = this.getValue(key)

operator fun <V : IForgeRegistryEntry<V>> IForgeRegistry<V>.plusAssign(value: V) = this.register(value)

operator fun <V : IForgeRegistryEntry<V>> IForgeRegistryModifiable<V>.minusAssign(value: V) {
    this -= value.registryName!!
}

operator fun <V : IForgeRegistryEntry<V>?> IForgeRegistryModifiable<V>.minusAssign(key: ResourceLocation) {
    this.remove(key)
}
