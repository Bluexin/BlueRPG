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

import be.bluexin.rpg.PacketSaveLoadEditorItem
import be.bluexin.rpg.PacketSetEditorStats
import be.bluexin.rpg.containers.ContainerEditor
import be.bluexin.rpg.gear.*
import be.bluexin.rpg.gui.Textures.BG
import be.bluexin.rpg.gui.Textures.PROGRESS_FG
import be.bluexin.rpg.gui.Textures.SLOT
import be.bluexin.rpg.stats.*
import com.teamwizardry.librarianlib.features.gui.EnumMouseButton
import com.teamwizardry.librarianlib.features.gui.component.GuiComponent
import com.teamwizardry.librarianlib.features.gui.component.GuiComponentEvents
import com.teamwizardry.librarianlib.features.gui.components.*
import com.teamwizardry.librarianlib.features.guicontainer.ComponentSlot
import com.teamwizardry.librarianlib.features.guicontainer.GuiContainerBase
import com.teamwizardry.librarianlib.features.guicontainer.builtin.BaseLayouts
import com.teamwizardry.librarianlib.features.helpers.vec
import com.teamwizardry.librarianlib.features.kotlin.*
import com.teamwizardry.librarianlib.features.network.PacketHandler
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.I18n
import net.minecraft.item.ItemStack
import java.awt.Color
import kotlin.math.PI

class GuiEditor(private val ct: ContainerEditor) : GuiContainerBase(ct, 176, 166) {

    init {
        val bg = ComponentSprite(BG, 0, 0)
        mainComponents.add(bg)

        for (i in 0..4) bg.add(ComponentSprite(SLOT, -21, 10 + i * 18))
        bg.add(BaseLayouts.player(ct.invPlayer).apply {
            main.pos = vec(8, 84)
            armor.pos = vec(-20, 11)
            offhand.pos = vec(-20, 83)
            armor.isVisible = true
            offhand.isVisible = true
        }.root)

        bg.add(ComponentSprite(SLOT, 7, 7))
        bg.add(ComponentSlot(ct.invBlock.slotArray.first(), 8, 8))

        val pos = ct.te.pos
        val state = ct.te.world.getBlockState(pos)
        bg.add(ComponentText(88, 6, horizontal = ComponentText.TextAlignH.CENTER).apply {
            text(I18n.format(ItemStack(state.block, 1, state.block.damageDropped(state)).displayName))
        })

        bg.add(ComponentCondition(8, 27) { iss.isNotEmpty }.apply {
            add(ComponentSprite(PROGRESS_FG, 0, 0, 16, 16).apply {
                geometry.transform.rotate = -PI / 2.0
                geometry.transform.anchor = size / 2.0
                color(Color.GREEN)
                render.tooltip(listOf("rpg.display.save".localize()))
                BUS.hook(GuiComponentEvents.MouseClickEvent::class.java) {
                    PacketHandler.NETWORK.sendToServer(PacketSaveLoadEditorItem(pos, true))
                }
            })
            add(ComponentSprite(PROGRESS_FG, 0, 18, 16, 16).apply {
                geometry.transform.rotate = PI / 2.0
                geometry.transform.anchor = size / 2.0
                color(Color.BLUE)
                render.tooltip(listOf("rpg.display.load".localize()))
                BUS.hook(GuiComponentEvents.MouseClickEvent::class.java) {
                    PacketHandler.NETWORK.sendToServer(PacketSaveLoadEditorItem(pos, false))
                }
            })
        })

        bg.add(ComponentText(171, 8, horizontal = ComponentText.TextAlignH.RIGHT).apply {
            text("Save TE")
        })
        bg.add(ComponentRect(131, 7, 40, 10).apply {
            BUS.hook(GuiComponentEvents.MouseClickEvent::class.java) {
                PacketHandler.NETWORK.sendToServer(PacketSetEditorStats(pos, null))
            }
            render.tooltip(listOf("Temporary workaround to save the current inventory content"))
            color(Color(0, 0, 0, 0))
        })

        bg.add(editToken())
        bg.add(editGear())
    }

