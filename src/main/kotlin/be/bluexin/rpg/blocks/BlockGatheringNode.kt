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

package be.bluexin.rpg.blocks

import be.bluexin.rpg.containers.GatheringContainer
import be.bluexin.rpg.jobs.GatheringCapability
import be.bluexin.saomclib.onServer
import com.teamwizardry.librarianlib.features.autoregister.TileRegister
import com.teamwizardry.librarianlib.features.base.block.BlockModDirectional
import com.teamwizardry.librarianlib.features.base.block.tile.TileMod
import com.teamwizardry.librarianlib.features.base.block.tile.TileModInventoryTickable
import com.teamwizardry.librarianlib.features.base.block.tile.module.ModuleCapability
import com.teamwizardry.librarianlib.features.container.GuiHandler
import com.teamwizardry.librarianlib.features.saving.Module
import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World

object BlockGatheringNode : BlockModDirectional("gathering_node", Material.IRON, true) {
    init {
        setBlockUnbreakable()
    }

    @Suppress("OverridingDeprecatedMember", "DEPRECATION")
    override fun getBoundingBox(state: IBlockState, source: IBlockAccess, pos: BlockPos): AxisAlignedBB {
        return super.getBoundingBox(state, source, pos)
    } // TODO

    @Suppress("OverridingDeprecatedMember", "DEPRECATION")
    override fun getCollisionBoundingBox(
        blockState: IBlockState,
        worldIn: IBlockAccess,
        pos: BlockPos
    ): AxisAlignedBB? {
        return super.getCollisionBoundingBox(blockState, worldIn, pos)
    } // TODO

    //region Container stuff

    override fun createTileEntity(world: World, state: IBlockState) = BlockGatheringNodeTE()

    @Suppress("OverridingDeprecatedMember")
    override fun eventReceived(
        state: IBlockState,
        worldIn: World,
        pos: BlockPos,
        eventID: Int,
        eventParam: Int
    ): Boolean {
        val tile = worldIn.getTileEntity(pos) ?: return false
        return tile.receiveClientEvent(eventID, eventParam)
    }

    override fun hasTileEntity(state: IBlockState?) = true

    override fun breakBlock(worldIn: World, pos: BlockPos, state: IBlockState) {
        val tile = worldIn.getTileEntity(pos)
        (tile as? TileMod)?.onBreak()
        super.breakBlock(worldIn, pos, state)
    }

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
    ) = if (playerIn.isCreative) {
        worldIn onServer {
            GuiHandler.open(GatheringContainer.NAME, playerIn, pos)
        }
        true
    } else {
        val tile = worldIn.getTileEntity(pos)
        tile is TileMod && tile.onClicked(playerIn, hand, facing, hitX, hitY, hitZ)
    }

    @Suppress("OverridingDeprecatedMember")
    override fun hasComparatorInputOverride(state: IBlockState) = true

    @Suppress("OverridingDeprecatedMember", "DEPRECATION")
    override fun getComparatorInputOverride(blockState: IBlockState, worldIn: World, pos: BlockPos): Int {
        val tile = worldIn.getTileEntity(pos)
        if (tile is TileMod) return tile.getComparatorOverride()
        return super.getComparatorInputOverride(blockState, worldIn, pos)
    }

    //endregion
}

@TileRegister("gathering_node")
class BlockGatheringNodeTE : TileModInventoryTickable(1) {

    @Module
    val gatheringHandler = GatheringModule()

    override fun tick() = Unit
}

class GatheringModule : ModuleCapability<GatheringCapability>(GatheringCapability.CAP_INSTANCE, GatheringCapability()) {
    override fun onUpdate(tile: TileMod) = handler.tick()
    override fun getComparatorOutput(tile: TileMod) = handler.getComparatorOutput()
    override fun hasComparatorOutput() = true
}