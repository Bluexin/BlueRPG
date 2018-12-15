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

import be.bluexin.saomclib.onServer
import com.mojang.authlib.GameProfile
import com.teamwizardry.librarianlib.features.autoregister.TileRegister
import com.teamwizardry.librarianlib.features.base.block.tile.BlockModContainer
import com.teamwizardry.librarianlib.features.base.block.tile.TileModInventoryTickable
import com.teamwizardry.librarianlib.features.kotlin.isNotEmpty
import com.teamwizardry.librarianlib.features.saving.Save
import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraft.world.WorldServer
import net.minecraftforge.common.util.FakePlayerFactory
import java.lang.ref.WeakReference
import java.util.*

object BlockCaster : BlockModContainer("block_caster", Material.CLOTH) {
    override fun createTileEntity(world: World, state: IBlockState): TileEntity? {
        return BlockCasterTE()
    }

    override fun canConnectRedstone(
        state: IBlockState?,
        world: IBlockAccess?,
        pos: BlockPos?,
        side: EnumFacing?
    ): Boolean {
        return true
    }
}

@TileRegister("block_caster")
class BlockCasterTE : TileModInventoryTickable(1) {

    @Save
    private var time = 0

    private val gp by lazy { GameProfile(UUID.randomUUID(), "block_caster") }
    private val fpr by lazy {
        WeakReference(FakePlayerFactory.get(world as WorldServer, gp).apply {
            posX = pos.x + 0.5
            posY = pos.y.toDouble()
            posZ = pos.z - 0.5
        })
    }

    override fun tick() {
        world onServer {
            if (world.isBlockPowered(pos) && time++ % 60 == 0) {

                val iss = getStackInSlot(0)
                if (iss.isNotEmpty) {
                    val fp = fpr.get()
                    if (fp != null) {
                        when (iss.maxItemUseDuration) {
                            72000 -> iss.onPlayerStoppedUsing(world, fp, 0)
                            0 -> iss.useItemRightClick(world, fp, EnumHand.MAIN_HAND)
                            else -> iss.onItemUseFinish(world, fp)
                        }
                    }
                }
                time = 1
            }
        }
    }
}