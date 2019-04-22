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

import be.bluexin.saomclib.capabilities.AbstractCapability
import com.teamwizardry.librarianlib.features.autoregister.SerializerRegister
import com.teamwizardry.librarianlib.features.saving.AbstractSaveHandler
import com.teamwizardry.librarianlib.features.saving.FieldType
import com.teamwizardry.librarianlib.features.saving.serializers.Serializer
import io.netty.buffer.ByteBuf
import net.minecraft.nbt.NBTBase
import net.minecraft.nbt.NBTTagByte
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraftforge.common.capabilities.Capability

@SerializerRegister(Unit::class)
object SerializeUnit : Serializer<Unit>(FieldType.create(Boolean::class.javaPrimitiveType!!)) {
    override fun getDefault() = Unit
    override fun readBytes(buf: ByteBuf, existing: Unit?, syncing: Boolean) = Unit
    override fun writeBytes(buf: ByteBuf, value: Unit, syncing: Boolean) = Unit
    override fun readNBT(nbt: NBTBase, existing: Unit?, syncing: Boolean) = Unit
    override fun writeNBT(value: Unit, syncing: Boolean): NBTBase = NBTTagByte(0)
}

class AutoCapabilityStorage<T : AbstractCapability> : Capability.IStorage<T> {
    override fun readNBT(
        capability: Capability<T>,
        instance: T,
        side: EnumFacing?,
        nbt: NBTBase
    ) {
        val nbtTagCompound = nbt as? NBTTagCompound ?: return
        AbstractSaveHandler.readAutoNBT(instance, nbtTagCompound, false)
    }

    override fun writeNBT(capability: Capability<T>, instance: T, side: EnumFacing?) =
        AbstractSaveHandler.writeAutoNBT(instance, false)
}