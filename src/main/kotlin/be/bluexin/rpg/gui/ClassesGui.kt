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

import be.bluexin.rpg.PacketChangeSkill
import be.bluexin.rpg.PacketSetClass
import be.bluexin.rpg.classes.PlayerClassCollection
import be.bluexin.rpg.classes.PlayerClassRegistry
import be.bluexin.rpg.classes.playerClass
import be.bluexin.rpg.skills.SkillItem
import com.saomc.saoui.GLCore
import com.saomc.saoui.api.elements.neo.basicAnimation
import com.saomc.saoui.api.elements.neo.plusAssign
import com.teamwizardry.librarianlib.features.animator.Easing
import com.teamwizardry.librarianlib.features.gui.EnumMouseButton
import com.teamwizardry.librarianlib.features.gui.GuiBase
import com.teamwizardry.librarianlib.features.gui.component.GuiComponent
import com.teamwizardry.librarianlib.features.gui.component.GuiComponentEvents
import com.teamwizardry.librarianlib.features.gui.components.ComponentRaw
import com.teamwizardry.librarianlib.features.gui.components.ComponentSprite
import com.teamwizardry.librarianlib.features.gui.components.ComponentStack
import com.teamwizardry.librarianlib.features.gui.components.ComponentVoid
import com.teamwizardry.librarianlib.features.helpers.vec
import com.teamwizardry.librarianlib.features.kotlin.Minecraft
import com.teamwizardry.librarianlib.features.kotlin.localize
import com.teamwizardry.librarianlib.features.math.Vec2d
import com.teamwizardry.librarianlib.features.network.PacketHandler
import net.minecraft.util.ResourceLocation
import java.util.*
import java.util.function.Consumer

class ClassesGui : GuiBase(WIDTH, HEIGHT) {

    companion object {
        private const val WIDTH = 258
        private const val HEIGHT = 139
    }

    private val comps: MutableList<ClassComponent> = LinkedList()

    init {
        val bg = ComponentSprite(Textures.CLASSES_BG, 0, 0)
        mainComponents.add(bg)

        val playerClass = Minecraft().player.playerClass
        repeat(3) {
            bg.add(ClassComponent(18 + it * 84, 6, playerClass, it).also { comps.add(it) })
        }
    }

    fun refresh() {
        comps.forEach(ClassComponent::refresh)
    }

    override fun doesGuiPauseGame() = false
}

class ClassComponent(posX: Int, posY: Int, private val playerData: PlayerClassCollection, private val classIndex: Int) :
    GuiComponent(posX, posY, 54, 96) {

    private var bg: GuiComponent? = null
    private val selectionComponent = ClassSelectionComponent(27, 8, playerData, classIndex)

    init {
        this.add(selectionComponent)
        refresh()
    }

    fun refresh() {
        selectionComponent.refresh()
        if (bg != null) this.relationships.remove(bg!!)
        bg = ComponentVoid(0, 0)
        this.add(bg!!)

        val playerClass = playerData[classIndex]
        bg!!.add(ClassButton(18, 0, playerClass?.key).apply {
            hook<GuiComponentEvents.MouseClickEvent> {
                if (it.button == EnumMouseButton.LEFT) selectionComponent.open()
                else if (it.button == EnumMouseButton.RIGHT) PacketHandler.NETWORK.sendToServer(
                    PacketSetClass(null, classIndex)
                )
            }
        })

        playerClass?.skills?.asSequence()?.groupBy(Map.Entry<ResourceLocation, Int>::value)?.forEach { (tier, skills) ->
            bg!!.add(ComponentVoid(1, 23 + 25 * tier).apply {
                skills.forEachIndexed { i, (skill, _) ->
                    add(SkillComponent(18 * i, 0, skill, playerData))
                }
            })
        }
    }
}

