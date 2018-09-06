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
import be.bluexin.rpg.stats.PrimaryStat
import be.bluexin.rpg.stats.SecondaryStat
import be.bluexin.rpg.stats.stats
import be.bluexin.rpg.util.ItemCapabilityWrapper
import be.bluexin.rpg.util.set
import be.bluexin.saomclib.onServer
import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import com.teamwizardry.librarianlib.features.kotlin.localize
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.entity.ai.attributes.AttributeModifier
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.EntityEquipmentSlot
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.ActionResult
import net.minecraft.util.EnumActionResult
import net.minecraft.util.EnumHand
import net.minecraft.util.text.translation.I18n
import net.minecraft.world.World
import net.minecraftforge.common.capabilities.ICapabilityProvider
import kotlin.math.max

interface IRPGGear { // TODO: use ISpecialArmor

    val type: GearType

    val gearSlot: EntityEquipmentSlot

    /*
&d&lThe Butt-tailed Spear of doinking   <--- Name
&8-=[A Level 37 Epic Spear]=-           <--- center it below the name ?
&7
&7Normal Damage: &7112-155             <--- Second color will change for element later.
&7
&7Level Req:&a 12                       <-- can we get these to be &a if good and &c if not met?
&7Strength Req:&a 23
&7
&a+12 Strength
&a+11 Intelligence
&7
&a+1.23% &7Speed
&a+2.34% &7Block Chance
&a+15.25% &7Crit Chance
&a+1.25% &7Bonus Damage
&7
&cBind on Equip    or  &6Soulbound
&7Durability &a800/800                  <-- can this transition from &a to &e / &c at 50/25% ?

     */

    fun addInformation(stack: ItemStack, worldIn: World?, tooltip: MutableList<String>, flagIn: ITooltipFlag) {
        val stats = stack.stats
        if (stats?.generated != true) tooltip += "rpg.display.notgenerated".localize()
        else {
            val spacer = "rpg.tooltip.spacer".localize()
            tooltip += "rpg.tooltip.desc".localize(
                    stats.ilvl, stats.rarity?.localized ?: "Error", "rpg.$key.name".localize()
            )
            tooltip += spacer
            tooltipizeFixedStats(stats).forEach { tooltip += it }
            tooltip += spacer
            tooltip += "rpg.tooltip.levelreq".localize(stats.levelReq) // TODO: color req based on met criteria
            // TODO: Add stats requirements once that's done
            tooltip += spacer
            PrimaryStat.values().forEach {
                if (stats[it] != 0) tooltip += "rpg.tooltip.pstat".localize(
                        it.localize(stats[it]), it.longName()
                ) // TODO: color for damage element
            }
            tooltip += spacer
            SecondaryStat.values().forEach {
                if (stats[it] != 0) tooltip += "rpg.tooltip.sstat".localize(
                        it.localize(stats[it]), it.longName()
                )
            }
            tooltip += spacer
            tooltip += if (stats.bound != null) "rpg.tooltip.bound".localize()
            else "rpg.tooltip.binding".localize(stats.binding.localized)
            // TODO: coloring on durability
            tooltip += "rpg.tooltip.durability".localize(stack.maxDamage - stack.itemDamage, stack.maxDamage)
        }
    }

    fun getAttributeModifiers(slot: EntityEquipmentSlot, stack: ItemStack): Multimap<String, AttributeModifier> {
        val m = HashMultimap.create<String, AttributeModifier>()
        if (slot != this.gearSlot) return m
        val stats = stack.stats ?: return m
        if (!stats.generated) return m

        stats.stats().forEach { (stat, value) ->
            m[stat.attribute.name] = AttributeModifier(stat.uuid(this.gearSlot), stat.attribute.name, if (stat.operation != 0) stat(value) / 100.0 else stat(value), stat.operation)
        }

        return m
    }

    fun initCapabilities(stack: ItemStack, nbt: NBTTagCompound?): ICapabilityProvider? {
        return ItemCapabilityWrapper(GearStats(stack), GearStats.Capability)
    }

    fun addNBTShare(stack: ItemStack, nbt: NBTTagCompound): NBTTagCompound {
        val cap = stack.stats ?: return nbt
        if (cap.generated) nbt.setTag("capabilities", GearStats.Storage.writeNBT(GearStats.Capability, cap, null))
        else nbt.setTag("capabilities", GearStats.Storage.writeNBT(GearStats.Capability, GearStats(stack), null))
        return nbt
    }

    fun readNBTShare(stack: ItemStack, nbt: NBTTagCompound?) {
        val capNbt = nbt?.getCompoundTag("capabilities") ?: return
        val cap = stack.stats ?: return
        GearStats.Storage.readNBT(GearStats.Capability, cap, null, capNbt)
    }

    fun onItemRightClick(worldIn: World, playerIn: EntityPlayer, handIn: EnumHand): ActionResult<ItemStack> {
        val stack = playerIn.getHeldItem(handIn)
        val stats = stack.stats
                ?: throw IllegalStateException("Missing capability!")
        return if (!stats.generated) {
            worldIn onServer { stats.generate(playerIn) }
            ActionResult.newResult(EnumActionResult.SUCCESS, stack)
        } else ActionResult.newResult(EnumActionResult.PASS, stack)
    }

    fun getItemStackDisplayName(stack: ItemStack): String {
        val stats = stack.stats
        return if (stats == null || !stats.generated || stats.name == null) I18n.translateToLocal(this.getUnlocalizedNameInefficientlyTrick(stack) + ".name").trim { it <= ' ' }
        else stats.name!!
    }

    fun getMaxDamage(stack: ItemStack) = stack.stats?.durability ?: 1

    fun setDamage(stack: ItemStack, damage: Int) {
        if (stack.stats?.generated == true) stack.itemDamage = max(0, damage)
    }

    fun getUnlocalizedNameInefficientlyTrick(stack: ItemStack): String

    fun tooltipizeFixedStats(stats: GearStats): Sequence<String>

    val item: Item

    val key: String
        get() = type.key
}