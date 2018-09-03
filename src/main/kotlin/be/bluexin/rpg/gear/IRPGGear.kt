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
import be.bluexin.rpg.stats.stats
import be.bluexin.rpg.util.ItemCapabilityWrapper
import be.bluexin.saomclib.onServer
import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import com.teamwizardry.librarianlib.features.kotlin.localize
import net.minecraft.client.gui.GuiScreen
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

interface IRPGGear { // TODO: use ISpecialArmor

    val type: GearType

    val gearSlot: EntityEquipmentSlot

    fun addInformation(stack: ItemStack, worldIn: World?, tooltip: MutableList<String>, flagIn: ITooltipFlag) {
        val cap = stack.stats
        if (cap?.generated != true) tooltip.add("rpg.display.notgenerated".localize())
        else {
            val shift = GuiScreen.isShiftKeyDown()
            tooltip.add("rpg.display.item".localize(cap.rarity?.localized?: "rpg.random.name".localize(), "rpg.$key.name".localize()))
            tooltip.add("rpg.display.level".localize(cap.ilvl))
            tooltip.add("rpg.display.levelreq".localize(cap.levelReq))
            tooltip.add("rpg.display.stats".localize())
            tooltip.addAll(cap.stats().map {
                "rpg.display.stat".localize(if (shift) it.key.longName() else it.key.shortName(), it.value)
            })
            if (!shift) tooltip.add("rpg.display.shift".localize())
        }
    }

    fun getAttributeModifiers(slot: EntityEquipmentSlot, stack: ItemStack): Multimap<String, AttributeModifier> {
        return HashMultimap.create() // TODO use this for stats like hp?
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

    fun getUnlocalizedNameInefficientlyTrick(stack: ItemStack): String

    val item: Item

    val key: String
        get() = type.key
}