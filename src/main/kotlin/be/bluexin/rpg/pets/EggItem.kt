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

package be.bluexin.rpg.pets

import com.teamwizardry.librarianlib.features.base.item.ItemMod
import moe.plushie.armourers_workshop.utils.SkinNBTHelper
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumActionResult
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

object EggItem : ItemMod("egg") {
    override fun onItemUse(player: EntityPlayer, worldIn: World, pos: BlockPos, hand: EnumHand, facing: EnumFacing, hitX: Float, hitY: Float, hitZ: Float): EnumActionResult {
        val itemstack = player.getHeldItem(hand)

        if (worldIn.isRemote) {
            return EnumActionResult.SUCCESS
        } else if (!player.canPlayerEdit(pos.offset(facing), facing, itemstack)) {
            return EnumActionResult.FAIL
        } else {
            val blockpos = pos.offset(facing)
            val entity = EntityPet(worldIn)
            entity.skinPointer = SkinNBTHelper.getSkinDescriptorFromStack(itemstack)
            entity.setPosition(blockpos.x + .5, blockpos.y + this.getYOffset(worldIn, blockpos), blockpos.z + .5)
            entity.setOwner(player)
            worldIn.spawnEntity(entity)

            applyItemEntityDataToEntity(worldIn, player, itemstack, entity)

            if (!player.capabilities.isCreativeMode) {
                itemstack.shrink(1)
            }

            return EnumActionResult.SUCCESS
        }
    }

    private fun applyItemEntityDataToEntity(entityWorld: World, player: EntityPlayer?, stack: ItemStack, targetEntity: Entity?) {
        // TODO: custom stuff
        val minecraftserver = entityWorld.minecraftServer

        if (minecraftserver != null && targetEntity != null) {
            val nbttagcompound = stack.tagCompound

            if (nbttagcompound != null && nbttagcompound.hasKey("EntityTag", 10)) {
                if (!entityWorld.isRemote && targetEntity.ignoreItemEntityData() && (player == null || !minecraftserver.playerList.canSendCommands(player.gameProfile))) {
                    return
                }

                val nbttagcompound1 = targetEntity.writeToNBT(NBTTagCompound())
                val uuid = targetEntity.uniqueID
                nbttagcompound1.merge(nbttagcompound.getCompoundTag("EntityTag"))
                targetEntity.setUniqueId(uuid)
                targetEntity.readFromNBT(nbttagcompound1)
            }
        }
    }

    private fun getYOffset(world: World, pos: BlockPos): Double {
        val axisalignedbb = AxisAlignedBB(pos).expand(0.0, -1.0, 0.0)
        val list = world.getCollisionBoxes(null, axisalignedbb)

        return if (list.isEmpty()) {
            0.0
        } else {
            var y = axisalignedbb.minY
            for (axisalignedbb1 in list) y = Math.max(axisalignedbb1.maxY, y)
            y - pos.y.toDouble()
        }
    }
}