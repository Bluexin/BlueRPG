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

package be.bluexin.rpg.blocks

import be.bluexin.rpg.BlueRPG
import be.bluexin.rpg.containers.ContainerEditor
import be.bluexin.rpg.gear.IRPGGear
import be.bluexin.rpg.gear.ItemGearToken
import be.bluexin.rpg.stats.GearStats
import be.bluexin.rpg.stats.TokenStats
import be.bluexin.rpg.stats.stats
import be.bluexin.rpg.stats.tokenStats
import be.bluexin.saomclib.onServer
import com.teamwizardry.librarianlib.features.autoregister.TileRegister
import com.teamwizardry.librarianlib.features.base.block.tile.BlockModContainer
import com.teamwizardry.librarianlib.features.base.block.tile.TileModInventory
import com.teamwizardry.librarianlib.features.base.block.tile.module.ModuleInventory
import com.teamwizardry.librarianlib.features.container.GuiHandler
import com.teamwizardry.librarianlib.features.kotlin.isNotEmpty
import com.teamwizardry.librarianlib.features.saving.Save
import net.minecraft.block.material.MapColor
import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.items.ItemStackHandler

object BlockEditor : BlockModContainer("editor", Material(MapColor.AIR)) {
    override fun createTileEntity(world: World, state: IBlockState) = TileEditor()

    override fun onBlockActivated(
        worldIn: World,
        pos: BlockPos,
        state: IBlockState,
        playerIn: EntityPlayer,
        hand: EnumHand,
        facing: EnumFacing,
        hitX: Float,
        hitY: Float,
        hitZ: Float
    ): Boolean {
        worldIn onServer {
            GuiHandler.open(ContainerEditor.NAME, playerIn, pos)
        }

        return true
    }
}

@TileRegister("${BlueRPG.MODID}:editor")
class TileEditor : TileModInventory(ModuleInventory(EditorInventory())) {

    @Save
    var tokenStats: TokenStats = TokenStats(ItemStack.EMPTY)
        get() = field.copy()
        set(value) {
            field = value.copy()
            world?.onServer(this@TileEditor::markDirty)
        }

    @Save
    var gearStats: GearStats = GearStats(ItemStack.EMPTY)
        get() = field.copy()
        set(value) {
            field = value.copy()
            world?.onServer(this@TileEditor::markDirty)
        }

    fun saveStats() {
        val i = module.handler.getStackInSlot(0)
        if (i.isNotEmpty) {
            i.tokenStats?.loadFrom(tokenStats)
            i.stats?.loadFrom(gearStats)
        }
    }

    fun loadStats() {
        val i = module.handler.getStackInSlot(0)
        val tstats = i.tokenStats
        if (tstats != null) this.tokenStats = tstats
        else {
            val gstats = i.stats
            if (gstats != null) this.gearStats = gstats
        }
    }

    private class EditorInventory : ItemStackHandler(1) {
        override fun isItemValid(slot: Int, stack: ItemStack): Boolean {
            val item = stack.item
            return item is IRPGGear || item is ItemGearToken
        }

        override fun insertItem(slot: Int, stack: ItemStack, simulate: Boolean) =
            if (isItemValid(slot, stack)) super.insertItem(slot, stack, simulate) else stack
    }
}
