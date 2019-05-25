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

    private val NEW_INVENTORY = Texture(ResourceLocation(BlueRPG.MODID, "textures/gui/gui_inventory.png"))

    val SCROLLBAR_FG = NEW_INVENTORY.getSprite("scrollbar_fg", 8, 8)

    val INVENTORY_BG = NEW_INVENTORY.getSprite("inventory_bg", 258, 115)
    val EQUIPMENT_BG = NEW_INVENTORY.getSprite("equipment_bg", 258, 135)
    val CHEST_BG = NEW_INVENTORY.getSprite("chest_bg", 258, 135)
    val EMPTY_BG = NEW_INVENTORY.getSprite("empty_bg", 258, 135)
    val CUSTOMIZATION_BG = NEW_INVENTORY.getSprite("customization_bg", 258, 135)

    val HEAD_BG = NEW_INVENTORY.getSprite("head_bg", 18, 18)
    val CHESTPLATE_BG = NEW_INVENTORY.getSprite("chestplate_bg", 18, 18)
    val LEGS_BG = NEW_INVENTORY.getSprite("legs_bg", 18, 18)
    val FEET_BG = NEW_INVENTORY.getSprite("feet_bg", 18, 18)
    val OFFHAND_BG = NEW_INVENTORY.getSprite("offhand_bg", 18, 18)
    val EGG_BG = NEW_INVENTORY.getSprite("egg_bg", 18, 18)
    val GATHERING_BG = NEW_INVENTORY.getSprite("gathering_bg", 18, 18)
    val CRAFTING_BG = NEW_INVENTORY.getSprite("crafting_bg", 18, 18)
    val CLASS_BG = NEW_INVENTORY.getSprite("class_bg", 18, 18)
    val BAG_BG = NEW_INVENTORY.getSprite("bag_bg", 18, 18)
    val COG_BG = NEW_INVENTORY.getSprite("cog_bg", 18, 18)
    val SLOT_BG = NEW_INVENTORY.getSprite("slot_bg", 18, 18)

    val MANA_BAR = ResourceLocation(BlueRPG.MODID, "textures/hud/mana_bar.png")

    private val CLASSES = Texture(ResourceLocation(BlueRPG.MODID, "textures/gui/class.png"))
    val CLASSES_BG = CLASSES.getSprite("bg", 258, 139)
    val CLASSES_MASK = CLASSES.getSprite("mask", 8, 8)
    val CLASSES_ENABLED = CLASSES.getSprite("enabled", 6, 6)
    val CLASSES_SELECTION = CLASSES.getSprite("class_select", 26, 26)
}