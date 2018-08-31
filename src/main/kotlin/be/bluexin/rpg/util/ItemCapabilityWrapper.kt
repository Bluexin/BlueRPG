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

package be.bluexin.rpg.util

import be.bluexin.rpg.stats.GearStats
import net.minecraft.nbt.NBTBase
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.ICapabilitySerializable

class ItemCapabilityWrapper<A>(val instance: A, val capability: Capability<A>) : ICapabilitySerializable<NBTBase> {

    override fun <T : Any?> getCapability(capability: Capability<T>, facing: EnumFacing?): T? =
            if (capability == this.capability) this.capability.cast(instance) else null

    override fun hasCapability(capability: Capability<*>, facing: EnumFacing?) = capability == GearStats.Capability

    override fun deserializeNBT(nbt: NBTBase?) {
        if (nbt != null) capability.storage.readNBT(capability, instance, null, nbt)
    }

    override fun serializeNBT(): NBTBase {
        return capability.storage.writeNBT(capability, instance, null)?: NBTTagCompound()
    }
}