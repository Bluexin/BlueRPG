/*
 * Copyright (C) 2018.  Arnaud 'Bluexin' Solé
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
import net.minecraft.entity.EntityLivingBase
import net.minecraft.util.ResourceLocation
import net.minecraftforge.registries.IForgeRegistry
import net.minecraftforge.registries.IForgeRegistryEntry
import net.minecraftforge.registries.RegistryBuilder

object SkillRegistry {
    private val registry: IForgeRegistry<SkillData> = RegistryBuilder<SkillData>()
        .setName(ResourceLocation(BlueRPG.MODID, "skills"))
        .setType(SkillData::class.java)
        .disableSaving()
        .allowModification()
        .create()

    operator fun get(rl: ResourceLocation) = registry.getValue(rl)
    operator fun get(str: String) = this[ResourceLocation(BlueRPG.MODID, str)]

    val allSkillStrings by lazy { registry.keys.map(ResourceLocation::getPath).toTypedArray() }
}

data class SkillData(
    val key: ResourceLocation,
    val icon: ResourceLocation,
    val description: String,
    val mana: Int,
    val cooldown: Int,
    val levelTransformer: Placeholder,
    val processor: Processor
) : IForgeRegistryEntry.Impl<SkillData>() {
    init {
        this.registryName = key
    }

    fun startUsing(caster: EntityLivingBase): Boolean =
        processor.startUsing(caster)

    fun stopUsing(caster: EntityLivingBase, timeChanneled: Int) =
        processor.stopUsing(caster, timeChanneled)
}

data class Placeholder(val t: Int)