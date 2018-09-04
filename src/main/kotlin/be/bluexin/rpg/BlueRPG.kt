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

package be.bluexin.rpg

import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.SidedProxy
import net.minecraftforge.fml.common.event.FMLInitializationEvent
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
    const val DEPS = "required-after:saomclib@[1.2.1,);required-after:librarianlib@[4.14,);required-after:forge@[14.23.4.2718,);" // No dep on saoui cuz it's client-only

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
    fun preInit(event: FMLPreInitializationEvent) {
        MinecraftForge.EVENT_BUS.register(CommonEventHandler)
        proxy.preInit(event)
    }

    @Mod.EventHandler
    fun init(event: FMLInitializationEvent) {
        proxy.init(event)
    }
}