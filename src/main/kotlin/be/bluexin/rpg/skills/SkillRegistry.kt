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
import be.bluexin.rpg.stats.mana
import be.bluexin.rpg.util.*
import be.bluexin.saomclib.onServer
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.ai.attributes.IAttribute
import net.minecraft.entity.ai.attributes.RangedAttribute
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.EntityEquipmentSlot
import net.minecraft.util.EnumActionResult
import net.minecraft.util.ResourceLocation
import net.minecraftforge.registries.IForgeRegistryEntry
import java.io.File
import java.util.*

object SkillRegistry : IdAwareForgeRegistryModifiable<SkillData> by buildRegistry("skills") {
    // TODO: set missing callback to return dummy value to be replaced by network loading

    val allSkillStrings by lazy { keys.map(ResourceLocation::getPath).toTypedArray() }
    val allSkillKeys by lazy { keys.map(ResourceLocation::toString).toTypedArray() }

    private lateinit var savefile: File

    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(Stat::class.java, StatDeserializer)
        .registerTypeAdapter(ResourceLocation::class.java, ResourceLocationSerde)
        .registerTypeAdapterFactory(ExpressionAdapterFactory)
        .registerTypeAdapterFactory(DynamicTypeAdapterFactory)
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
                gson.fromJson<List<SkillData.SerData>>(
                    it,
                    object : TypeToken<List<SkillData.SerData>>() {}.type
                )
            }.map(SkillData.SerData::mapped).forEach(::register)
            else savefile.writer().use {
                gson.toJson(valuesCollection.map(SkillData::mapped), it)
            }
        } catch (e: Exception) {
            BlueRPG.LOGGER.error("Couldn't read Skill registry", Exception(e))
        }
    }
}

data class SkillData(
    val key: ResourceLocation,
    val mana: Int,
    val cooldown: Int,
    val magic: Boolean,
    val levelTransformer: LevelModifier, // TODO: use scaling
    val processor: Processor,
    override val uuid: Array<UUID>
) : IForgeRegistryEntry.Impl<SkillData>(), Stat {
    init {
        this.registryName = key
    }

    /**
     * [EnumActionResult.SUCCESS] = casting done (instant)
     * [EnumActionResult.PASS] = casting ongoing
     * [EnumActionResult.FAIL] = casting failed
     */
    fun startUsing(caster: EntityLivingBase): EnumActionResult = if (caster is EntityPlayer) {
        if (checkRequirement(caster)) {
            if (processor.startUsing(caster)) {
                consume(caster)
                EnumActionResult.SUCCESS
            } else EnumActionResult.PASS
        } else EnumActionResult.FAIL
    } else if (processor.startUsing(caster)) EnumActionResult.SUCCESS else EnumActionResult.PASS

    fun stopUsing(caster: EntityLivingBase, timeChanneled: Int): Boolean = if (caster is EntityPlayer) {
        if (checkRequirement(caster) && processor.stopUsing(caster, timeChanneled)) {
            consume(caster)
            true
        } else false
    } else processor.stopUsing(caster, timeChanneled)

    private fun checkRequirement(caster: EntityPlayer) = caster.mana >= this.mana && this !in caster.cooldowns

    private fun consume(caster: EntityPlayer) {
        caster.world onServer {
            caster.mana -= this.mana
            caster.cooldowns[this] = this.cooldown
        }
    }

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

    val mapped get() = SerData(key, mana, cooldown, magic, levelTransformer, processor, uuid)

    data class SerData(
        val key: ResourceLocation,
        val mana: Int,
        val cooldown: Int,
        val magic: Boolean,
        val levelTransformer: LevelModifier,
        val processor: Processor,
        val uuid: Array<UUID>
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is SkillData) return false

            if (key != other.key) return false

            return true
        }

        override fun hashCode(): Int {
            return key.hashCode()
        }

        val mapped get() = SkillData(key, mana, cooldown, magic, levelTransformer, processor, uuid)
    }
}

data class LevelModifier(
    val manaMod: Float,
    val cooldownMod: Float
)