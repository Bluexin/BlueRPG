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
import com.teamwizardry.librarianlib.features.container.builtin.SlotTypeEquipment
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.EntityEquipmentSlot
import net.minecraft.util.ResourceLocation
import net.minecraftforge.items.IItemHandler
import net.minecraftforge.items.wrapper.InvWrapper

class RPGContainer(player: EntityPlayer) : ContainerBase(player) {

    val invPlayer = InventoryWrapperPlayer(InvWrapper(player.inventory), player)

    init {
        addSlots(invPlayer)

        transferRule().from(invPlayer.main).deposit(invPlayer.egg).deposit(invPlayer.armor).deposit(invPlayer.hotbar)
        transferRule().from(invPlayer.hotbar).deposit(invPlayer.egg).deposit(invPlayer.armor).deposit(invPlayer.main)
        transferRule().from(invPlayer.egg).from(invPlayer.armor).deposit(invPlayer.main).deposit(invPlayer.hotbar)
    }

    companion object {
        val NAME = ResourceLocation(BlueRPG.MODID, "container_rpg_player")

        init {
            GuiHandler.registerBasicContainer(
                NAME, { player, _, _ ->
                    RPGContainer(player)
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

        val hotbar = slots[rInventory.rpgHotbarIndices]
        val main = slots[rInventory.realMainIndices]
        val offhand = slots[rInventory.offHandIndex]
        val egg = slots[rInventory.eggIndex]
        val bags = slots[rInventory.bagIndices]

    }
}
