@file:Suppress("unused")

package be.bluexin.rpg

import be.bluexin.rpg.items.DebugExpItem
import be.bluexin.rpg.items.DebugStatsItem
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

open class CommonProxy {
    open fun preInit() {
        classLoadItems()

        MinecraftForge.EVENT_BUS.register(CommonEventHandler)
    }

    private fun classLoadItems() {
        DebugStatsItem
        DebugExpItem
    }
}

@SideOnly(Side.CLIENT)
class ClientProxy: CommonProxy() {
    override fun preInit() {
        super.preInit()

        MinecraftForge.EVENT_BUS.register(ClientEventHandler)
    }
}

@SideOnly(Side.SERVER)
class ServerProxy: CommonProxy() {
    override fun preInit() {
        super.preInit()

        MinecraftForge.EVENT_BUS.register(ServerEventHandler)
    }
}