    private fun editToken(): GuiComponent {
        val pos = ct.te.pos
        fun GuiComponent.fillToken() {
            add(ComponentList(0, 0, 10).apply {
                add(ComponentVoid(0, 0).apply {
                    add(ComponentText(0, 0).apply {
                        text {
                            "rpg.display.rarity".localize(tokenStats.rarity?.localized
                                    ?: "rpg.random.name".localize())
                        }
                    })
                    add(ComponentRect(0, -1, 120, 10).apply {
                        color(Color(0, 0, 0, 0))
                        render.tooltip(listOf("rpg.display.cyclerarity".localize()))
                        BUS.hook(GuiComponentEvents.MouseClickEvent::class.java) {
                            val stats = tokenStats
                            val r = stats.rarity
                            var idx = r?.ordinal ?: -1
                            stats.rarity = if (++idx >= Rarity.values().size) null else Rarity.values()[idx]
                            PacketHandler.NETWORK.sendToServer(PacketSetEditorStats(pos, stats))
                        }
                    })
                })
                add(ComponentVoid(0, 0).apply {
                    add(ComponentText(0, 0).apply {
                        text { "rpg.display.binding".localize(tokenStats.binding.localized) }
                    })
                    add(ComponentRect(0, -1, 120, 10).apply {
                        color(Color(0, 0, 0, 0))
                        render.tooltip(listOf("rpg.display.cyclebind".localize()))
                        BUS.hook(GuiComponentEvents.MouseClickEvent::class.java) {
                            val stats = tokenStats
                            stats.binding = Binding.values()[stats.binding.ordinal % Binding.values().size]
                            PacketHandler.NETWORK.sendToServer(PacketSetEditorStats(pos, stats))
                        }
                    })
                })

                add(ComponentVoid(0, 0).apply {
                    add(ComponentText(0, 0).apply {
                        text { "rpg.display.itemlevel".localize(tokenStats.ilvl) }
                    })

                    add(ComponentVoid(90, 0).apply {
                        add(ComponentRect(-3, -1, 50, 9).apply {
                            color(Color.LIGHT_GRAY)
                        })
                        add(ComponentTextField(0, 0, 47, 8).apply {
                            text = tokenStats.ilvl.toString()
                            enabledColor(Color.BLACK)
                            cursorColor(Color.BLACK)
                            useShadow(false)
                            filter = f@{
                                if (it.isEmpty()) return@f "1"
                                val i = it.toIntOrNull() ?: return@f null
                                if (i < 1) return@f "1"
                                return@f it
                            }
                            render.tooltip(listOf("rpg.display.apply".localize()))
                            BUS.hook(ComponentTextField.TextSentEvent::class.java) {
                                val stats = tokenStats
                                stats.ilvl = it.content.toInt()
                                PacketHandler.NETWORK.sendToServer(PacketSetEditorStats(pos, stats))
                            }
                        })
                    })
                })

                add(ComponentVoid(0, 0).apply {
                    add(ComponentText(0, 0).apply {
                        text { "rpg.display.levelreq".localize(tokenStats.levelReq) }
                    })

                    add(ComponentVoid(105, 0).apply {
                        add(ComponentRect(-3, -1, 35, 9).apply {
                            color(Color.LIGHT_GRAY)
                        })
                        add(ComponentTextField(0, 0, 32, 8).apply {
                            text = tokenStats.levelReq.toString()
                            enabledColor(Color.BLACK)
                            cursorColor(Color.BLACK)
                            useShadow(false)
                            filter = f@{
                                if (it.isEmpty()) return@f "1"
                                val i = it.toIntOrNull() ?: return@f null
                                if (i < 1) return@f "1"
                                if (i > Level.LEVEL_CAP) return@f Level.LEVEL_CAP.toString()
                                return@f it
                            }
                            render.tooltip(listOf("rpg.display.apply".localize()))
                            BUS.hook(ComponentTextField.TextSentEvent::class.java) {
                                val stats = tokenStats
                                stats.levelReq = it.content.toInt()
                                PacketHandler.NETWORK.sendToServer(PacketSetEditorStats(pos, stats))
                            }
                        })
                    })
                })
            })
        }

        return ComponentCondition(30, 20) { iss.item is ItemGearToken }.apply {
            visibilityChanged = {
                if (it) fillToken()
                else clear()
            }
        }
    }

