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
import be.bluexin.rpg.gear.GearTypeGenerator
import be.bluexin.rpg.gear.ItemGearToken
import be.bluexin.rpg.gear.Rarity
import be.bluexin.saomclib.capabilities.Key
import be.bluexin.saomclib.onServer
import com.teamwizardry.librarianlib.features.saving.AbstractSaveHandler
import com.teamwizardry.librarianlib.features.saving.NamedDynamic
import com.teamwizardry.librarianlib.features.saving.Savable
import com.teamwizardry.librarianlib.features.saving.Save
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTBase
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraft.util.ResourceLocation
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.CapabilityInject
import net.minecraftforge.items.ItemHandlerHelper

@Savable
@NamedDynamic(resourceLocation = "b:ts")
class TokenStats(val itemStackIn: ItemStack) : StatCapability {

    internal constructor() : this(ItemStack.EMPTY)

    @Save
    var rarity: Rarity? = null

    @Save
    var binding = Binding.BOE

    @Save
    var ilvl = 1

    @Save
    var levelReq = 1

    fun open(playerIn: EntityPlayer): ItemStack {
        playerIn.world onServer {
            val token = itemStackIn.item as? ItemGearToken ?: return itemStackIn
            val rarity = this.rarity ?: token.type.generateRarity()
            val iss = ItemStack(GearTypeGenerator().item)
            val stats = iss.stats ?: throw IllegalStateException("Missing capability!")
            stats.rarity = rarity
            stats.ilvl = this.ilvl
            stats.levelReq = this.levelReq
            stats.binding = this.binding
            stats.generate(playerIn)

            if (!playerIn.isCreative) itemStackIn.shrink(1)
            if (itemStackIn.isEmpty) return iss
            else ItemHandlerHelper.giveItemToPlayer(playerIn, iss)
        }
        return itemStackIn
    }

    fun loadFrom(other: TokenStats) {
        rarity = other.rarity
        binding = other.binding
        ilvl = other.ilvl
        levelReq = other.levelReq
    }

    override fun copy() = TokenStats(ItemStack.EMPTY).also {
        it.loadFrom(this)
    }

    internal object Storage : Capability.IStorage<TokenStats> {
        override fun readNBT(capability: Capability<TokenStats>, instance: TokenStats, side: EnumFacing?, nbt: NBTBase) {
            val nbtTagCompound = nbt as? NBTTagCompound ?: return
            try {
                AbstractSaveHandler.readAutoNBT(instance, nbtTagCompound.getTag(KEY.toString()), false)
            } catch (e: Exception) {
                BlueRPG.LOGGER.warn("Failed to read token stats.", e)
                // Resetting bad data is fine
            }
        }

        override fun writeNBT(capability: Capability<TokenStats>, instance: TokenStats, side: EnumFacing?): NBTBase {
            return NBTTagCompound().also { it.setTag(KEY.toString(), AbstractSaveHandler.writeAutoNBT(instance, false)) }
        }
    }

    companion object {
        @Key
        val KEY = ResourceLocation(BlueRPG.MODID, "token_stats")

        @CapabilityInject(TokenStats::class)
        lateinit var Capability: Capability<TokenStats>
            internal set
    }
}

val ItemStack.tokenStats get() = this.getCapability(TokenStats.Capability, null)
