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
import be.bluexin.rpg.gui.ClassesGui
import be.bluexin.rpg.skills.SkillData
import be.bluexin.rpg.stats.Stat
import be.bluexin.rpg.util.*
import be.bluexin.saomclib.capabilities.AbstractEntityCapability
import be.bluexin.saomclib.capabilities.Key
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.teamwizardry.librarianlib.features.config.ConfigProperty
import com.teamwizardry.librarianlib.features.kotlin.Minecraft
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

/* TODO: revamp base stats
LordPhrozenToday at 23:15
as for base stats I think each  subclass tree should have a stat associsated with it
dex/str/ etc
and then you earn 1 added in for every tier 1 skill slot
3 for every point in a tier 2
and 5 for every point in a tier 3
 */
@SaveInPlace
class PlayerClassCollection(
    @Save private var skills: MutableMap<ResourceLocation, Int> = HashMap()
) : AbstractEntityCapability(), HasReadCallback {
    @Save("player_class")
    private var _playerClass: Array<ResourceLocation?> = arrayOfNulls(3)
        set(value) {
            field = value
            @Suppress("DEPRECATION")
            playerClasses = arrayOfNulls(3) // clear cache, but keep it lazy
        }

    @Save
    private var selectedSkills: Array<ResourceLocation?> = arrayOfNulls(5)

    @Save
    var skillPoints = 1
        set(value) {
            field = value
            this.sync()
        }

    @Deprecated("Only use trough PlayerClassCollection::get/set !!")
    private var playerClasses: Array<PlayerClass?> = arrayOfNulls(3)

    @Suppress("DEPRECATION")
    operator fun get(index: Int): PlayerClass? = playerClasses[index] ?: _playerClass[index]?.let {
        val c = PlayerClassRegistry[it]
        playerClasses[index] = c
        return c
    }

    @Suppress("DEPRECATION")
    operator fun set(index: Int, playerClass: PlayerClass?) {
        if (playerClass == null || playerClass !in classesSequence) {
            this.playerClasses[index] = playerClass
            this._playerClass[index] = playerClass?.key
            // TODO: event, update base stats... or not?
            // TODO: remove skills from selectedSkills
            this.checkAvailableSkills()
            this.checkSelectedSkills()
            this.sync()
        }
    }

    fun getSelectedSkill(index: Int) = selectedSkills[index]
    fun setSelectedSkill(index: Int, skill: ResourceLocation) {
        if (this[skill] <= 0) return

        for (i in selectedSkills.indices) if (selectedSkills[i] == skill) selectedSkills[i] = null
        selectedSkills[index] = skill
        // TODO: change hotbar skills
        this.sync()
    }

    operator fun contains(playerClass: PlayerClass) = playerClass.key in this
    operator fun contains(playerClass: ResourceLocation) = playerClass in _playerClass

    operator fun get(skill: SkillData) = this[skill.key]
    operator fun set(skill: SkillData, value: Int) {
        this[skill.key] = value
    }

    operator fun get(skill: ResourceLocation): Int = skills.getOrDefault(skill, 0)
    operator fun set(skill: ResourceLocation, value: Int): Boolean {
        if (value < 0 || value > 3) return false
        if (skill !in this.skills && classesSequence.none { it?.skills?.contains(skill) == true }) return false
        // TODO: skill tiers... or not?

        val deltaPoints = value - this[skill]
        if (deltaPoints > skillPoints) return false

        val r = reference.get()
        val evt = if (r is EntityPlayer) {
            SkillChangeEvent(r, skill, this[skill], value)
        } else null

        return if (evt == null || (fire(evt) && evt.result != Event.Result.DENY)) {
            if (evt?.newValue ?: value != 0) skills[skill] = evt?.newValue ?: value
            else skills.remove(skill)
            skillPoints -= deltaPoints
            this.checkSelectedSkills()
            this.sync()
            // TODO: refresh passive
            true
        } else false
    }

    val classesSequence = (0 until 3).asSequence().map(this::get)

    operator fun invoke() = skills.asSequence()

    operator fun iterator(): Iterator<MutableMap.MutableEntry<ResourceLocation, Int>> = skills.iterator()

    private fun checkSelectedSkills() {
        for (i in selectedSkills.indices) if (selectedSkills[i] != null && this[selectedSkills[i]!!] <= 0) selectedSkills[i] =
            null
    }

    private fun checkAvailableSkills() {
        val toRemove = this.skills.entries.filter { (skill, _) ->
            classesSequence.none { it?.skills?.contains(skill) == true }
        }
        toRemove.forEach { this[it.key] = 0 }
    }

    override fun postRead() {
        val ent = reference.get()
        if (ent is EntityPlayer && ent.world.isRemote) {
            val gui = Minecraft().currentScreen
            if (gui is ClassesGui) gui.refresh()
        }
    }

    companion object {
        @Key
        val KEY = ResourceLocation(BlueRPG.MODID, "player_class")

        @CapabilityInject(PlayerClassCollection::class)
        lateinit var Capability: Capability<PlayerClassCollection>
            internal set


        @ConfigProperty(
            category = "general",
            comment = "Allow players to remove selected classes whenever they want to"
        )
        var removeClassWhenever = false

        @ConfigProperty(
            category = "general",
            comment = "Allow players to remove invested skill points whenever they want to"
        )
        var removeSkillsWhenever = false
    }
}

val EntityPlayer.playerClass get() = this.getCapability(PlayerClassCollection.Capability, null)!!
