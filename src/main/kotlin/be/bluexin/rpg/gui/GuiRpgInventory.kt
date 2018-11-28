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

import be.bluexin.rpg.inventory.RPGContainer
import com.teamwizardry.librarianlib.features.gui.components.ComponentSprite
import com.teamwizardry.librarianlib.features.gui.components.ComponentVoid
import com.teamwizardry.librarianlib.features.guicontainer.ComponentSlot
import com.teamwizardry.librarianlib.features.guicontainer.GuiContainerBase
import com.teamwizardry.librarianlib.features.guicontainer.builtin.BaseLayouts
import com.teamwizardry.librarianlib.features.helpers.vec
import net.minecraft.client.gui.inventory.GuiContainerCreative

class GuiRpgInventory(private val ct: RPGContainer) : GuiContainerBase(ct, 258, 250) {
    init {
        val bg = ComponentSprite(Textures.INVENTORY_BG, 0, 0)
        mainComponents.add(bg)

        bg.add(PlayerLayout(ct.invPlayer).apply {
            main.pos = vec(13, 137)
            armor.pos = vec(13, 7)
            armor.isVisible = true
        }.root)
    }

    override fun updateScreen() {
        if (this.mc.playerController.isInCreativeMode) this.mc.displayGuiScreen(GuiContainerCreative(this.mc.player))
    }

    override fun initGui() =
        if (this.mc.playerController.isInCreativeMode) this.mc.displayGuiScreen(GuiContainerCreative(this.mc.player))
        else super.initGui()
}

class PlayerLayout(player: RPGContainer.InventoryWrapperPlayer) {
    val armor = ComponentVoid(0, 0).apply {
        isVisible = false
        add(
            ComponentSlot(player.head, 0, 0),
            ComponentSlot(player.chest, 0, 18),
            ComponentSlot(player.legs, 0, 2 * 18),
            ComponentSlot(player.feet, 0, 3 * 18),
            ComponentSlot(player.offhand, 0, 4 * 18),
            ComponentSlot(player.egg, 0, 5 * 18)
        )
    }
    val mainLayout = BaseLayouts.grid(player.main, 13)
    val hotbar = ComponentVoid(0, 92).apply {
        player.hotbar.forEachIndexed { index, slot ->
            add(ComponentSlot(slot, index * 18, 0))
        }
    }
    val bags = ComponentVoid(180, 92).apply {
        player.bags.forEachIndexed { index, slot ->
            add(ComponentSlot(slot, index * 18, 0))
        }
    }
    val main = ComponentVoid(0, 0).apply {
        add(mainLayout.root, hotbar, bags)
    }
    val root = ComponentVoid(0, 0).apply {
        add(armor, main)
    }
}
