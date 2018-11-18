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

import be.bluexin.rpg.stats.StatCapability
import be.bluexin.saomclib.onServer
import com.teamwizardry.librarianlib.features.base.item.ItemMod
import com.teamwizardry.librarianlib.features.kotlin.localize
import com.teamwizardry.librarianlib.features.kotlin.tagCompound
import com.teamwizardry.librarianlib.features.saving.AbstractSaveHandler
import com.teamwizardry.librarianlib.features.saving.NamedDynamic
import com.teamwizardry.librarianlib.features.saving.Savable
import com.teamwizardry.librarianlib.features.saving.Save
import moe.plushie.armourers_workshop.utils.SkinNBTHelper
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

object EggItem : ItemMod("egg") {
    override fun addInformation(stack: ItemStack, worldIn: World?, tooltip: MutableList<String>, flagIn: ITooltipFlag) {
//        super.addInformation(stack, worldIn, tooltip, flagIn)

        val data = stack.eggData ?: return
        if (data.isHatched) tooltip.add("rpg.pet.hatched".localize())
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

    fun onUpdateInPetSlot(
        player: EntityPlayer,
        stack: ItemStack,
        world: World,
        petStorage: PetStorage
    ) {
        if (world.totalWorldTime % 20 == 0L) {
            val data = EggData()
            val tag = stack.tagCompound
            if (tag != null) AbstractSaveHandler.readAutoNBT(
                data,
                tag.getCompoundTag("EntityTag").getCompoundTag("auto"),
                false
            )
            if (data.isHatched) {
                world onServer {
                    val p = petStorage.petEntity
                    if (p?.isDead != false) {
                        val blockpos = player.position
                        val entity = EntityPet(world)
                        entity.skinPointer = SkinNBTHelper.getSkinDescriptorFromStack(stack)
                        entity.setPosition(
                            blockpos.x + .5,
                            blockpos.y + this.getYOffset(world, blockpos),
                            blockpos.z + .5
                        )
                        entity.setOwner(player)
                        applyItemEntityDataToEntity(world, player, stack, entity)
                        world.spawnEntity(entity)
                        petStorage.petEntity = entity
                    }
                }
                return
            }
            ++data.secondsLived
            if (data.shouldHatch) data.hatch()

            val newTag = tagCompound {
                "EntityTag" to tagCompound {
                    "auto" to AbstractSaveHandler.writeAutoNBT(data, false)
                }
            }
            tag?.merge(newTag)
            stack.tagCompound = tag ?: newTag
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
    @Save var primaryColor: Int = 0,
    @Save var secondaryColor: Int = 0,
    @Save var stepSound: String = "TODO",
    @Save var idleSound: String = "TODO",
    @Save var interactSound: String = "TODO",
    @Save var loopingParticle: String = "TODO",
    @Save var isHatched: Boolean = false
) : StatCapability {
    val shouldHatch get() = secondsLived >= hatchTimeSeconds

    fun loadFrom(stack: ItemStack, other: EggData) {
        val tag = stack.tagCompound
        val newTag = tagCompound {
            "EntityTag" to tagCompound {
                "auto" to AbstractSaveHandler.writeAutoNBT(other, false)
            }
        }
        tag?.merge(newTag)
        stack.tagCompound = tag ?: newTag
    }

    fun hatch() {
        isHatched = true
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