    private fun editGear(): GuiComponent {
        val pos = ct.te.pos
        fun GuiComponent.fillGear() {
            val scrollList = ComponentScrollList(0, 0, 10, 6).apply {
                _add(ComponentVoid(0, 0).apply {
                    add(ComponentText(0, 0).apply {
                        text { "rpg.display.name".localize(gearStats.name ?: "rpg.random.name".localize()) }
                    })
                    add(ComponentRect(0, -1, 129, 10).apply {
                        color(Color(0, 0, 0, 0))
                        render.tooltip(listOf("rpg.display.cyclename".localize()))
                        BUS.hook(GuiComponentEvents.MouseClickEvent::class.java) {
                            val stats = gearStats
                            stats.name = if (it.button == EnumMouseButton.RIGHT) null else NameGenerator(iss, Minecraft.getMinecraft().player)
                            PacketHandler.NETWORK.sendToServer(PacketSetEditorStats(pos, stats))
                        }
                    })
                })

                _add(ComponentVoid(0, 0).apply {
                    add(ComponentRect(0, -1, 129, 9).apply {
                        color(Color.LIGHT_GRAY)
                    })
                    add(ComponentTextField(3, 0, 126, 8).apply {
                        text = gearStats.name ?: ""
                        enabledColor(Color.BLACK)
                        cursorColor(Color.BLACK)
                        useShadow(false)
                        useVanillaFilter(false)
                        render.tooltip(listOf("rpg.display.namefield".localize(), "rpg.display.apply".localize()))
                        BUS.hook(ComponentTextField.TextSentEvent::class.java) {
                            val stats = gearStats
                            val t = it.content.trim()
                            stats.name = if (t.isEmpty()) null else t
                            PacketHandler.NETWORK.sendToServer(PacketSetEditorStats(pos, stats))
                        }
                    })
                })

                _add(ComponentVoid(0, 0).apply {
                    add(ComponentText(0, 0).apply {
                        text {
                            "rpg.display.rarity".localize(gearStats.rarity?.localized
                                    ?: "rpg.random.name".localize())
                        }
                    })
                    add(ComponentRect(0, -1, 129, 10).apply {
                        color(Color(0, 0, 0, 0))
                        render.tooltip(listOf("rpg.display.cyclerarity".localize()))
                        BUS.hook(GuiComponentEvents.MouseClickEvent::class.java) {
                            val stats = gearStats
                            val r = stats.rarity
                            var idx = r?.ordinal ?: -1
                            stats.rarity = if (++idx >= Rarity.values().size) null else Rarity.values()[idx]
                            PacketHandler.NETWORK.sendToServer(PacketSetEditorStats(pos, stats))
                        }
                    })
                })

                _add(ComponentCondition(0, 0) { gearStats.rarity == null }.apply {
                    add(ComponentText(0, 0).apply {
                        text { "rpg.display.generator".localize(gearStats.generator.localized) }
                    })
                    add(ComponentRect(0, -1, 129, 10).apply {
                        color(Color(0, 0, 0, 0))
                        render.tooltip(listOf("rpg.display.cyclegenerator".localize()))
                        BUS.hook(GuiComponentEvents.MouseClickEvent::class.java) {
                            val stats = gearStats
                            stats.generator = TokenType.values()[stats.generator.ordinal + 1 % TokenType.values().size]
                            PacketHandler.NETWORK.sendToServer(PacketSetEditorStats(pos, stats))
                        }
                    })
                })

                _add(ComponentVoid(0, 0).apply {
                    add(ComponentText(0, 0).apply {
                        text { "rpg.display.binding".localize(gearStats.binding.localized) }
                    })
                    add(ComponentRect(0, -1, 129, 10).apply {
                        color(Color(0, 0, 0, 0))
                        render.tooltip(listOf("rpg.display.cyclebind".localize()))
                        BUS.hook(GuiComponentEvents.MouseClickEvent::class.java) {
                            val stats = gearStats
                            stats.binding = Binding.values()[(stats.binding.ordinal + 1) % Binding.values().size]
                            PacketHandler.NETWORK.sendToServer(PacketSetEditorStats(pos, stats))
                        }
                    })
                })

                _add(ComponentVoid(0, 0).apply {
                    add(ComponentText(0, 0).apply {
                        text { "rpg.display.itemlevel".localize(gearStats.ilvl) }
                    })

                    add(ComponentVoid(90, 0).apply {
                        add(ComponentRect(-3, -1, 42, 9).apply {
                            color(Color.LIGHT_GRAY)
                        })
                        add(ComponentTextField(0, 0, 39, 8).apply {
                            text = gearStats.ilvl.toString()
                            enabledColor(Color.BLACK)
                            cursorColor(Color.BLACK)
                            useShadow(false)
                            filter = f@{
                                if (it.isEmpty()) return@f "1"
                                val i = it.toIntOrNull() ?: return@f null
                                if (i < 1) return@f "1"
                                return@f it
                            }
                            render.tooltip(listOf("rpg.display.apply".localize()))
                            BUS.hook(ComponentTextField.TextSentEvent::class.java) {
                                val stats = gearStats
                                stats.ilvl = it.content.toInt()
                                PacketHandler.NETWORK.sendToServer(PacketSetEditorStats(pos, stats))
                            }
                        })
                    })
                })

                _add(ComponentVoid(0, 0).apply {
                    add(ComponentText(0, 0).apply {
                        text { "rpg.display.levelreq".localize(gearStats.levelReq) }
                    })

                    add(ComponentVoid(105, 0).apply {
                        add(ComponentRect(-3, -1, 27, 9).apply {
                            color(Color.LIGHT_GRAY)
                        })
                        add(ComponentTextField(0, 0, 24, 8).apply {
                            text = gearStats.levelReq.toString()
                            enabledColor(Color.BLACK)
                            cursorColor(Color.BLACK)
                            useShadow(false)
                            filter = f@{
                                if (it.isEmpty()) return@f "1"
                                val i = it.toIntOrNull() ?: return@f null
                                if (i < 1) return@f "1"
                                if (i > Level.LEVEL_CAP) return@f Level.LEVEL_CAP.toString()
                                return@f it
                            }
                            render.tooltip(listOf("rpg.display.apply".localize()))
                            BUS.hook(ComponentTextField.TextSentEvent::class.java) {
                                val stats = gearStats
                                stats.levelReq = it.content.toInt()
                                PacketHandler.NETWORK.sendToServer(PacketSetEditorStats(pos, stats))
                            }
                        })
                    })
                })

                _add(ComponentVoid(0, 0).apply {
                    add(ComponentText(0, 0).apply {
                        text { "rpg.display.generated".localize(gearStats.generated) }
                    })
                    add(ComponentRect(0, -1, 129, 10).apply {
                        color(Color(0, 0, 0, 0))
                        render.tooltip(listOf("rpg.display.cyclegenerated".localize()))
                        BUS.hook(GuiComponentEvents.MouseClickEvent::class.java) {
                            val stats = gearStats
                            stats.generated = !stats.generated
                            PacketHandler.NETWORK.sendToServer(PacketSetEditorStats(pos, stats))
                        }
                    })
                })

                fun ComponentScrollList.addStat(stat: Stat) {
                    _add(ComponentCondition(0, 0) { gearStats.generated }.apply {
                        add(ComponentText(0, 0).apply {
                            text { "rpg.display.stat".localize(stat.shortName(), gearStats[stat]) }
                        })
                        add(ComponentRect(0, -1, 70, 10).apply {
                            color(Color(0, 0, 0, 0))
                            render.tooltip {
                                listOf(stat.longName(),
                                        with(FormulaeConfiguration.invoke(stat, gearStats.ilvl, gearStats.rarity
                                                ?: Rarity.COMMON, (iss.item as IRPGGear).type, (iss.item as IRPGGear).gearSlot)) {
                                            "rpg.display.defaultrange".localize(this.min, this.max)
                                        })
                            }
                        })
                        add(ComponentVoid(75, 0).apply {
                            add(ComponentRect(-3, -1, 57, 9).apply {
                                color(Color.LIGHT_GRAY)
                            })
                            add(ComponentTextField(0, 0, 54, 8).apply {
                                text = gearStats[stat].toString()
                                enabledColor(Color.BLACK)
                                cursorColor(Color.BLACK)
                                useShadow(false)
                                filter = f@{
                                    if (it.isEmpty()) return@f "0"
                                    val i = it.toIntOrNull() ?: return@f null
                                    if (i < 0) return@f "0"
                                    return@f it
                                }
                                render.tooltip(listOf("rpg.display.apply".localize()))
                                BUS.hook(ComponentTextField.TextSentEvent::class.java) {
                                    val stats = gearStats
                                    stats.stats[stat] = it.content.toInt()
                                    PacketHandler.NETWORK.sendToServer(PacketSetEditorStats(pos, stats))
                                }
                            })
                        })
                    })
                }

                // Bug in compiler prevents using references here
                FixedStat.values().forEach { addStat(it) }
                PrimaryStat.values().forEach { addStat(it) }
                SecondaryStat.values().forEach { addStat(it) }
            }
            add(scrollList)

            add(ComponentRect(131, 0, 8, 60).apply {
                color(Color.GRAY)
                clipping.clipToBounds = true
                add(ComponentRect(1, 0, 6, 3).apply {
                    color(Color.BLUE)
                    clipping.clipToBounds = true
                    fun maxY() = this.parent!!.height.toDouble() - this.height
                    scrollList.BUS.hook(ComponentScrollList.ScrollChangeEvent::class.java) { (it, _, new) ->
                        this.pos = vec(this.pos.x, maxY() * (new / it.maxScroll.toDouble()))
                    }
                    BUS.hook(GuiComponentEvents.MouseDragEvent::class.java) {
                        val p = (this.pos.y + it.mousePos.y).clamp(0.0, maxY())
                        val v = p / maxY()
                        val step = 1.0 / scrollList.maxScroll
                        scrollList.scroll = ((v + step / 2.0) * scrollList.maxScroll).toInt()
                    }
                })
            })
        }

        return ComponentCondition(30, 20) { iss.item is IRPGGear }.apply {
            visibilityChanged = {
                if (it) fillGear()
                else clear()
            }
        }
    }

    private val iss: ItemStack
        get() = ct.invBlock.slotArray.first().stack

    private val tokenStats: TokenStats
        get() = ct.te.tokenStats

    private val gearStats: GearStats
        get() = ct.te.gearStats
}
