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

import be.bluexin.rpg.classes.playerClass
import be.bluexin.rpg.containers.RPGContainer
import be.bluexin.rpg.stats.*
import com.teamwizardry.librarianlib.features.gui.component.GuiComponentEvents
import com.teamwizardry.librarianlib.features.gui.components.ComponentRect
import com.teamwizardry.librarianlib.features.gui.components.ComponentSprite
import com.teamwizardry.librarianlib.features.gui.components.ComponentText
import com.teamwizardry.librarianlib.features.gui.components.ComponentVoid
import com.teamwizardry.librarianlib.features.guicontainer.ComponentSlot
import com.teamwizardry.librarianlib.features.guicontainer.GuiContainerBase
import com.teamwizardry.librarianlib.features.guicontainer.builtin.BaseLayouts
import com.teamwizardry.librarianlib.features.helpers.vec
import com.teamwizardry.librarianlib.features.kotlin.Minecraft
import com.teamwizardry.librarianlib.features.kotlin.height
import com.teamwizardry.librarianlib.features.kotlin.localize
import net.minecraft.client.gui.inventory.GuiContainerCreative
import net.minecraft.entity.ai.attributes.IAttributeInstance
import java.awt.Color

class GuiRpgInventory(private val ct: RPGContainer) : GuiContainerBase(ct, 258, 250) {
    init {
        val bg = ComponentVoid(0, 0)
        mainComponents.add(bg)
        val equipment = ComponentSprite(Textures.EQUIPMENT_BG, 0, 0)

        val scrollList = buildStatPanel()
        bg.add(
            equipment,
            ComponentSprite(Textures.INVENTORY_BG, 0, 135),
            PlayerLayout(ct.invPlayer).apply {
                main.pos = vec(13, 137)
                armor.pos = vec(13, 7)
                armor.isVisible = true
            }.root,
            scrollList,
            ComponentVoid(238, 6, 8, 108).apply {
                add(ComponentSprite(Textures.SCROLLBAR_FG, 0, 0).apply {
                    fun maxY() = this.parent!!.height.toDouble() - this.height
                    scrollList.hook<ComponentScrollList.ScrollChangeEvent> { (it, _, new) ->
                        this.pos = vec(this.pos.x, maxY() * (new / it.maxScroll.toDouble()))
                    }
                })
            },
            ComponentEntity(ct.player, 33, 14, 80, 99)
        )

        repeat(2) {
            equipment.add(ComponentSprite(Textures.GATHERING_BG, 33 + 18 * it, 116))
        }

        repeat(2) {
            equipment.add(ComponentSprite(Textures.CRAFTING_BG, 78 + 18 * it, 116))
        }

        val playerClassData = Minecraft().player.playerClass
        repeat(3) {
            val playerClass = playerClassData[it]
            equipment.add(ClassButton(149 + 18 * it, 116, playerClass?.key).apply {
                hook<GuiComponentEvents.MouseClickEvent> {
                    mc.displayGuiScreen(ClassesGui())
                }
            })
        }
    }

    private fun buildStatPanel() = ComponentScrollList(120, 10, 10, 10).apply {
        val textColor = Color.WHITE

        _add(ComponentVoid(0, 0).apply {
            val l = ct.player.stats.level
            add(ComponentText(0, 0).apply {
                text {
                    "rpg.display.plevel".localize(
                        l.level_a,
                        "rpg.display.exp.short".localize((10000 * l.progression).toInt() / 100f)
                    )
                }
                color(textColor)
                shadow(true)
            }, ComponentRect(0, -1, 100, 10).apply {
                color(Color(0, 0, 0, 0))
                render.tooltip {
                    listOf(
                        "rpg.display.exp.long".localize(l.exp_a, l.toNext)
                    )
                }
            })
        })
        _add(ComponentVoid(0, 0))

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
                }, ComponentRect(0, -1, 100, 10).apply {
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
            ComponentSlot(player.head, 0, 0).apply {
                background.add(ComponentSprite(Textures.HEAD_BG, -1, -1))
            },
            ComponentSlot(player.chest, 0, 18).apply {
                background.add(ComponentSprite(Textures.CHESTPLATE_BG, -1, -1))
            },
            ComponentSlot(player.legs, 0, 2 * 18).apply {
                background.add(ComponentSprite(Textures.LEGS_BG, -1, -1))
            },
            ComponentSlot(player.feet, 0, 3 * 18).apply {
                background.add(ComponentSprite(Textures.FEET_BG, -1, -1))
            },
            ComponentSlot(player.offhand, 0, 4 * 18).apply {
                background.add(ComponentSprite(Textures.OFFHAND_BG, -1, -1))
            },
            ComponentSlot(player.egg, 0, 5 * 18).apply {
                background.add(ComponentSprite(Textures.EGG_BG, -1, -1))
            }
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
            add(ComponentSlot(slot, index * 18, 0).apply {
                background.add(ComponentSprite(Textures.BAG_BG, -1, -1))
            })
        }
    }
    val main = ComponentVoid(0, 0).apply {
        add(mainLayout.root, hotbar, bags)
    }
    val root = ComponentVoid(0, 0).apply {
        add(armor, main)
    }
}
