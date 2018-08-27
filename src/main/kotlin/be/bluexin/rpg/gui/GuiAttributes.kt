package be.bluexin.rpg.gui

import be.bluexin.rpg.PacketRaiseStat
import be.bluexin.rpg.stats.Stats
import be.bluexin.rpg.stats.stats
import com.teamwizardry.librarianlib.features.gui.GuiBase
import com.teamwizardry.librarianlib.features.gui.component.GuiComponentEvents
import com.teamwizardry.librarianlib.features.gui.components.ComponentRect
import com.teamwizardry.librarianlib.features.gui.components.ComponentText
import com.teamwizardry.librarianlib.features.kotlin.localize
import com.teamwizardry.librarianlib.features.network.PacketHandler
import java.awt.Color

class GuiAttributes : GuiBase(WIDTH, HEIGHT) {

    companion object {
        private const val WIDTH = 150
        private const val HEIGHT = 120
    }

    val stats by lazy { mc.player.stats }

    init {
        val bg = ComponentRect(0, 0, WIDTH, HEIGHT)
        bg.color(Color(100, 100, 100, 200))
        mainComponents.add(bg)

        val header = ComponentText(WIDTH / 2, 10, horizontal = ComponentText.TextAlignH.CENTER)
        bg.add(header)
        header.text("rpg.display.attributes".localize())

        bg.add(ComponentText(10, 30).apply {
            text { "rpg.display.stat".localize("rpg.attributepoints.long".localize(), stats.attributePoints) }
        })

        for (stat in Stats.values()) {
            bg.add(ComponentText(10, 45 + stat.ordinal * 10).apply {
                text { "rpg.display.stat".localize(stat.longName(), stats.baseStats[stat]) }
            })
            bg.add(ComponentRect(9, 44 + stat.ordinal * 10, 80, 9).apply {
                color(Color(0, 0, 0, 0))
                render.tooltip(listOf("Some description for ${stat.longName()} which I cba writing"))
            })
            bg.add(ComponentText(100, 45 + stat.ordinal * 10).apply {
                text("+")
            })
            bg.add(ComponentRect(99, 44 + stat.ordinal * 10, 8, 8).apply {
                color(Color(0, 0, 0, 0))
                render.tooltip(listOf("rpg.display.increase.small".localize(stat.longName())))
                BUS.hook(GuiComponentEvents.MouseClickEvent::class.java) {
                    PacketHandler.NETWORK.sendToServer(PacketRaiseStat(stat, 1))
                }
            })
            bg.add(ComponentText(110, 45 + stat.ordinal * 10).apply {
                text("++")
                render.tooltip(listOf("rpg.display.increase.big".localize(stat.longName())))
                BUS.hook(GuiComponentEvents.MouseClickEvent::class.java) {
                    PacketHandler.NETWORK.sendToServer(PacketRaiseStat(stat, 5))
                }
            })
            bg.add(ComponentRect(109, 44 + stat.ordinal * 10, 14, 8).apply {
                color(Color(0, 0, 0, 0))
                render.tooltip(listOf("rpg.display.increase.big".localize(stat.longName())))
                BUS.hook(GuiComponentEvents.MouseClickEvent::class.java) {
                    PacketHandler.NETWORK.sendToServer(PacketRaiseStat(stat, 5))
                }
            })
        }
    }

    override fun doesGuiPauseGame() = false
}