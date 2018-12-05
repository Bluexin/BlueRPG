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

import com.teamwizardry.librarianlib.features.eventbus.EventCancelable
import com.teamwizardry.librarianlib.features.gui.component.GuiComponent
import com.teamwizardry.librarianlib.features.gui.component.GuiComponentEvents
import com.teamwizardry.librarianlib.features.helpers.vec
import com.teamwizardry.librarianlib.features.kotlin.clamp
import com.teamwizardry.librarianlib.features.kotlin.height
import com.teamwizardry.librarianlib.features.kotlin.width
import com.teamwizardry.librarianlib.features.math.Vec2d
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.entity.EntityLivingBase
import java.util.*
import kotlin.math.max

class ComponentCondition(posX: Int, posY: Int, private val condition: () -> Boolean) : GuiComponent(posX, posY) {
    override var isVisible: Boolean = super.isVisible
        get() {
            val v = field && condition()
            if (v == (history and 3 != 0)) {
                if (v != wasVisible) visibilityChanged?.invoke(v)
                wasVisible = v
            }
            history = history shl 1
            history = history or if (v) 1 else 0
            history = history and 3
            return wasVisible
        }

    private var wasVisible = false
    private var history = 0

    var visibilityChanged: ((Boolean) -> Unit)? = null

    fun clear() = relationships.forEachChild { relationships.remove(it) }
}

class ComponentScrollList(posX: Int, posY: Int, var rowHeight: Int, var limit: Int) : GuiComponent(posX, posY) {

    var scroll = 0
        set(value) {
            val old = field
            val new = value.clamp(0, maxScroll)
            if (old != new) {
                val e = ScrollChangeEvent(this, old, new).apply {
                    BUS.fire(this)
                }
                if (!e.isCanceled()) field = e.newValue
            }
        }

    val maxScroll get() = max(0, _children.asSequence().filter(GuiComponent::isVisible).count() - limit)

    private val _children = LinkedList<GuiComponent>()

    override fun drawComponent(mousePos: Vec2d, partialTicks: Float) {
        super.relationships.forEachChild(super.relationships::remove)
        scroll = scroll // This is to make sure it gets clamped to whatever currently visible children we have

        var y = 0
        _children.asSequence().filter(GuiComponent::isVisible).drop(scroll).take(limit).forEach {
            val h = rowHeight
            it.pos = vec(it.pos.x, y)
            super.add(it)
            y += h
        }
    }

    fun _add(vararg components: GuiComponent) {
        _children.addAll(components)
    }

    fun _clear() = _children.clear()

    init {
        BUS.hook(GuiComponentEvents.MouseWheelEvent::class.java) {
            if (it.direction == GuiComponentEvents.MouseWheelDirection.UP) --scroll
            else ++scroll
        }
    }

    data class ScrollChangeEvent(
        @JvmField val component: ComponentScrollList,
        val oldValue: Int, var newValue: Int
    ) : EventCancelable()
}

class ComponentEntity(
    val entity: EntityLivingBase,
    posX: Int,
    posY: Int,
    width: Int,
    height: Int,
    val scale: Int = 35
) : GuiComponent(posX, posY, width, height) {
    override fun drawComponent(mousePos: Vec2d, partialTicks: Float) {
        val x = width / 2
        val y = (height * (entity.eyeHeight / 1.8f)).toInt()
        val mx = x - mousePos.xf
        val my = (y - scale * (entity.eyeHeight)) - mousePos.yf
        GuiInventory.drawEntityOnScreen(x, y, scale, mx, my, entity)
    }
}
