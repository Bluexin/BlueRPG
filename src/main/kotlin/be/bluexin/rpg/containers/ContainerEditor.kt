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

package be.bluexin.rpg.containers

import be.bluexin.rpg.BlueRPG
import be.bluexin.rpg.blocks.TileEditor
import be.bluexin.rpg.gui.GuiEditor
import com.teamwizardry.librarianlib.features.container.ContainerBase
import com.teamwizardry.librarianlib.features.container.GuiHandler
import com.teamwizardry.librarianlib.features.container.builtin.BaseWrappers
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.ResourceLocation

class ContainerEditor(player: EntityPlayer, val te: TileEditor) : ContainerBase(player) {

    val invPlayer = BaseWrappers.player(player)
    val invBlock = BaseWrappers.stacks(te)

    init {
        addSlots(invPlayer)
        addSlots(invBlock)

        transferRule().from(invPlayer.main).from(invPlayer.hotbar).deposit(invBlock.slotArray)
        transferRule().from(invBlock.slotArray).deposit(invPlayer.hotbar).deposit(invPlayer.main)
    }

    companion object {
        val NAME = ResourceLocation(BlueRPG.MODID, "container_editor")

        init {
            GuiHandler.registerBasicContainer(
                NAME, { player, _, tile ->
                    ContainerEditor(player, tile as TileEditor)
                }, { _, container ->
                    GuiEditor(container)
                }
            )
        }
    }
}
