package be.bluexin.rpg.util

import com.teamwizardry.librarianlib.features.kotlin.localize
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.Event

fun fire(event: Event) = !MinecraftForge.EVENT_BUS.post(event)

val RNG = XoRoRNG()

interface Localizable {
    val name: String

    val localized: String
        get() = "rpg.$key.name".localize()

    val key: String
        get() = name.toLowerCase()
}