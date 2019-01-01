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

package be.bluexin.rpg.gear

import be.bluexin.rpg.items.IUsable
import be.bluexin.rpg.stats.*
import be.bluexin.rpg.util.ItemCapabilityWrapper
import be.bluexin.rpg.util.set
import be.bluexin.saomclib.onServer
import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import com.teamwizardry.librarianlib.features.kotlin.Minecraft
import com.teamwizardry.librarianlib.features.kotlin.localize
import net.minecraft.client.Minecraft
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
import net.minecraft.world.World
import net.minecraftforge.common.capabilities.ICapabilityProvider

interface IRPGGear : IUsable<ItemStack> { // TODO: use ISpecialArmor

    val type: GearType

    val gearSlot: EntityEquipmentSlot

    fun addInformation(stack: ItemStack, worldIn: World?, tooltip: MutableList<String>, flagIn: ITooltipFlag) {
        val stats = stack.stats
        if (stats?.generated != true) tooltip += "rpg.display.notgenerated".localize(Minecraft().gameSettings.keyBindPickBlock.displayName)
        else {
            val spacer = "rpg.tooltip.spacer".localize()
            tooltip += "rpg.tooltip.desc".localize(
                stats.ilvl, stats.rarity?.localized ?: "Error", "rpg.$key.name".localize()
            )
            tooltip += spacer
            tooltipizeFixedStats(stats).forEach { tooltip += it }
            tooltip += spacer
            val p = Minecraft.getMinecraft().player
            tooltip += "rpg.tooltip.levelreq".localize(
                "rpg.tooltip.${if (stats.levelReqMet(p)) "metreq" else "unmetreq"}".localize(stats.levelReq)
            )
            if (stats.requiredValue > 0) tooltip += "rpg.tooltip.statreq".localize(
                stats.requiredStat!!.longName(),
                "rpg.tooltip.${if (stack.statsReqMet(p)) "metreq" else "unmetreq"}".localize(stats.requiredValue)
            )
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
            val d = if (stack.maxDamage > 0) ((stack.maxDamage - stack.itemDamage) / stack.maxDamage.toFloat()) else 0f
            tooltip += "rpg.tooltip.durability.${
            when {
                d < 0.25f -> "low"
                d < 0.50f -> "medium"
                else -> "high"
            }
            }".localize(stack.maxDamage - stack.itemDamage, stack.maxDamage)
        }
    }

    fun getAttributeModifiers(slot: EntityEquipmentSlot, stack: ItemStack): Multimap<String, AttributeModifier> {
        val m = HashMultimap.create<String, AttributeModifier>()
        if (slot != this.gearSlot) return m
        if (!stack.enabled) return m
        val stats = stack.stats ?: return m
        if (!stats.generated) return m

        stats.stats().forEach { (stat, value) ->
            m[stat.attribute.name] = AttributeModifier(
                stat.uuid(this.gearSlot),
                stat.attribute.name,
                if (stat.operation != 0) stat(value) / 100.0 else stat(value),
                stat.operation
            )
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

    fun onItemRightClick(worldIn: World, playerIn: EntityPlayer, handIn: EnumHand) =
        this(playerIn.getHeldItem(handIn), worldIn, playerIn)

    override fun invoke(stack: ItemStack, worldIn: World, playerIn: EntityPlayer): ActionResult<ItemStack> {
        val stats = stack.stats
            ?: throw IllegalStateException("Missing capability!")
        return if (!stats.generated) {
            worldIn onServer { stats.generate(playerIn) }
            ActionResult.newResult(EnumActionResult.SUCCESS, stack)
        } else ActionResult.newResult(EnumActionResult.PASS, stack)
    }

    fun getItemStackDisplayName(stack: ItemStack): String {
        val stats = stack.stats
        return if (stats == null || !stats.generated || stats.name == null)
            "${this.getUnlocalizedNameInefficientlyTrick(stack)}.name".localize().trim()
        else stats.name!!
    }

    fun getMaxDamage(stack: ItemStack) = stack.stats?.durability ?: 1

    fun getUnlocalizedNameInefficientlyTrick(stack: ItemStack): String

    fun tooltipizeFixedStats(stats: GearStats): Sequence<String>

    val item: Item

    val key: String
        get() = type.key
}