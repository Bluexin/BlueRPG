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
import be.bluexin.rpg.events.SkillChangeEvent
import be.bluexin.rpg.skills.SkillData
import be.bluexin.rpg.stats.Stat
import be.bluexin.rpg.util.*
import be.bluexin.saomclib.capabilities.AbstractEntityCapability
import be.bluexin.saomclib.capabilities.Key
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.teamwizardry.librarianlib.features.saving.Save
import com.teamwizardry.librarianlib.features.saving.SaveInPlace
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.ResourceLocation
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.CapabilityInject
import net.minecraftforge.fml.common.eventhandler.Event
import net.minecraftforge.registries.IForgeRegistryEntry
import net.minecraftforge.registries.IForgeRegistryModifiable
import java.io.File
import java.util.*
import kotlin.collections.set

object PlayerClassRegistry : IForgeRegistryModifiable<PlayerClass> by buildRegistry("player_classes") {
    // TODO: set missing callback to return dummy value to be replaced by network loading

    private lateinit var savefile: File

    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(Stat::class.java, StatDeserializer)
        .registerTypeAdapter(ResourceLocation::class.java, ResourceLocationSerde)
        .create()

    val allClassesStrings by lazy { keys.map(ResourceLocation::toString).toTypedArray() }

    fun setupDataDir(dir: File) {
        this.loadDirectoryLayout(dir)
    }

    private fun loadDirectoryLayout(dir: File) {
        if ((!dir.exists() && !dir.mkdirs()) || !dir.isDirectory) throw IllegalStateException("$dir exists but is not a directory")
        savefile = File(dir, "playerclassregistry.json")
    }

    fun load() { // TODO: this should be changed to registry event
        try {
            if (savefile.exists()) savefile.reader().use {
                this.clear()
                gson.fromJson<List<PlayerClass.SerData>>(
                    it,
                    object : TypeToken<List<PlayerClass.SerData>>() {}.type
                )
            }.map(PlayerClass.SerData::mapped).forEach(::register)
            else savefile.writer().use {
                gson.toJson(valuesCollection.map(PlayerClass::mapped), it)
            }
        } catch (e: Exception) {
            BlueRPG.LOGGER.error("Couldn't read Player Class registry", Exception(e))
        }
    }
}

data class PlayerClass(
    val key: ResourceLocation,
    val skills: Map<ResourceLocation, Int>,
    val baseStats: Map<Stat, Int>
) : IForgeRegistryEntry.Impl<PlayerClass>() {
    init {
        this.registryName = key
    }

    val mapped get() = SerData(key, skills, baseStats)

    data class SerData(
        val key: ResourceLocation,
        val skills: Map<ResourceLocation, Int>,
        val baseStats: Map<Stat, Int>
    ) {
        val mapped get() = PlayerClass(key, skills, baseStats)
    }
}

@SaveInPlace
class PlayerClassCollection(
    @Save private var skills: MutableMap<ResourceLocation, Int> = HashMap()
) : AbstractEntityCapability() {
    @Save("player_class")
    private var _playerClass: Array<ResourceLocation?> = arrayOfNulls(3)

    @Deprecated("Only use trough PlayerClassCollection::get/set !!")
    private val playerClasses: Array<PlayerClass?> = arrayOfNulls(3)

    @Suppress("DEPRECATION")
    operator fun get(index: Int) = playerClasses[index] ?: _playerClass[index]?.let(PlayerClassRegistry::get)

    @Suppress("DEPRECATION")
    operator fun set(index: Int, playerClass: PlayerClass?) {
        this.playerClasses[index] = playerClass
        this._playerClass[index] = playerClass?.key
        // TODO: reset skills, base stats, ...
    }

    operator fun get(skill: SkillData) = this[skill.key]
    operator fun set(skill: SkillData, value: Int) {
        this[skill.key] = value
    }

    operator fun get(skill: ResourceLocation): Int = skills.getOrDefault(skill, 0)
    operator fun set(skill: ResourceLocation, value: Int): Boolean {
        val r = reference.get()
        val evt = if (r is EntityPlayer) {
            SkillChangeEvent(r, skill, this[skill], value)
        } else null

        return if (evt == null || (fire(evt) && evt.result != Event.Result.DENY)) {
            if (evt?.newValue ?: value != 0) skills[skill] = evt?.newValue ?: value
            else skills.remove(skill)
            // TODO: refresh passive
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

val EntityPlayer.playerClass get() = this.getCapability(PlayerClassCollection.Capability, null)!!
