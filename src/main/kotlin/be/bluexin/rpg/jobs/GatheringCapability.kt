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

package be.bluexin.rpg.jobs

import be.bluexin.rpg.BlueRPG
import be.bluexin.rpg.devutil.get
import be.bluexin.saomclib.capabilities.AbstractCapability
import be.bluexin.saomclib.capabilities.Key
import com.teamwizardry.librarianlib.features.saving.AbstractSaveHandler
import com.teamwizardry.librarianlib.features.saving.Save
import com.teamwizardry.librarianlib.features.saving.SaveInPlace
import net.minecraft.nbt.NBTBase
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraft.util.ResourceLocation
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.CapabilityInject
import net.minecraftforge.common.util.INBTSerializable

@SaveInPlace
class GatheringCapability : AbstractCapability(), INBTSerializable<NBTTagCompound> {

    @Save
    var age = 0L
        set(value) {
            if (value < maxAge) field = value
            else if (value >= maxAge) {
                //TODO: grow
                maxAge = 0
                field = 0
            }
        }

    @Save
    var maxAge = 0L
        set(value) {
            field = if (value > 0) value
            else data?.nextGatherTime ?: 0
        }

    @Save
    @Deprecated("Use data directly instead")
    @Suppress("PropertyName")
    internal /*private*/ var _data: ResourceLocation? = null

    @Suppress("DEPRECATION")
    var data: GatheringData? = null
        get() {
            return if (field == null && _data != null) {
                // TODO: get from registry
                field = GatheringRegistry[_data!!]
                return field
            } else null
        }
        set(value) {
            field = value
            _data = value?.key
        }

    override fun setup(param: Any): AbstractCapability {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun tick() {
        ++age
    }

    fun getComparatorOutput() = if (maxAge == 0L) 0f else age / maxAge.toFloat()

    companion object {
        @Suppress("unused")
        @Key
        val KEY = ResourceLocation(BlueRPG.MODID, "gatheringCapability")

        @CapabilityInject(GatheringCapability::class)
        lateinit var CAP_INSTANCE: Capability<GatheringCapability>
            internal set

    }

    internal object Storage : Capability.IStorage<GatheringCapability> {
        override fun readNBT(
            capability: Capability<GatheringCapability>,
            instance: GatheringCapability,
            side: EnumFacing?,
            nbt: NBTBase
        ) {
            val nbtTagCompound = nbt as? NBTTagCompound ?: return
            AbstractSaveHandler.readAutoNBT(instance, nbtTagCompound.getTag(GatheringCapability.KEY.toString()), false)
        }

        override fun writeNBT(
            capability: Capability<GatheringCapability>,
            instance: GatheringCapability,
            side: EnumFacing?
        ): NBTBase {
            return NBTTagCompound().also {
                it.setTag(
                    GatheringCapability.KEY.toString(),
                    AbstractSaveHandler.writeAutoNBT(instance, false)
                )
            }
        }
    }

    override fun deserializeNBT(nbt: NBTTagCompound) = Storage.readNBT(CAP_INSTANCE, this, null, nbt)
    override fun serializeNBT() = Storage.writeNBT(CAP_INSTANCE, this, null) as NBTTagCompound
}