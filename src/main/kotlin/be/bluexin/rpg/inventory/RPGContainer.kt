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

package be.bluexin.rpg.inventory

import be.bluexin.rpg.BlueRPG
import be.bluexin.rpg.gui.GuiRpgInventory
import com.teamwizardry.librarianlib.features.container.ContainerBase
import com.teamwizardry.librarianlib.features.container.GuiHandler
import com.teamwizardry.librarianlib.features.container.InventoryWrapper
import com.teamwizardry.librarianlib.features.container.SlotType
import com.teamwizardry.librarianlib.features.container.builtin.SlotTypeEquipment
import com.teamwizardry.librarianlib.features.container.internal.ContainerImpl
import com.teamwizardry.librarianlib.features.container.internal.SlotBase
import com.teamwizardry.librarianlib.features.kotlin.isNotEmpty
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.inventory.ClickType
import net.minecraft.inventory.ContainerPlayer
import net.minecraft.inventory.EntityEquipmentSlot
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumActionResult
import net.minecraft.util.EnumHand
import net.minecraft.util.ResourceLocation
import net.minecraftforge.items.IItemHandler
import net.minecraftforge.items.wrapper.InvWrapper
import java.util.*
import java.util.function.Consumer
import java.util.function.Predicate
import java.util.function.UnaryOperator
import java.util.stream.Stream

class RPGContainer(player: EntityPlayer, original: ContainerPlayer) : ContainerBase(player) {

    val invPlayer = InventoryWrapperPlayer(InvWrapper(player.inventory), player)

    init {
        addSlots(invPlayer)

        transferRule().from(invPlayer.main).deposit(invPlayer.egg).deposit(invPlayer.armor).deposit(invPlayer.hotbar)
        transferRule().from(invPlayer.hotbar).deposit(invPlayer.egg).deposit(invPlayer.armor).deposit(invPlayer.main)
        transferRule().from(invPlayer.egg).from(invPlayer.armor).deposit(invPlayer.main).deposit(invPlayer.hotbar)

        RPGContainerImpl(original)
    }

    companion object {
        val NAME = ResourceLocation(BlueRPG.MODID, "container_rpg_player")

        init {
            GuiHandler.registerBasicContainer(
                NAME, { player, _, _ ->
                    (player.inventoryContainer as ContainerImpl).container as RPGContainer
                }, { _, container ->
                    GuiRpgInventory(container)
                }
            )
        }
    }

    class InventoryWrapperPlayer(inv: IItemHandler, val player: EntityPlayer) : InventoryWrapper(inv) {

        val rInventory = player.inventory as RPGInventory
        val armor = slots[rInventory.armorIndices]
        val head = slots[rInventory.armorIndices.start + EntityEquipmentSlot.HEAD.index].apply {
            type = SlotTypeEquipment(player, EntityEquipmentSlot.HEAD)
        }
        val chest = slots[rInventory.armorIndices.start + EntityEquipmentSlot.CHEST.index].apply {
            type = SlotTypeEquipment(player, EntityEquipmentSlot.CHEST)
        }
        val legs = slots[rInventory.armorIndices.start + EntityEquipmentSlot.LEGS.index].apply {
            type = SlotTypeEquipment(player, EntityEquipmentSlot.LEGS)
        }
        val feet = slots[rInventory.armorIndices.start + EntityEquipmentSlot.FEET.index].apply {
            type = SlotTypeEquipment(player, EntityEquipmentSlot.FEET)
        }

        val hotbar = slots[rInventory.rpgHotbarIndices].apply {
            forEach {
                it.type = SlotTypeUsable
            }
        }
        val main = slots[rInventory.realMainIndices].apply {
            forEach {
                it.type = SlotTypeUsable
            }
        }
        val offhand = slots[rInventory.offHandIndex]
        val egg = slots[rInventory.eggIndex]
        val bags = slots[rInventory.bagIndices]
    }

