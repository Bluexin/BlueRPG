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

package be.bluexin.rpg.utilities

import be.bluexin.rpg.PacketSaveLoadEditorItem
import be.bluexin.rpg.PacketSetEditorStats
import be.bluexin.rpg.devutil.ComponentCondition
import be.bluexin.rpg.devutil.ComponentScrollList
import be.bluexin.rpg.devutil.Textures.BG
import be.bluexin.rpg.devutil.Textures.PROGRESS_FG
import be.bluexin.rpg.devutil.Textures.SLOT
import be.bluexin.rpg.devutil.hook
import be.bluexin.rpg.gear.*
import be.bluexin.rpg.pets.EggData
import be.bluexin.rpg.pets.EggItem
import be.bluexin.rpg.pets.PetMovementType
import be.bluexin.rpg.stats.*
import com.teamwizardry.librarianlib.features.gui.EnumMouseButton
import com.teamwizardry.librarianlib.features.gui.component.GuiComponent
import com.teamwizardry.librarianlib.features.gui.component.GuiComponentEvents
import com.teamwizardry.librarianlib.features.gui.components.*
import com.teamwizardry.librarianlib.features.guicontainer.ComponentSlot
import com.teamwizardry.librarianlib.features.guicontainer.GuiContainerBase
import com.teamwizardry.librarianlib.features.guicontainer.builtin.BaseLayouts
import com.teamwizardry.librarianlib.features.helpers.vec
import com.teamwizardry.librarianlib.features.kotlin.div
import com.teamwizardry.librarianlib.features.kotlin.height
import com.teamwizardry.librarianlib.features.kotlin.isNotEmpty
import com.teamwizardry.librarianlib.features.kotlin.localize
import com.teamwizardry.librarianlib.features.network.PacketHandler
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.I18n
import net.minecraft.item.ItemStack
import java.awt.Color
import kotlin.math.PI

