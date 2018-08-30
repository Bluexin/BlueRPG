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

package be.bluexin.rpg.gear

import be.bluexin.rpg.stats.GearStats
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTBase
import net.minecraft.util.EnumFacing
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.ICapabilitySerializable

class GearStatWrapper(itemStack: ItemStack) : ICapabilitySerializable<NBTBase> {

    val stats = GearStats(itemStack)//.also(GearStats::generate)

    override fun <T : Any?> getCapability(capability: Capability<T>, facing: EnumFacing?): T? =
            if (capability == GearStats.Capability) GearStats.Capability.cast(stats) else null

    override fun hasCapability(capability: Capability<*>, facing: EnumFacing?) = capability == GearStats.Capability

    override fun deserializeNBT(nbt: NBTBase?) {
        if (nbt != null) GearStats.Storage.readNBT(GearStats.Capability, stats, null, nbt)
    }

    override fun serializeNBT(): NBTBase {
        return GearStats.Storage.writeNBT(GearStats.Capability, stats, null)
    }
}