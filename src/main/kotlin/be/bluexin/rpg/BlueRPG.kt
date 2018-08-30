package be.bluexin.rpg

import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.SidedProxy
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

@Mod(
        modid = BlueRPG.MODID,
        name = BlueRPG.NAME,
        version = BlueRPG.VERSION,
        dependencies = BlueRPG.DEPS,
        acceptableSaveVersions = "*"
)
object BlueRPG {
    const val MODID = "bluerpg"
    const val NAME = "Blue's RPG"
    const val VERSION = "1.0"
    const val DEPS = "required-after:saomclib@[1.2.1,);required-after:librarianlib@[4.14,);required-after:forge@[14.23.4.2718,)"

    val LOGGER: Logger = LogManager.getLogger(MODID)

    @JvmStatic
    @Mod.InstanceFactory
    fun shenanigan() = this

    @Suppress("MemberVisibilityCanBePrivate")
    @SidedProxy(clientSide = "be.bluexin.rpg.ClientProxy", serverSide = "be.bluexin.rpg.ServerProxy")
    lateinit var proxy: CommonProxy

    init {
        //
    }

    @Mod.EventHandler
    fun preInit(evt: FMLPreInitializationEvent) {
        MinecraftForge.EVENT_BUS.register(CommonEventHandler)
        proxy.preInit()
    }
}