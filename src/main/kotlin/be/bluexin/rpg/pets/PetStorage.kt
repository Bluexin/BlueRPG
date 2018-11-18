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
import be.bluexin.saomclib.capabilities.AbstractEntityCapability
import be.bluexin.saomclib.capabilities.Key
import com.teamwizardry.librarianlib.features.saving.AbstractSaveHandler
import com.teamwizardry.librarianlib.features.saving.Save
import com.teamwizardry.librarianlib.features.saving.SaveInPlace
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTBase
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraft.util.ResourceLocation
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.CapabilityInject
import net.minecraftforge.common.util.FakePlayer
import net.minecraftforge.event.entity.EntityJoinWorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.PlayerEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import net.minecraftforge.items.IItemHandlerModifiable
import net.minecraftforge.items.SlotItemHandler
import java.lang.ref.WeakReference

@SaveInPlace
class PetStorage : AbstractEntityCapability(), IItemHandlerModifiable {

    @Save
    var stack: ItemStack = ItemStack.EMPTY

    @Save
    internal var petID: Int = -1

    private var petEntityRef: WeakReference<EntityPet>? = null
        get() {
            if (field == null && petID > 0) {
                val p = reference.get()?.world?.getEntityByID(petID) as? EntityPet
                if (p == null) petID = -1
                else field = WeakReference(p)
            }

            return field
        }
        set(value) {
            field = value
            petID = value?.get()?.entityId ?: -1
        }

    var petEntity: EntityPet?
        get() = petEntityRef?.get()
        set(value) {
            petEntityRef = if (value != null) WeakReference(value) else null
        }

    fun killPet() {
        petEntity?.setDead()
        petEntity = null
    }

    override fun insertItem(slot: Int, stack: ItemStack, simulate: Boolean): ItemStack =
        if (this.stack.isEmpty && stack.item is EggItem) {
            if (!simulate) this.stack = stack.copy().apply { count = 1 }
            stack.copy().apply { this.shrink(1) }
        } else stack

    override fun getStackInSlot(slot: Int) = stack

    override fun getSlotLimit(slot: Int) = 1

    override fun getSlots() = 1

    override fun extractItem(slot: Int, amount: Int, simulate: Boolean): ItemStack {
        if (amount < 1) return ItemStack.EMPTY
        val t = this.stack
        if (!simulate) this.stack = ItemStack.EMPTY
        this.killPet()
        return t
    }

    override fun setStackInSlot(slot: Int, stack: ItemStack) {
        this.stack = stack
        this.killPet()
    }

    internal object Storage : Capability.IStorage<PetStorage> {
        override fun readNBT(
            capability: Capability<PetStorage>,
            instance: PetStorage,
            side: EnumFacing?,
            nbt: NBTBase
        ) {
            val nbtTagCompound = nbt as? NBTTagCompound ?: return
            AbstractSaveHandler.readAutoNBT(instance, nbtTagCompound.getTag(KEY.toString()), false)
        }

        override fun writeNBT(capability: Capability<PetStorage>, instance: PetStorage, side: EnumFacing?): NBTBase {
            return NBTTagCompound().also {
                it.setTag(
                    KEY.toString(),
                    AbstractSaveHandler.writeAutoNBT(instance, false)
                )
            }
        }
    }

    companion object {
        @Key
        val KEY = ResourceLocation(BlueRPG.MODID, "pet_storage")

        @CapabilityInject(PetStorage::class)
        lateinit var capability: Capability<PetStorage>
            internal set

        init {
            MinecraftForge.EVENT_BUS.register(object {
                @SubscribeEvent
                fun onTick(event: TickEvent.PlayerTickEvent) {
                    if (event.phase == TickEvent.Phase.END) {
                        val petStorage = event.player.petStorage
                        val i = petStorage.stack.item
                        if (i is EggItem) i.onUpdateInPetSlot(
                            event.player,
                            petStorage.stack,
                            event.player.world,
                            petStorage
                        )
                    }
                }

                @SubscribeEvent
                fun onPlayerLogout(event: PlayerEvent.PlayerLoggedOutEvent) = event.player.petStorage.killPet()

                @SubscribeEvent
                fun onPlayerAddedToWorld(event: EntityJoinWorldEvent) {
                    with(event.entity) {
                        if (this is EntityPlayer && this !is FakePlayer) {
                            inventoryContainer.addSlotToContainer(object :
                                SlotItemHandler(petStorage, 0, 77, 44) {
                                @SideOnly(Side.CLIENT)
                                override fun getSlotTexture(): String? {
                                    return "minecraft:items/spawn_egg"
                                }
                            })
                            MinecraftForge.EVENT_BUS.unregister(this)
                        }
                    }

                }
            })
        }
    }
}

val EntityPlayer.petStorage get() = this.getCapability(PetStorage.capability, null)!!