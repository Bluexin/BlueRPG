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

package be.bluexin.rpg.stats

import be.bluexin.rpg.BlueRPG
import be.bluexin.rpg.gear.Binding
import be.bluexin.rpg.gear.IRPGGear
import be.bluexin.rpg.gear.Rarity
import be.bluexin.saomclib.capabilities.Key
import com.teamwizardry.librarianlib.features.saving.AbstractSaveHandler
import com.teamwizardry.librarianlib.features.saving.Save
import com.teamwizardry.librarianlib.features.saving.SaveInPlace
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTBase
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraft.util.ResourceLocation
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.CapabilityInject
import java.lang.ref.WeakReference

@SaveInPlace
class GearStats(val itemStackIn: ItemStack) {

    @Save
    var rarity = Rarity.COMMON // TODO

    @Save
    var binding = Binding.BOE // TODO

    @Save
    var bound = false

    @Save
    var ilvl = 1

    @Save
    var stats: StatsCollection = StatsCollection(WeakReference(itemStackIn))
        internal set

    fun generate() {
        this.stats.clear()
        val gear = itemStackIn.item as? IRPGGear ?: return
        val stats = rarity.rollStats()
        stats.forEach {
            this.stats[it] += it.getRoll(ilvl, rarity, gear.type, gear.gearSlot)
        }
    }

    operator fun get(stat: Stat) = stats[stat]

    internal object Storage : Capability.IStorage<GearStats> {
        override fun readNBT(capability: Capability<GearStats>, instance: GearStats, side: EnumFacing?, nbt: NBTBase) {
            val nbtTagCompound = nbt as? NBTTagCompound ?: return
            instance.stats.clear()
            try {
                AbstractSaveHandler.readAutoNBT(instance, nbtTagCompound.getTag(KEY.toString()), false)
            } catch (e: Exception) {
                BlueRPG.LOGGER.warn("Failed to read gear stats.", e)
                // Resetting bad data is fine
            }
        }

        override fun writeNBT(capability: Capability<GearStats>, instance: GearStats, side: EnumFacing?): NBTBase {
            return NBTTagCompound().also { it.setTag(KEY.toString(), AbstractSaveHandler.writeAutoNBT(instance, false)) }
        }
    }

    companion object {
        @Key
        val KEY = ResourceLocation(BlueRPG.MODID, "gear_stats")

        @CapabilityInject(GearStats::class)
        lateinit var Capability: Capability<GearStats>
            internal set
    }
}

val ItemStack.stats get() = this.getCapability(GearStats.Capability, null)
