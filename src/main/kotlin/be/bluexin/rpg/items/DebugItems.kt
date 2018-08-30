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

package be.bluexin.rpg.items

import be.bluexin.rpg.gui.GuiAttributes
import be.bluexin.rpg.stats.PlayerStats
import be.bluexin.rpg.stats.exp
import be.bluexin.rpg.stats.stats
import be.bluexin.saomclib.onClient
import be.bluexin.saomclib.onServer
import com.teamwizardry.librarianlib.features.base.item.ItemMod
import com.teamwizardry.librarianlib.features.kotlin.localize
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.ActionResult
import net.minecraft.util.EnumActionResult
import net.minecraft.util.EnumHand
import net.minecraft.world.World
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

object DebugStatsItem : ItemMod("debug_stats") {

    init {
        maxStackSize = 1
        maxDamage = 0
    }

    @SideOnly(Side.CLIENT)
    override fun addInformation(stack: ItemStack, worldIn: World?, tooltip: MutableList<String>, flagIn: ITooltipFlag) {
        val stats = Minecraft.getMinecraft().player?.stats?: return
        val shift = GuiScreen.isShiftKeyDown()

        tooltip.add("rpg.display.stats".localize())
        tooltip.add("rpg.display.level".localize(stats.level.level))
        tooltip.add("rpg.display.stat".localize("rpg.attributepoints.${if (shift) "long" else "short"}".localize(), stats.attributePoints))
        tooltip.add(
                if (shift) "rpg.display.exp.long".localize(stats.level.exp, stats.level.toNext)
                else "rpg.display.exp.short".localize(100f * stats.level.exp / stats.level.toNext)
        )
        tooltip.addAll(stats.baseStats().map {
            "rpg.display.stat".localize(if (shift) it.key.longName() else it.key.shortName(), it.value)
        })
        if (!shift) tooltip.add("rpg.display.shift".localize())
    }

    override fun onItemRightClick(worldIn: World, playerIn: EntityPlayer, handIn: EnumHand): ActionResult<ItemStack> {
        if (playerIn.isSneaking) {
            worldIn onServer {
                playerIn.stats.setup(playerIn)
                playerIn.stats.attributePoints = PlayerStats.LEVELUP_ATTRIBUTES * 3
            }
        } else worldIn onClient {
            Minecraft.getMinecraft().displayGuiScreen(GuiAttributes())
            return ActionResult.newResult(EnumActionResult.SUCCESS, playerIn.getHeldItem(handIn))
        }

        return super.onItemRightClick(worldIn, playerIn, handIn)
    }
}

object DebugExpItem : ItemMod("debug_exp") {
    init {
        maxStackSize = 1
        maxDamage = 0
    }

    @SideOnly(Side.CLIENT)
    override fun addInformation(stack: ItemStack, worldIn: World?, tooltip: MutableList<String>, flagIn: ITooltipFlag) {
        tooltip.add("rpg.display.debug_exp".localize())
        tooltip.add("rpg.display.debug_exp.shift".localize())
    }

    override fun onItemRightClick(worldIn: World, playerIn: EntityPlayer, handIn: EnumHand): ActionResult<ItemStack> {
        worldIn onServer {
            playerIn exp if (playerIn.isSneaking) 100 else 20
        }

        return super.onItemRightClick(worldIn, playerIn, handIn)
    }
}
