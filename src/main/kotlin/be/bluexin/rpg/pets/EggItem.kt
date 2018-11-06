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

import be.bluexin.rpg.BlueRPG
import be.bluexin.rpg.stats.StatCapability
import com.teamwizardry.librarianlib.features.base.item.ItemMod
import com.teamwizardry.librarianlib.features.kotlin.tagCompound
import com.teamwizardry.librarianlib.features.saving.AbstractSaveHandler
import com.teamwizardry.librarianlib.features.saving.NamedDynamic
import com.teamwizardry.librarianlib.features.saving.Savable
import com.teamwizardry.librarianlib.features.saving.Save
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
    override fun onItemUse(
        player: EntityPlayer,
        worldIn: World,
        pos: BlockPos,
        hand: EnumHand,
        facing: EnumFacing,
        hitX: Float,
        hitY: Float,
        hitZ: Float
    ): EnumActionResult {
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
            applyItemEntityDataToEntity(worldIn, player, itemstack, entity)
            worldIn.spawnEntity(entity)

            if (!player.capabilities.isCreativeMode) itemstack.shrink(1)

            return EnumActionResult.SUCCESS
        }
    }

    private fun applyItemEntityDataToEntity(
        entityWorld: World,
        player: EntityPlayer?,
        stack: ItemStack,
        targetEntity: EntityPet
    ) {
        val minecraftserver = entityWorld.minecraftServer

        if (minecraftserver != null) {
            val itemNbt = stack.tagCompound

            if (itemNbt != null && itemNbt.hasKey("EntityTag", 10)) {
                if (!entityWorld.isRemote
                    && targetEntity.ignoreItemEntityData()
                    && (player == null || !minecraftserver.playerList.canSendCommands(player.gameProfile))
                ) return

                val oldEntityNbt = targetEntity.writeToNBT(NBTTagCompound())
                val uuid = targetEntity.uniqueID
                oldEntityNbt.merge(itemNbt.getCompoundTag("EntityTag"))
                targetEntity.readFromNBT(oldEntityNbt)
                targetEntity.setUniqueId(uuid)
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

    override fun onUpdate(stack: ItemStack, worldIn: World, entityIn: Entity, itemSlot: Int, isSelected: Boolean) {
        super.onUpdate(stack, worldIn, entityIn, itemSlot, isSelected)

        if (worldIn.totalWorldTime % 20 == 0L) {
            val data = EggData()
            val tag = stack.tagCompound
            if (tag != null) AbstractSaveHandler.readAutoNBT(
                data,
                tag.getCompoundTag("EntityTag").getCompoundTag("auto"),
                false
            )
            if (data.shouldHatch) return
            ++data.secondsLived
            if (data.shouldHatch) BlueRPG.LOGGER.warn("Hatching!") // TODO
            /*else {*/
            val newTag = tagCompound {
                "EntityTag" to tagCompound {
                    "auto" to AbstractSaveHandler.writeAutoNBT(data, false)
                }
            }
            tag?.merge(newTag)
            stack.tagCompound = tag ?: newTag
            /*}*/
        }
    }
}

@Savable
@NamedDynamic(resourceLocation = "b:ed")
data class EggData(
    @Save var name: String = "Unnamed",
    @Save var movementType: PetMovementType = PetMovementType.BOUNCE,
    @Save var hatchTimeSeconds: Int = 300,
    @Save var secondsLived: Int = 0,
    @Save var stepSound: String = "TODO",
    @Save var idleSound: String = "TODO",
    @Save var interactSound: String = "TODO",
    @Save var loopingParticle: String = "TODO"
) : StatCapability {
    val shouldHatch get() = secondsLived >= hatchTimeSeconds

    fun loadFrom(stack: ItemStack, other: EggData) {
        val tag = stack.tagCompound
        val newTag = tagCompound {
            "EntityTag" to tagCompound {
                "auto" to AbstractSaveHandler.writeAutoNBT(other, false)
            }
        }
        if (tag != null) newTag.merge(tag)
        stack.tagCompound = newTag
    }

    override fun copy() = this.copy(name = name)
}

val ItemStack.eggData
    get() = if (item is EggItem) {
        EggData().also {
            if (tagCompound != null && tagCompound!!.getCompoundTag("EntityTag").hasKey(
                    "auto",
                    10
                )
            ) AbstractSaveHandler.readAutoNBT(
                it,
                tagCompound!!.getCompoundTag("EntityTag").getCompoundTag("auto"),
                false
            )
        }
    } else null