    inner class RPGContainerImpl(private val vanillaSlots: ContainerPlayer) : ContainerImpl(this@RPGContainer) {
        private val ourSlots = this.inventorySlots

        init {
            this.inventorySlots = FakeSlotList()
        }

        override fun slotClick(slotId: Int, dragType: Int, clickTypeIn: ClickType, player: EntityPlayer): ItemStack =
            if (player.isCreative) vanillaSlots.slotClick(slotId, dragType, clickTypeIn, player)
            else super.slotClick(slotId, dragType, clickTypeIn, player)

        override fun transferStackInSlot(playerIn: EntityPlayer?, index: Int): ItemStack =
            if (player.isCreative) vanillaSlots.transferStackInSlot(playerIn!!, index)
            else super.transferStackInSlot(playerIn, index)

        private inner class FakeSlotList : MutableList<Slot> {
            override fun contains(element: Slot): Boolean =
                (if (player.isCreative) vanillaSlots.inventorySlots else ourSlots).contains(element)

            override fun addAll(elements: Collection<Slot>): Boolean =
                (if (player.isCreative) vanillaSlots.inventorySlots else ourSlots).addAll(elements)

            override fun addAll(index: Int, elements: Collection<Slot>): Boolean =
                (if (player.isCreative) vanillaSlots.inventorySlots else ourSlots).addAll(index, elements)

            override fun clear() =
                (if (player.isCreative) vanillaSlots.inventorySlots else ourSlots).clear()

            override fun replaceAll(operator: UnaryOperator<Slot>) =
                (if (player.isCreative) vanillaSlots.inventorySlots else ourSlots).replaceAll(operator)

            override fun parallelStream(): Stream<Slot> =
                (if (player.isCreative) vanillaSlots.inventorySlots else ourSlots).parallelStream()

            override fun listIterator(index: Int): MutableListIterator<Slot> =
                (if (player.isCreative) vanillaSlots.inventorySlots else ourSlots).listIterator(index)

            override fun listIterator(): MutableListIterator<Slot> =
                (if (player.isCreative) vanillaSlots.inventorySlots else ourSlots).listIterator()

            override fun removeAll(elements: Collection<Slot>): Boolean =
                (if (player.isCreative) vanillaSlots.inventorySlots else ourSlots).removeAll(elements)

            override fun add(element: Slot): Boolean =
                (if (player.isCreative) vanillaSlots.inventorySlots else ourSlots).add(element)

            override fun add(index: Int, element: Slot) =
                (if (player.isCreative) vanillaSlots.inventorySlots else ourSlots).add(index, element)

            override fun stream(): Stream<Slot> =
                (if (player.isCreative) vanillaSlots.inventorySlots else ourSlots).stream()

            override fun iterator(): MutableIterator<Slot> =
                (if (player.isCreative) vanillaSlots.inventorySlots else ourSlots).iterator()

            override fun get(index: Int): Slot =
                (if (player.isCreative) vanillaSlots.inventorySlots else ourSlots).get(index)

            override fun forEach(action: Consumer<in Slot>?) =
                (if (player.isCreative) vanillaSlots.inventorySlots else ourSlots).forEach(action)

            override fun spliterator(): Spliterator<Slot> =
                (if (player.isCreative) vanillaSlots.inventorySlots else ourSlots).spliterator()

            override fun equals(other: Any?): Boolean =
                (if (player.isCreative) vanillaSlots.inventorySlots else ourSlots) == other

            override fun hashCode(): Int =
                (if (player.isCreative) vanillaSlots.inventorySlots else ourSlots).hashCode()

            override fun indexOf(element: Slot): Int =
                (if (player.isCreative) vanillaSlots.inventorySlots else ourSlots).indexOf(element)

            override fun lastIndexOf(element: Slot): Int =
                (if (player.isCreative) vanillaSlots.inventorySlots else ourSlots).lastIndexOf(element)

            override fun isEmpty(): Boolean =
                (if (player.isCreative) vanillaSlots.inventorySlots else ourSlots).isEmpty()

            override fun removeAt(index: Int): Slot =
                (if (player.isCreative) vanillaSlots.inventorySlots else ourSlots).removeAt(index)

            override fun toString(): String =
                (if (player.isCreative) vanillaSlots.inventorySlots else ourSlots).toString()

            override fun remove(element: Slot): Boolean =
                (if (player.isCreative) vanillaSlots.inventorySlots else ourSlots).remove(element)

            override fun containsAll(elements: Collection<Slot>): Boolean =
                (if (player.isCreative) vanillaSlots.inventorySlots else ourSlots).containsAll(elements)

            override fun sort(c: Comparator<in Slot>) =
                (if (player.isCreative) vanillaSlots.inventorySlots else ourSlots).sortWith(c)

            override fun set(index: Int, element: Slot): Slot =
                (if (player.isCreative) vanillaSlots.inventorySlots else ourSlots).set(index, element)

            override fun removeIf(filter: Predicate<in Slot>): Boolean =
                (if (player.isCreative) vanillaSlots.inventorySlots else ourSlots).removeIf(filter)

            override fun retainAll(elements: Collection<Slot>): Boolean =
                (if (player.isCreative) vanillaSlots.inventorySlots else ourSlots).retainAll(elements)

            override fun subList(fromIndex: Int, toIndex: Int): MutableList<Slot> =
                (if (player.isCreative) vanillaSlots.inventorySlots else ourSlots).subList(fromIndex, toIndex)

            override val size: Int
                get() = (if (player.isCreative) vanillaSlots.inventorySlots else ourSlots).size
        }
    }
}

object SlotTypeUsable : SlotType() {
    override fun handleClick(
        slot: SlotBase,
        container: ContainerBase,
        dragType: Int,
        clickType: ClickType?,
        player: EntityPlayer
    ): Pair<Boolean, ItemStack> =
        if (!player.world.isRemote && slot.stack.isNotEmpty && !player.isCreative && clickType == ClickType.CLONE) {
            val r = (player as EntityPlayerMP).interactionManager.processItemUse(
                player,
                player.world,
                slot.stack,
                EnumHand.MAIN_HAND
            )
            if (r == EnumActionResult.PASS) super.handleClick(slot, container, dragType, clickType, player)
            else true to slot.stack
        } else super.handleClick(slot, container, dragType, clickType, player)
}
