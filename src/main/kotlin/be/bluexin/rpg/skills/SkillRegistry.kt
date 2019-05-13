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

package be.bluexin.rpg.skills

import be.bluexin.rpg.BlueRPG
import be.bluexin.rpg.gear.GearType
import be.bluexin.rpg.gear.Rarity
import be.bluexin.rpg.stats.Stat
import be.bluexin.rpg.util.ResourceLocationSerde
import be.bluexin.rpg.util.StatDeserializer
import be.bluexin.rpg.util.buildRegistry
import com.google.gson.GsonBuilder
import com.google.gson.annotations.Expose
import com.google.gson.reflect.TypeToken
import com.teamwizardry.librarianlib.features.saving.NamedDynamic
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.ai.attributes.IAttribute
import net.minecraft.entity.ai.attributes.RangedAttribute
import net.minecraft.inventory.EntityEquipmentSlot
import net.minecraft.util.ResourceLocation
import net.minecraftforge.registries.IForgeRegistryEntry
import net.minecraftforge.registries.IForgeRegistryModifiable
import java.io.File
import java.util.*

object SkillRegistry : IForgeRegistryModifiable<SkillData> by buildRegistry("skills") {
    // TODO: set missing callback to return dummy value to be replaced by network loading

    val allSkillStrings by lazy { keys.map(ResourceLocation::getPath).toTypedArray() }
    val allSkillKeys by lazy { keys.map(ResourceLocation::toString).toTypedArray() }

    private lateinit var savefile: File

    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(Stat::class.java, StatDeserializer)
        .registerTypeAdapter(ResourceLocation::class.java, ResourceLocationSerde)
        .excludeFieldsWithoutExposeAnnotation()
        .create()

    fun setupDataDir(dir: File) {
        this.loadDirectoryLayout(dir)
    }

    private fun loadDirectoryLayout(dir: File) {
        if ((!dir.exists() && !dir.mkdirs()) || !dir.isDirectory) throw IllegalStateException("$dir exists but is not a directory")
        savefile = File(dir, "skillregistry.json")
    }

    fun load() { // TODO: this should be changed to registry event
        try {
            if (savefile.exists()) savefile.reader().use {
                this.clear()
                gson.fromJson<List<SkillData>>(
                    it,
                    object : TypeToken<List<SkillData>>() {}.type
                )
            }.forEach(::register)
            else savefile.writer().use {
                gson.toJson(valuesCollection, it)
            }
        } catch (e: Exception) {
            BlueRPG.LOGGER.error("Couldn't read Skill registry", Exception(e))
        }
    }
}

@NamedDynamic(resourceLocation = "b:sk")
data class SkillData(
    @Expose val key: ResourceLocation,
    @Expose val mana: Int,
    @Expose val cooldown: Int,
    @Expose val magic: Boolean,
    @Expose val levelTransformer: LevelModifier,
    @Expose val processor: Processor,
    @Expose override val uuid: Array<UUID>
) : IForgeRegistryEntry.Impl<SkillData>(), Stat {
    init {
        this.registryName = key
    }

    fun startUsing(caster: EntityLivingBase): Boolean =
        processor.startUsing(caster)

    fun stopUsing(caster: EntityLivingBase, timeChanneled: Int) =
        processor.stopUsing(caster, timeChanneled)

    override val name: String = key.toString()

    override val attribute: IAttribute by lazy {
        RangedAttribute(
            null,
            "${BlueRPG.MODID}.${this.name.toLowerCase()}",
            baseValue,
            0.0,
            Double.MAX_VALUE
        ).setShouldWatch(true)
    }

    override fun getRoll(ilvl: Int, rarity: Rarity, gearType: GearType, slot: EntityEquipmentSlot): Int =
        TODO("Not supported yet")

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SkillData) return false

        if (key != other.key) return false

        return true
    }

    override fun hashCode(): Int {
        return key.hashCode()
    }
}

data class LevelModifier(
    @Expose val manaMod: Float,
    @Expose val cooldownMod: Float
)