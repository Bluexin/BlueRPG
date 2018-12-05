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
import be.bluexin.rpg.stats.FixedStat
import be.bluexin.rpg.stats.PrimaryStat
import be.bluexin.rpg.stats.SecondaryStat
import be.bluexin.rpg.stats.Stat
import com.teamwizardry.librarianlib.features.gui.components.ComponentRect
import com.teamwizardry.librarianlib.features.gui.components.ComponentSprite
import com.teamwizardry.librarianlib.features.gui.components.ComponentText
import com.teamwizardry.librarianlib.features.gui.components.ComponentVoid
import com.teamwizardry.librarianlib.features.guicontainer.ComponentSlot
import com.teamwizardry.librarianlib.features.guicontainer.GuiContainerBase
import com.teamwizardry.librarianlib.features.guicontainer.builtin.BaseLayouts
import com.teamwizardry.librarianlib.features.helpers.vec
import com.teamwizardry.librarianlib.features.kotlin.height
import com.teamwizardry.librarianlib.features.kotlin.localize
import net.minecraft.client.gui.inventory.GuiContainerCreative
import net.minecraft.entity.ai.attributes.IAttributeInstance
import java.awt.Color

class GuiRpgInventory(private val ct: RPGContainer) : GuiContainerBase(ct, 258, 250) {
    init {
        val bg = ComponentSprite(Textures.INVENTORY_BG, 0, 0)
        mainComponents.add(bg)

        val scrollList = buildStatPanel()
        bg.add(
            PlayerLayout(ct.invPlayer).apply {
                main.pos = vec(13, 137)
                armor.pos = vec(13, 7)
                armor.isVisible = true
            }.root,
            scrollList,
            ComponentVoid(238, 6, 8, 108).apply {
                add(ComponentSprite(Textures.SCROLLBAR_FG, 0, 0).apply {
                    fun maxY() = this.parent!!.height.toDouble() - this.height
                    scrollList.BUS.hook(ComponentScrollList.ScrollChangeEvent::class.java) { (it, _, new) ->
                        this.pos = vec(this.pos.x, maxY() * (new / it.maxScroll.toDouble()))
                    }
                })
            },
            ComponentEntity(ct.player, 33, 14, 80, 99)
        )
    }

    private fun buildStatPanel() = ComponentScrollList(142, 10, 10, 10).apply {
        val textColor = Color.WHITE
        val f = { stat: Stat ->
            val att: IAttributeInstance? = ct.player.getEntityAttribute(stat.attribute)
            val base = att?.baseValue ?: .0

            _add(ComponentVoid(0, 0).apply {
                add(ComponentText(0, 0).apply {
                    text {
                        "rpg.display.stat".localize(stat.longName(), stat.localize(att?.attributeValue ?: .0))
                    }
                    color(textColor)
                    shadow(true)
                }, ComponentRect(0, -1, 80, 10).apply {
                    color(Color(0, 0, 0, 0))
                    render.tooltip {
                        listOf(
                            "${stat.localize(base)} +${stat.localize((att?.attributeValue ?: .0) - base)}",
                            stat.tooltip()
                        )
                    }
                })
            })
        }
        sequenceOf("").toSortedSet()
        arrayOf(
            FixedStat.HEALTH,
            FixedStat.PSYCHE,
            FixedStat.ARMOR
        ).forEach(f)
        _add(ComponentVoid(0, 0))
        PrimaryStat.values().forEach(f)
        _add(ComponentVoid(0, 0))
        SecondaryStat.values().forEach(f)
        /*_add(ComponentVoid(0, 0))
        WeaponAttribute.values().forEach(f)*/
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
