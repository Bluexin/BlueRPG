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
import be.bluexin.rpg.gear.*
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
import net.minecraft.nbt.NBTTagInt
import net.minecraft.util.EnumFacing
import net.minecraft.util.ResourceLocation
import net.minecraft.util.text.TextComponentTranslation
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.CapabilityInject
import java.lang.ref.WeakReference
import java.util.*

@Savable
@NamedDynamic(resourceLocation = "b:gs")
class GearStats(val itemStackIn: ItemStack) : StatCapability {

    internal constructor() : this(ItemStack.EMPTY)

    @Save
    var generated = false

    @Save
    var generator = TokenType.CRAFTED

    @Save
    var rarity: Rarity? = null

    @Save
    var binding = Binding.BOE // TODO

    @Save
    var bound: UUID? = null

    @Save
    var ilvl = 1

    @Save
    var levelReq = 1

    @Save
    var name: String? = null

    @Save
    var stats: StatsCollection = StatsCollection(WeakReference(itemStackIn))
        internal set

    @Save
    var durability: Int = 1

    fun generate(playerIn: EntityPlayer) {
        playerIn.world onServer {
            val gear = itemStackIn.item as? IRPGGear ?: return
            this.stats.clear()
            if (rarity == null) rarity = generator.generateRarity()
            val stats = rarity!!.rollStats()
            stats.forEach { this.stats[it] += it.getRoll(ilvl, rarity!!, gear.type, gear.gearSlot) }
            when (gear) {
                is ItemArmor -> {
                    this.stats[FixedStat.HEALTH] += FixedStat.HEALTH.getRoll(ilvl, rarity!!, gear.type, gear.gearSlot)
                    this.stats[FixedStat.ARMOR] += FixedStat.ARMOR.getRoll(ilvl, rarity!!, gear.type, gear.gearSlot)
                }
                is ItemOffHand -> {
                    this.stats[FixedStat.HEALTH] += FixedStat.HEALTH.getRoll(ilvl, rarity!!, gear.type, gear.gearSlot)
                    when (gear.type) {
                        OffHandType.SHIELD -> this.stats[FixedStat.F_BLOCK] += FixedStat.F_BLOCK.getRoll(ilvl, rarity!!, gear.type, gear.gearSlot)
                        OffHandType.PARRY_DAGGER -> this.stats[FixedStat.F_PARRY] += FixedStat.F_PARRY.getRoll(ilvl, rarity!!, gear.type, gear.gearSlot)
                        OffHandType.FOCUS -> this.stats[FixedStat.F_CRIT_CHANCE] += FixedStat.F_CRIT_CHANCE.getRoll(ilvl, rarity!!, gear.type, gear.gearSlot)
                    }
                }
                else -> {
                    this.stats[FixedStat.BASE_DAMAGE] += FixedStat.BASE_DAMAGE.getRoll(ilvl, rarity!!, gear.type, gear.gearSlot)
                    this.stats[FixedStat.MAX_DAMAGE] += FixedStat.MAX_DAMAGE.getRoll(ilvl, rarity!!, gear.type, gear.gearSlot)
                }
            }
            durability = FixedStat.DURABILITY.getRoll(ilvl, rarity!!, gear.type, gear.gearSlot)
            generated = true
            if (name == null) name = NameGenerator(itemStackIn, playerIn)
            itemStackIn.setTagInfo("HideFlags", NBTTagInt(2))
            if (rarity!!.shouldNotify) playerIn.world.minecraftServer?.playerList?.players?.forEach {
                it.sendMessage(TextComponentTranslation("rpg.broadcast.item", playerIn.displayName, itemStackIn.textComponent))
            }
        }
    }

    operator fun get(stat: Stat) = stats[stat]

    fun loadFrom(other: GearStats) {
        generated = other.generated
        generator = other.generator
        rarity = other.rarity
        binding = other.binding
        bound = other.bound
        ilvl = other.ilvl
        levelReq = other.levelReq
        stats = other.stats
        name = other.name
    }

    override fun copy() = GearStats(ItemStack.EMPTY).also {
        it.loadFrom(this)
    }

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
