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

package be.bluexin.rpg.gui

import be.bluexin.rpg.containers.RPGEnderChestContainer
import com.teamwizardry.librarianlib.features.gui.components.ComponentSprite
import com.teamwizardry.librarianlib.features.gui.components.ComponentVoid
import com.teamwizardry.librarianlib.features.guicontainer.GuiContainerBase
import com.teamwizardry.librarianlib.features.guicontainer.builtin.BaseLayouts
import com.teamwizardry.librarianlib.features.helpers.vec

class GuiRpgEnderInventory(private val ct: RPGEnderChestContainer) : GuiContainerBase(ct, 258, 250) {
    init {
        val bg = ComponentVoid(0, 0)
        mainComponents.add(bg)
        val equipment = ComponentSprite(Textures.CHEST_BG, 0, 0)

        bg.add(
            equipment,
            ComponentSprite(Textures.INVENTORY_BG, 0, 135),
            PlayerLayout(ct.invPlayer).apply {
                main.pos = vec(13, 137)
                armor.pos = vec(13, 7)
                armor.isVisible = true

                armor.children.forEach {
                    it.add(ComponentSprite(Textures.SLOT_BG, -1, -1).apply {
                        zIndex = -1
                    })
                }
            }.root,
            BaseLayouts.grid(ct.invChest, 9).apply {
                root.pos = vec(49, 57)
            }.root
        )


    }
}
