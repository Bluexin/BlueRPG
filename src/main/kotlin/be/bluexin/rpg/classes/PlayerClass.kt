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

package be.bluexin.rpg.classes

import be.bluexin.rpg.BlueRPG
import be.bluexin.rpg.stats.Stat
import be.bluexin.rpg.util.buildRegistry
import be.bluexin.saomclib.capabilities.AbstractEntityCapability
import be.bluexin.saomclib.capabilities.Key
import com.teamwizardry.librarianlib.features.saving.Save
import com.teamwizardry.librarianlib.features.saving.SaveInPlace
import net.minecraft.util.ResourceLocation
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.CapabilityInject
import net.minecraftforge.registries.IForgeRegistryEntry
import net.minecraftforge.registries.IForgeRegistryModifiable
import java.util.*

object PlayerClassRegistry : IForgeRegistryModifiable<PlayerClass> by buildRegistry("player_classes") {
    // TODO: set missing callback to return dummy value to be replaced by network loading

    val allClassesStrings by lazy { keys.map(ResourceLocation::toString).toTypedArray() }
}

data class PlayerClass(
    val key: ResourceLocation,
    val skills: List<ResourceLocation>,
    val baseStats: Map<Stat, Int>
) : IForgeRegistryEntry.Impl<PlayerClass>() {
    init {
        this.registryName = key
    }
}

@SaveInPlace
class PlayerClassCollection(
    @Save internal var skills: MutableMap<ResourceLocation, Int> = HashMap()
) : AbstractEntityCapability() {
    @Save("player_class")
    internal var _playerClass = ResourceLocation(BlueRPG.MODID, "unknown_class")
    // TODO: actual playerClass var

    operator fun get(stat: ResourceLocation): Int = skills.getOrDefault(stat, 0)
    operator fun set(stat: ResourceLocation, value: Int): Boolean {
        val r = reference.get()
        val evt = if (r != null) {
            // fire event
        } else null

        return if (evt == null || evt == Unit/*(fire(evt) && evt.result != Event.Result.DENY)*/) {
            if (/*evt?.newValue ?: */value != 0) skills[stat] = /*evt?.newValue ?:*/ value
            else skills.remove(stat)
            /*if (r is EntityPlayer) {
                r.getEntityAttribute(stat.attribute).baseValue = evt!!.newValue.toDouble()
            }*/
            dirty()
            true
        } else false
    }

    operator fun invoke() = skills.asSequence()

    operator fun iterator(): Iterator<MutableMap.MutableEntry<ResourceLocation, Int>> = skills.iterator()

    fun load(other: PlayerClassCollection) {
        this.skills = other.skills
    }

    internal var dirty = false
        private set

    internal fun clean() {
        dirty = false
    }

    internal fun dirty() {
        dirty = true
    }

    fun clear() = skills.clear()

    fun isEmpty() = skills.isEmpty()

    companion object {
        @Key
        val KEY = ResourceLocation(BlueRPG.MODID, "player_class")

        @CapabilityInject(PlayerClassCollection::class)
        lateinit var Capability: Capability<PlayerClassCollection>
            internal set
    }
}