// TODO: The code here can be improved a LOT
class EditorGui(private val ct: EditorContainer) : GuiContainerBase(ct, 176, 166) {

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
                hook<GuiComponentEvents.MouseClickEvent> {
                    PacketHandler.NETWORK.sendToServer(PacketSaveLoadEditorItem(pos, true))
                }
            })
            add(ComponentSprite(PROGRESS_FG, 0, 18, 16, 16).apply {
                geometry.transform.rotate = PI / 2.0
                geometry.transform.anchor = size / 2.0
                color(Color.BLUE)
                render.tooltip(listOf("rpg.display.load".localize()))
                hook<GuiComponentEvents.MouseClickEvent> {
                    PacketHandler.NETWORK.sendToServer(PacketSaveLoadEditorItem(pos, false))
                }
            })
        })

        bg.add(ComponentText(171, 8, horizontal = ComponentText.TextAlignH.RIGHT).apply {
            text("Save TE")
        })
        bg.add(ComponentRect(131, 7, 40, 10).apply {
            hook<GuiComponentEvents.MouseClickEvent> {
                PacketHandler.NETWORK.sendToServer(PacketSetEditorStats(pos, null))
            }
            render.tooltip(listOf("Temporary workaround to save the current inventory content"))
            color(Color(0, 0, 0, 0))
        })

        bg.add(editToken())
        bg.add(editGear())
        bg.add(editEgg())
        bg.add(editDyn())
    }

    private fun editToken(): GuiComponent {
        fun GuiComponent.fillToken() {
            val pos = ct.te.pos
            add(ComponentList(0, 0, 10).apply {
                add(ComponentVoid(0, 0).apply {
                    add(ComponentText(0, 0).apply {
                        text {
                            "rpg.display.rarity".localize(
                                tokenStats.rarity?.localized
                                    ?: "rpg.random.name".localize()
                            )
                        }
                    })
                    add(ComponentRect(0, -1, 120, 10).apply {
                        color(Color(0, 0, 0, 0))
                        render.tooltip(listOf("rpg.display.cyclerarity".localize()))
                        hook<GuiComponentEvents.MouseClickEvent> {
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
                        hook<GuiComponentEvents.MouseClickEvent> {
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
                            hook<ComponentTextField.TextSentEvent> {
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
                            hook<ComponentTextField.TextSentEvent> {
                                val stats = tokenStats
                                stats.levelReq = it.content.toInt()
                                PacketHandler.NETWORK.sendToServer(PacketSetEditorStats(pos, stats))
                            }
                        })
                    })
                })
            })
        }

        return ComponentCondition(30, 20) { iss.item is GearTokenItem }.apply {
            visibilityChanged = {
                if (it) fillToken()
                else clear()
            }
        }
    }

    private fun editGear(): GuiComponent {
        fun GuiComponent.fillGear() {
            val pos = ct.te.pos
            val scrollList = ComponentScrollList(0, 0, 10, 6).apply {
                _add(ComponentVoid(0, 0).apply {
                    add(ComponentText(0, 0).apply {
                        text { "rpg.display.name".localize(gearStats.name ?: "rpg.random.name".localize()) }
                    })
                    add(ComponentRect(0, -1, 129, 10).apply {
                        color(Color(0, 0, 0, 0))
                        render.tooltip(listOf("rpg.display.cyclename".localize()))
                        hook<GuiComponentEvents.MouseClickEvent> {
                            val stats = gearStats
                            stats.name = if (it.button == EnumMouseButton.RIGHT) null else NameGenerator(
                                iss,
                                Minecraft.getMinecraft().player
                            )
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
                        hook<ComponentTextField.TextSentEvent> {
                            val stats = gearStats
                            val t = it.content.trim()
                            stats.name = t.takeUnless { t.isEmpty() }
                            PacketHandler.NETWORK.sendToServer(PacketSetEditorStats(pos, stats))
                        }
                    })
                })

                _add(ComponentVoid(0, 0).apply {
                    add(ComponentText(0, 0).apply {
                        text {
                            "rpg.display.rarity".localize(
                                gearStats.rarity?.localized
                                    ?: "rpg.random.name".localize()
                            )
                        }
                    })
                    add(ComponentRect(0, -1, 129, 10).apply {
                        color(Color(0, 0, 0, 0))
                        render.tooltip(listOf("rpg.display.cyclerarity".localize()))
                        hook<GuiComponentEvents.MouseClickEvent> {
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
                        hook<GuiComponentEvents.MouseClickEvent> {
                            val stats = gearStats
                            stats.generator = TokenType.values()[
                                    (stats.generator.ordinal + 1) % TokenType.values().size
                            ]
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
                        hook<GuiComponentEvents.MouseClickEvent> {
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
                            hook<ComponentTextField.TextSentEvent> {
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
                            hook<ComponentTextField.TextSentEvent> {
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
                        hook<GuiComponentEvents.MouseClickEvent> {
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
                                    with(
                                        FormulaeConfiguration.invoke(
                                            stat,
                                            gearStats.ilvl,
                                            gearStats.rarity ?: Rarity.COMMON,
                                            (iss.item as IRPGGear).type,
                                            (iss.item as IRPGGear).gearSlot
                                        )
                                    ) {
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
                                hook<ComponentTextField.TextSentEvent> {
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
                    scrollList.hook<ComponentScrollList.ScrollChangeEvent> { (it, _, new) ->
                        this.pos = vec(this.pos.x, maxY() * (new / it.maxScroll.toDouble()))
                    }
                    /*hook<GuiComponentEvents.MouseDragEvent> {
                        val p = (this.pos.y + it.mousePos.y).clamp(0.0, maxY())
                        val v = p / maxY()
                        val step = 1.0 / scrollList.maxScroll
                        scrollList.scroll = ((v + step / 2.0) * scrollList.maxScroll).toInt()
                    }*/
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

    private fun editEgg(): GuiComponent {
        fun GuiComponent.fillEgg() {
            val pos = ct.te.pos
            val scrollList = ComponentScrollList(0, 0, 10, 6).apply {
                _add(ComponentVoid(0, 0).apply {
                    add(ComponentText(0, 0).apply {
                        text { "rpg.display.name".localize(eggStats.name) }
                    })
                    add(ComponentRect(0, -1, 129, 10).apply {
                        color(Color(0, 0, 0, 0))
                        render.tooltip(listOf("rpg.display.cyclepetname".localize()))
                        hook<GuiComponentEvents.MouseClickEvent> {
                            val stats = eggStats
                            stats.name = if (it.button == EnumMouseButton.RIGHT) "Unnamed" else return@hook
                            PacketHandler.NETWORK.sendToServer(PacketSetEditorStats(pos, stats))
                        }
                    })
                })

                _add(ComponentVoid(0, 0).apply {
                    add(ComponentRect(0, -1, 129, 9).apply {
                        color(Color.LIGHT_GRAY)
                    })
                    add(ComponentTextField(3, 0, 126, 8).apply {
                        text = eggStats.name
                        enabledColor(Color.BLACK)
                        cursorColor(Color.BLACK)
                        useShadow(false)
                        useVanillaFilter(false)
                        render.tooltip(listOf("rpg.display.namefield".localize(), "rpg.display.apply".localize()))
                        hook<ComponentTextField.TextSentEvent> {
                            val stats = eggStats
                            val t = it.content.trim()
                            stats.name = if (t.isEmpty()) "Unnamed" else t
                            PacketHandler.NETWORK.sendToServer(PacketSetEditorStats(pos, stats))
                        }
                    })
                })

                _add(ComponentVoid(0, 0).apply {
                    add(ComponentText(0, 0).apply {
                        text {
                            "rpg.display.movementtype".localize(
                                eggStats.movementType.localized
                            )
                        }
                    })
                    add(ComponentRect(0, -1, 129, 10).apply {
                        color(Color(0, 0, 0, 0))
                        render.tooltip(listOf("rpg.display.cyclemovementtype".localize()))
                        hook<GuiComponentEvents.MouseClickEvent> {
                            val stats = eggStats
                            stats.movementType =
                                    PetMovementType.values()[(stats.movementType.ordinal + 1) % PetMovementType.values().size]
                            PacketHandler.NETWORK.sendToServer(PacketSetEditorStats(pos, stats))
                        }
                    })
                })

                _add(ComponentVoid(0, 0).apply {
                    add(ComponentText(0, 0).apply {
                        text { "rpg.display.hatchtime".localize(eggStats.hatchTimeSeconds) }
                    })

                    add(ComponentVoid(90, 0).apply {
                        add(ComponentRect(-3, -1, 42, 9).apply {
                            color(Color.LIGHT_GRAY)
                        })
                        add(ComponentTextField(0, 0, 39, 8).apply {
                            text = eggStats.hatchTimeSeconds.toString()
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
                            hook<ComponentTextField.TextSentEvent> {
                                val stats = eggStats
                                stats.hatchTimeSeconds = it.content.toInt()
                                PacketHandler.NETWORK.sendToServer(PacketSetEditorStats(pos, stats))
                            }
                        })
                    })
                })

                _add(ComponentVoid(0, 0).apply {
                    add(ComponentText(0, 0).apply {
                        text { "rpg.display.secondslived".localize(eggStats.secondsLived) }
                    })

                    add(ComponentVoid(90, 0).apply {
                        add(ComponentRect(-3, -1, 42, 9).apply {
                            color(Color.LIGHT_GRAY)
                        })
                        add(ComponentTextField(0, 0, 39, 8).apply {
                            text = eggStats.secondsLived.toString()
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
                            hook<ComponentTextField.TextSentEvent> {
                                val stats = eggStats
                                stats.secondsLived = it.content.toInt()
                                PacketHandler.NETWORK.sendToServer(PacketSetEditorStats(pos, stats))
                            }
                        })
                    })
                })

                // TODO: color picker

                _add(ComponentVoid(0, 0).apply {
                    add(ComponentText(0, 0).apply {
                        text { "rpg.display.primarycolor".localize(eggStats.primaryColor) }
                    })
                    add(ComponentRect(0, -1, 129, 10).apply {
                        color(Color(0, 0, 0, 0))
                        render.tooltip(listOf("rpg.display.cyclecolor".localize()))
                        hook<GuiComponentEvents.MouseClickEvent> {
                            val stats = eggStats
                            stats.primaryColor = if (it.button == EnumMouseButton.RIGHT) 0 else return@hook
                            PacketHandler.NETWORK.sendToServer(PacketSetEditorStats(pos, stats))
                        }
                    })
                })

                _add(ComponentVoid(0, 0).apply {
                    add(ComponentRect(0, -1, 129, 9).apply {
                        color(Color.LIGHT_GRAY)
                    })
                    add(ComponentTextField(3, 0, 126, 8).apply {
                        text = eggStats.primaryColor.toString()
                        enabledColor(Color.BLACK)
                        cursorColor(Color.BLACK)
                        useShadow(false)
                        filter = f@{
                            if (it.isEmpty()) return@f "0"
                            (if (it.startsWith("0x")) it.substring(2).toIntOrNull(16) else it.toIntOrNull())
                                ?: return@f null
                            return@f it
                        }
                        render.tooltip(listOf("rpg.display.apply".localize(), "rpg.display.colortooltip".localize()))
                        hook<ComponentTextField.TextSentEvent> {
                            val stats = eggStats
                            stats.primaryColor =
                                    if (it.content.startsWith("0x"))
                                        it.content.substring(2).toInt(16)
                                    else it.content.toInt()
                            PacketHandler.NETWORK.sendToServer(PacketSetEditorStats(pos, stats))
                        }
                    })
                })

                _add(ComponentVoid(0, 0).apply {
                    add(ComponentText(0, 0).apply {
                        text { "rpg.display.secondarycolor".localize(eggStats.secondaryColor) }
                    })
                    add(ComponentRect(0, -1, 129, 10).apply {
                        color(Color(0, 0, 0, 0))
                        render.tooltip(listOf("rpg.display.cyclecolor".localize()))
                        hook<GuiComponentEvents.MouseClickEvent> {
                            val stats = eggStats
                            stats.secondaryColor = if (it.button == EnumMouseButton.RIGHT) 0 else return@hook
                            PacketHandler.NETWORK.sendToServer(PacketSetEditorStats(pos, stats))
                        }
                    })
                })

                _add(ComponentVoid(0, 0).apply {
                    add(ComponentRect(0, -1, 129, 9).apply {
                        color(Color.LIGHT_GRAY)
                    })
                    add(ComponentTextField(3, 0, 126, 8).apply {
                        text = eggStats.secondaryColor.toString()
                        enabledColor(Color.BLACK)
                        cursorColor(Color.BLACK)
                        useShadow(false)
                        filter = f@{
                            if (it.isEmpty()) return@f "0"
                            (if (it.startsWith("0x")) it.substring(2).toIntOrNull(16) else it.toIntOrNull())
                                ?: return@f null
                            return@f it
                        }
                        render.tooltip(listOf("rpg.display.apply".localize(), "rpg.display.colortooltip".localize()))
                        hook<ComponentTextField.TextSentEvent> {
                            val stats = eggStats
                            stats.secondaryColor =
                                    if (it.content.startsWith("0x"))
                                        it.content.substring(2).toInt(16)
                                    else it.content.toInt()
                            PacketHandler.NETWORK.sendToServer(PacketSetEditorStats(pos, stats))
                        }
                    })
                })

                // TODO: sounds
            }
            add(scrollList)

            add(ComponentRect(131, 0, 8, 60).apply {
                color(Color.GRAY)
                clipping.clipToBounds = true
                add(ComponentRect(1, 0, 6, 3).apply {
                    color(Color.BLUE)
                    clipping.clipToBounds = true
                    fun maxY() = this.parent!!.height.toDouble() - this.height
                    scrollList.hook<ComponentScrollList.ScrollChangeEvent> { (it, _, new) ->
                        this.pos = vec(this.pos.x, maxY() * (new / it.maxScroll.toDouble()))
                    }
                    /*hook<GuiComponentEvents.MouseDragEvent> {
                        val p = (this.pos.y + it.mousePos.y).clamp(0.0, maxY())
                        val v = p / maxY()
                        val step = 1.0 / scrollList.maxScroll
                        scrollList.scroll = ((v + step / 2.0) * scrollList.maxScroll).toInt()
                    }*/
                })
            })
        }

        return ComponentCondition(30, 20) { iss.item === EggItem }.apply {
            visibilityChanged = {
                if (it) fillEgg()
                else clear()
            }
        }
    }

    private fun editDyn(): GuiComponent {
        fun GuiComponent.fillDyn() {
            val pos = ct.te.pos
            val scrollList = ComponentScrollList(0, 0, 10, 6).apply {
                // TODO: color picker

                _add(ComponentVoid(0, 0).apply {
                    add(ComponentText(0, 0).apply {
                        text { "rpg.display.primarycolor".localize(dynStats.primaryColor) }
                    })
                    add(ComponentRect(0, -1, 129, 10).apply {
                        color(Color(0, 0, 0, 0))
                        render.tooltip(listOf("rpg.display.cyclecolor".localize()))
                        hook<GuiComponentEvents.MouseClickEvent> {
                            val stats = dynStats
                            stats.primaryColor = if (it.button == EnumMouseButton.RIGHT) 0 else return@hook
                            PacketHandler.NETWORK.sendToServer(PacketSetEditorStats(pos, stats))
                        }
                    })
                })

                _add(ComponentVoid(0, 0).apply {
                    add(ComponentRect(0, -1, 129, 9).apply {
                        color(Color.LIGHT_GRAY)
                    })
                    add(ComponentTextField(3, 0, 126, 8).apply {
                        text = dynStats.primaryColor.toString()
                        enabledColor(Color.BLACK)
                        cursorColor(Color.BLACK)
                        useShadow(false)
                        filter = f@{
                            if (it.isEmpty()) return@f "0"
                            (if (it.startsWith("0x")) it.substring(2).toIntOrNull(16) else it.toIntOrNull())
                                ?: return@f null
                            return@f it
                        }
                        render.tooltip(listOf("rpg.display.apply".localize(), "rpg.display.colortooltip".localize()))
                        hook<ComponentTextField.TextSentEvent> {
                            val stats = dynStats
                            stats.primaryColor =
                                if (it.content.startsWith("0x"))
                                    it.content.substring(2).toInt(16)
                                else it.content.toInt()
                            PacketHandler.NETWORK.sendToServer(PacketSetEditorStats(pos, stats))
                        }
                    })
                })

                _add(ComponentVoid(0, 0).apply {
                    add(ComponentText(0, 0).apply {
                        text { "rpg.display.secondarycolor".localize(dynStats.secondaryColor) }
                    })
                    add(ComponentRect(0, -1, 129, 10).apply {
                        color(Color(0, 0, 0, 0))
                        render.tooltip(listOf("rpg.display.cyclecolor".localize()))
                        hook<GuiComponentEvents.MouseClickEvent> {
                            val stats = dynStats
                            stats.secondaryColor = if (it.button == EnumMouseButton.RIGHT) 0 else return@hook
                            PacketHandler.NETWORK.sendToServer(PacketSetEditorStats(pos, stats))
                        }
                    })
                })

                _add(ComponentVoid(0, 0).apply {
                    add(ComponentRect(0, -1, 129, 9).apply {
                        color(Color.LIGHT_GRAY)
                    })
                    add(ComponentTextField(3, 0, 126, 8).apply {
                        text = dynStats.secondaryColor.toString()
                        enabledColor(Color.BLACK)
                        cursorColor(Color.BLACK)
                        useShadow(false)
                        filter = f@{
                            if (it.isEmpty()) return@f "0"
                            (if (it.startsWith("0x")) it.substring(2).toIntOrNull(16) else it.toIntOrNull())
                                ?: return@f null
                            return@f it
                        }
                        render.tooltip(listOf("rpg.display.apply".localize(), "rpg.display.colortooltip".localize()))
                        hook<ComponentTextField.TextSentEvent> {
                            val stats = dynStats
                            stats.secondaryColor =
                                if (it.content.startsWith("0x"))
                                    it.content.substring(2).toInt(16)
                                else it.content.toInt()
                            PacketHandler.NETWORK.sendToServer(PacketSetEditorStats(pos, stats))
                        }
                    })
                })

                // TODO: sounds
            }
            add(scrollList)

            add(ComponentRect(131, 0, 8, 60).apply {
                color(Color.GRAY)
                clipping.clipToBounds = true
                add(ComponentRect(1, 0, 6, 3).apply {
                    color(Color.BLUE)
                    clipping.clipToBounds = true
                    fun maxY() = this.parent!!.height.toDouble() - this.height
                    scrollList.hook<ComponentScrollList.ScrollChangeEvent> { (it, _, new) ->
                        this.pos = vec(this.pos.x, maxY() * (new / it.maxScroll.toDouble()))
                    }
                    /*hook<GuiComponentEvents.MouseDragEvent> {
                        val p = (this.pos.y + it.mousePos.y).clamp(0.0, maxY())
                        val v = p / maxY()
                        val step = 1.0 / scrollList.maxScroll
                        scrollList.scroll = ((v + step / 2.0) * scrollList.maxScroll).toInt()
                    }*/
                })
            })
        }

        return ComponentCondition(30, 20) { iss.item is DynItem }.apply {
            visibilityChanged = {
                if (it) fillDyn()
                else clear()
            }
        }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (!this.mc.gameSettings.keyBindInventory.isActiveAndMatches(keyCode)) super.keyTyped(typedChar, keyCode)
    }

    private val iss: ItemStack
        get() = ct.invBlock.slotArray.first().stack

    private val tokenStats: TokenStats
        get() = ct.te.tokenStats

    private val gearStats: GearStats
        get() = ct.te.gearStats

    private val eggStats: EggData
        get() = ct.te.eggStats

    private val dynStats: DynamicData
        get() = ct.te.dynStats
}
