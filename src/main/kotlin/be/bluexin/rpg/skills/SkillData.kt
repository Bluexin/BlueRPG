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
import net.minecraft.util.ResourceLocation
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.RegistryEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
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

    init {
        MinecraftForge.EVENT_BUS.register(this)
    }

    @SubscribeEvent
    fun registerSkills(event: RegistryEvent.Register<SkillData>) {
        MinecraftForge.EVENT_BUS.unregister(this)

        // TODO: register skills
    }
}

data class SkillData(
        val key: ResourceLocation,
        val name: String,
        val icon: ResourceLocation,
        val description: String,
        val mana: Int,
        val cooldown: Int,
        val levelTransformer: Placeholder,
        val processor: Processor
) : IForgeRegistryEntry.Impl<SkillData>()

data class Placeholder(val t: Int)