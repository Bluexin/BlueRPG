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

package be.bluexin.rpg.util

import be.bluexin.rpg.BlueRPG
import net.minecraft.util.ResourceLocation
import net.minecraftforge.registries.IForgeRegistry
import net.minecraftforge.registries.IForgeRegistryEntry
import net.minecraftforge.registries.IForgeRegistryModifiable
import net.minecraftforge.registries.RegistryBuilder

// TODO: allow for callbacks
inline fun <reified V : IForgeRegistryEntry<V>> buildRegistry(name: String) = RegistryBuilder<V>()
    .setName(ResourceLocation(BlueRPG.MODID, name))
    .setType(V::class.java)
    .allowModification()
    .create() as IForgeRegistryModifiable<V>

operator fun <V : IForgeRegistryEntry<V>> IForgeRegistry<V>.get(key: ResourceLocation) = this.getValue(key)

operator fun <V : IForgeRegistryEntry<V>> IForgeRegistry<V>.plusAssign(value: V) = this.register(value)

operator fun <V : IForgeRegistryEntry<V>> IForgeRegistryModifiable<V>.minusAssign(value: V) {
    this -= value.registryName!!
}

operator fun <V : IForgeRegistryEntry<V>?> IForgeRegistryModifiable<V>.minusAssign(key: ResourceLocation) {
    this.remove(key)
}
