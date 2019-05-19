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
import be.bluexin.rpg.classes.PlayerClass
import be.bluexin.rpg.classes.PlayerClassCollection
import be.bluexin.rpg.classes.playerClass
import be.bluexin.rpg.skills.SkillItem
import com.saomc.saoui.GLCore
import com.teamwizardry.librarianlib.features.gui.EnumMouseButton
import com.teamwizardry.librarianlib.features.gui.GuiBase
import com.teamwizardry.librarianlib.features.gui.component.GuiComponent
import com.teamwizardry.librarianlib.features.gui.component.GuiComponentEvents
import com.teamwizardry.librarianlib.features.gui.components.ComponentRaw
import com.teamwizardry.librarianlib.features.gui.components.ComponentSprite
import com.teamwizardry.librarianlib.features.gui.components.ComponentStack
import com.teamwizardry.librarianlib.features.gui.components.ComponentVoid
import com.teamwizardry.librarianlib.features.kotlin.Minecraft
import net.minecraft.util.ResourceLocation
import java.util.function.Consumer

class ClassesGui : GuiBase(WIDTH, HEIGHT) {

    companion object {
        private const val WIDTH = 258
        private const val HEIGHT = 139
    }

    init {
        val bg = ComponentSprite(Textures.CLASSES_BG, 0, 0)
        mainComponents.add(bg)

        val playerClass = Minecraft().player.playerClass
        repeat(3) {
            bg.add(ClassComponent(18 + it * 84, 6, playerClass[it], playerClass))
        }
    }

    override fun doesGuiPauseGame() = false
}

class ClassComponent(posX: Int, posY: Int, playerClass: PlayerClass?, playerData: PlayerClassCollection) :
    GuiComponent(posX, posY, 54, 96) {
    init {
        val bg = ComponentVoid(0, 0)
        root.add(bg)

        if (playerClass != null) {
            bg.add(ComponentRaw(19, 1, Consumer {
                GLCore.glBindTexture(playerClass.key)
                GLCore.glTexturedRectV2(x = .0, y = .0, width = 16.0, height = 16.0, textureW = 16, textureH = 16)
            }))
            playerClass.skills.asSequence().groupBy(Map.Entry<ResourceLocation, Int>::value).forEach { (tier, skills) ->
                bg.add(ComponentVoid(0, 23 + 25 * tier).apply {
                    skills.forEachIndexed { i, (skill, _) ->
                        add(SkillComponent(18 * i, 0, skill, playerData))
                    }
                })
            }
        } else {
            bg.add(ComponentSprite(Textures.CLASSES_EMPTY, 19, 1))
            // TODO: events for selecting class
        }

    }
}

class SkillComponent(posX: Int, posY: Int, skill: ResourceLocation, skills: PlayerClassCollection) :
    GuiComponent(posX, posY, 16, 22) {

    init {
        val bg = ComponentVoid(0, 0)
        root.add(bg)

        bg.add(ComponentStack(0, 0).apply {
            stack.setValue(SkillItem[skill])
            quantityText.add { _, _ -> null }
            BUS.hook(GuiComponentEvents.MouseClickEvent::class.java) {
                BlueRPG.LOGGER.info("Click $skill ${it.button} !")
                when (it.button) { // TODO: change this to packets (this is tmp demo code)
                    EnumMouseButton.LEFT -> ++skills[skill]
                    EnumMouseButton.RIGHT -> --skills[skill]
                    else -> Unit
                }
            }
        }) // TODO: events for leveling up/down/dragging to hotbarb

        repeat(3) {
            bg.add(ComponentCondition(it * 6 - 1, 17) { skills[skill] > it }.apply {
                add(ComponentSprite(Textures.CLASSES_ENABLED, 0, 0))
            })
        }
    }
}
