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
import com.teamwizardry.librarianlib.features.sprite.Sprite
import com.teamwizardry.librarianlib.features.sprite.Texture
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.util.ResourceLocation

object Textures {

    private val BASE = Texture(ResourceLocation(BlueRPG.MODID, "textures/gui/gui_base.png"))
    val BG = BASE.getSprite("bg", 176, 166)
    val VANILLA_BG = Sprite(GuiContainer.INVENTORY_BACKGROUND, 176, 166)
    val SLOT = BASE.getSprite("slot", 18, 18)
    val PROGRESS_BG = BASE.getSprite("progression_bg", 14, 10)
    val PROGRESS_FG = BASE.getSprite("progression_fg", 12, 8)
    val POWER_BG = BASE.getSprite("power_bg", 8, 56)
    val POWER_FG = BASE.getSprite("power_fg", 8, 56)
    val NEW_INVENTORY = Texture(ResourceLocation(BlueRPG.MODID, "textures/gui/gui_inventory.png"))
    val INVENTORY_BG = NEW_INVENTORY.getSprite("bg", 258, 250)
    val SCROLLBAR_FG = NEW_INVENTORY.getSprite("scrollbar_fg", 8, 8)
}