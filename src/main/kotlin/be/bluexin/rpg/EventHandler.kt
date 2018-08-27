package be.bluexin.rpg

import be.bluexin.rpg.stats.stats
import be.bluexin.saomclib.onServer
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

object CommonEventHandler {

    @SubscribeEvent
    fun playerTick(event: TickEvent.PlayerTickEvent) {
        event.player.world onServer {
            val stats = event.player.stats
            if (stats.dirty) stats.sync()
        }
    }
}

@SideOnly(Side.CLIENT)
object ClientEventHandler

@SideOnly(Side.SERVER)
object ServerEventHandler