class SkillComponent(posX: Int, posY: Int, skill: ResourceLocation, skills: PlayerClassCollection) :
    GuiComponent(posX, posY, 16, 22) {

    init {
        val bg = ComponentVoid(0, 0)
        this.add(bg)

        bg.add(ComponentStack(0, 0).apply {
            stack.setValue(SkillItem[skill])
            quantityText.add { _, _ -> null }
            hook<GuiComponentEvents.MouseClickEvent> {
                when (it.button) {
                    EnumMouseButton.LEFT -> PacketHandler.NETWORK.sendToServer(PacketChangeSkill(skill, true))
                    EnumMouseButton.RIGHT -> PacketHandler.NETWORK.sendToServer(PacketChangeSkill(skill, false))
                    else -> Unit
                }
            }
        }) // TODO: dragging skills to hotbar

        repeat(3) {
            bg.add(ComponentCondition(it * 6 - 1, 17) { skills[skill] > it }.apply {
                add(ComponentSprite(Textures.CLASSES_ENABLED, 0, 0))
            })
        }
    }
}

class ClassSelectionComponent(
    posX: Int,
    posY: Int,
    private val skills: PlayerClassCollection,
    private val classIndex: Int
) : GuiComponent(posX, posY) {
    private lateinit var targetSize: Vec2d
    private val middlePos: Vec2d
    private lateinit var targetPos: Vec2d
    private var bg: GuiComponent? = null
    private var open = false

    init {
        ++zIndex
        this.middlePos = this.pos

        hook<GuiComponentEvents.MouseClickOutsideEvent> {
            this.close()
        }
    }

    fun refresh() {
        val allClasses = PlayerClassRegistry.valuesCollection.asSequence().filter { it !in skills }
        val count = allClasses.count()

        this.targetSize = vec(if (count > 5) 94 else 4 + count * 18, (count / 5 + 1) * 18 + 4)
        this.size = vec(0, this.targetSize.y)
        this.targetPos = vec(this.middlePos.x - this.targetSize.x / 2, this.middlePos.y)

        this.clipping.clipToBounds = true

        if (bg != null) this.relationships.remove(bg!!)
        bg = ComponentVoid(2, 2)
        this.add(bg!!)

        allClasses.forEachIndexed { i, playerClass ->
            bg!!.add(ClassButton((i % 5) * 18, i / 5 * 18, playerClass.key).apply {
                hook<GuiComponentEvents.MouseClickEvent> {
                    if (it.button == EnumMouseButton.LEFT) {
                        PacketHandler.NETWORK.sendToServer(PacketSetClass(playerClass.key, classIndex))
                        this@ClassSelectionComponent.close()
                    }
                }
            })
        }
    }

    override var isVisible: Boolean
        get() = super.isVisible && this.size.x > 0
        set(value) {
            super.isVisible = value
        }

    fun open() {
        if (this.open) return
        this.open = true
        animator += basicAnimation(this, "size") {
            duration = 10f
            to = targetSize
            easing = Easing.easeOutQuint
        }
        animator += basicAnimation(this, "pos") {
            duration = 10f
            to = targetPos
            easing = Easing.easeOutQuint
        }
    }

    fun close() {
        if (!this.open) return
        this.open = false
        animator += basicAnimation(this, "size") {
            duration = 10f
            to = vec(0, targetSize.y)
            easing = Easing.easeOutQuint
        }
        animator += basicAnimation(this, "pos") {
            duration = 10f
            to = middlePos
            easing = Easing.easeOutQuint
        }
    }
}

class ClassButton(posX: Int, posY: Int, clazz: ResourceLocation? = null) : GuiComponent(posX, posY, 18, 18) {
    init {
        this.add(if (clazz == null) ComponentSprite(Textures.CLASSES_EMPTY, 1, 1) else ComponentRaw(1, 1, Consumer {
            GLCore.glBindTexture(clazz)
            GLCore.glTexturedRectV2(x = .0, y = .0, width = 16.0, height = 16.0, textureW = 16, textureH = 16)
        }))
        this.render.setTooltip(
            if (clazz == null) listOf("bluerpg.noclass".localize()) else listOf(
                "$clazz.name".localize(),
                "$clazz.description".localize()
            )
        )
    }
}
