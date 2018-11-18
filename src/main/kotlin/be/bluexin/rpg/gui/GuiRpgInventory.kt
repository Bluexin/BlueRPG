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

package be.bluexin.rpg.gui

import be.bluexin.rpg.BlueRPG
import be.bluexin.rpg.pets.petStorage
import com.teamwizardry.librarianlib.features.container.ContainerBase
import com.teamwizardry.librarianlib.features.container.GuiHandler
import com.teamwizardry.librarianlib.features.container.builtin.BaseWrappers
import com.teamwizardry.librarianlib.features.gui.components.ComponentSprite
import com.teamwizardry.librarianlib.features.guicontainer.GuiContainerBase
import com.teamwizardry.librarianlib.features.guicontainer.builtin.BaseLayouts
import com.teamwizardry.librarianlib.features.helpers.vec
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.ResourceLocation

class GuiRpgInventory(private val ct: ContainerRpgPlayer) : GuiContainerBase(ct, 176, 166) {
    init {
        val bg = ComponentSprite(Textures.VANILLA_BG, 0, 0)
        mainComponents.add(bg)

        bg.add(BaseLayouts.player(ct.invPlayer).apply {
            main.pos = vec(8, 84)
            armor.pos = vec(8, 8)
            offhand.pos = vec(77, 62)
            armor.isVisible = true
            offhand.isVisible = true
        }.root)

//        bg.add(ComponentSprite(Textures.SLOT, 7, 7))
//        bg.add(ComponentSlot(ct.invBlock.slotArray.first(), 8, 8))

    }
}

class ContainerRpgPlayer(player: EntityPlayer) : ContainerBase(player) {
    val invPlayer = BaseWrappers.player(player)
    val invPet = BaseWrappers.stacks(player.petStorage)

    init {
        addSlots(invPlayer)
        addSlots(invPet)

        transferRule().from(invPlayer.main).from(invPlayer.hotbar).deposit(invPet.slotArray).deposit(invPlayer.armor)
        transferRule().from(invPet.slotArray).from(invPlayer.armor).deposit(invPlayer.hotbar).deposit(invPlayer.main)
    }

    companion object {
        val NAME = ResourceLocation(BlueRPG.MODID, "container_rpg_player")

        init {
            GuiHandler.registerBasicContainer(
                NAME, { player, _, _ ->
                    ContainerRpgPlayer(player)
                }, { _, container ->
                    GuiRpgInventory(container)
                }
            )
        }
    }
}