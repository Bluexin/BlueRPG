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

package be.bluexin.rpg.inventory

import be.bluexin.rpg.BlueRPG
import be.bluexin.rpg.gui.GuiRpgEnderInventory
import com.teamwizardry.librarianlib.features.container.ContainerBase
import com.teamwizardry.librarianlib.features.container.GuiHandler
import com.teamwizardry.librarianlib.features.container.builtin.BaseWrappers
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.InventoryEnderChest
import net.minecraft.util.ResourceLocation
import net.minecraftforge.items.wrapper.InvWrapper

class RPGEnderChestContainer(player: EntityPlayer, chest: InventoryEnderChest) : ContainerBase(player) {

    val invPlayer = RPGContainer.InventoryWrapperPlayer(InvWrapper(player.inventory), player)
    val invChest = BaseWrappers.inventory(chest)

    init {
        addSlots(invPlayer)
        addSlots(invChest)

        transferRule().from(invPlayer.slotArray).deposit(invChest.slotArray)
        transferRule().from(invChest.slotArray).deposit(invPlayer.main).deposit(invPlayer.hotbar)
    }

    companion object {
        val NAME = ResourceLocation(BlueRPG.MODID, "container_rpg_ender_player")

        init {
            GuiHandler.registerBasicContainer(
                NAME, { player, _, _ ->
                    RPGEnderChestContainer(player, player.inventoryEnderChest)
                }, { _, container ->
                    GuiRpgEnderInventory(container)
                }
            )
        }
    }
}