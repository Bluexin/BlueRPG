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
import net.minecraft.util.text.TextComponentTranslation
import net.minecraft.world.World
import net.minecraftforge.common.capabilities.ICapabilityProvider

interface IRPGGear { // TODO: use ISpecialArmor

    val type: GearType

    val gearSlot: EntityEquipmentSlot

    fun addInformation(stack: ItemStack, worldIn: World?, tooltip: MutableList<String>, flagIn: ITooltipFlag) {
        val cap = stack.stats?: return
        tooltip.add("rpg.display.item".localize(cap.rarity.localized, "rpg.$key.name".localize()))
        tooltip.add("rpg.display.level".localize(cap.ilvl))
        val shift = GuiScreen.isShiftKeyDown()
        tooltip.add("rpg.display.stats".localize())
        if (cap.stats.isEmpty()) tooltip.add("rpg.display.notgenerated".localize())
        else tooltip.addAll(cap.stats().map {
            "rpg.display.stat".localize(if (shift) it.key.longName() else it.key.shortName(), it.value)
        })
        if (!shift && !cap.stats.isEmpty()) tooltip.add("rpg.display.shift".localize())
        stack.maxDamage
    }

    fun getAttributeModifiers(slot: EntityEquipmentSlot, stack: ItemStack): Multimap<String, AttributeModifier> {
        return HashMultimap.create() // TODO use this for stats like hp?
    }

    fun initCapabilities(stack: ItemStack, nbt: NBTTagCompound?): ICapabilityProvider? {
        return GearStatWrapper(stack)
    }

    fun addNBTShare(stack: ItemStack, nbt: NBTTagCompound): NBTTagCompound {
        val cap = stack.stats?: return nbt
        val capNbt = GearStats.Storage.writeNBT(GearStats.Capability, cap, null)
        nbt.setTag("capabilities", capNbt)
        return nbt
    }

    fun readNBTShare(stack: ItemStack, nbt: NBTTagCompound?) {
        val capNbt = nbt?.getCompoundTag("capabilities")?: return
        val cap = stack.stats?: return
        GearStats.Storage.readNBT(GearStats.Capability, cap, null, capNbt)
    }

    fun onItemRightClick(worldIn: World, playerIn: EntityPlayer, handIn: EnumHand): ActionResult<ItemStack> {
        var stack = playerIn.getHeldItem(handIn)
        val stats = stack.stats
                ?: throw IllegalStateException("Missing capability!")

        return if (stats.stats.isEmpty()) {
            worldIn onServer {
                stats.generate()
                stack.setStackDisplayName(NameGenerator(stack, playerIn))

                if (stats.rarity.shouldNotify) worldIn.minecraftServer?.playerList?.players?.forEach {
                    it.sendMessage(TextComponentTranslation("rpg.broadcast.item", playerIn.displayName, stack.textComponent))
                }
            }
            ActionResult.newResult(EnumActionResult.SUCCESS, stack)
        } else ActionResult.newResult(EnumActionResult.PASS, stack)
    }

    val item: Item

    val key: String
        get() = type.key
}