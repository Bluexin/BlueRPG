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

package be.bluexin.rpg

import be.bluexin.rpg.devutil.BlueRPGDataFixer
import be.bluexin.rpg.modplugins.CNPCEventHandler
import be.bluexin.rpg.modplugins.SAOUIEventHandler
import be.bluexin.rpg.pets.AWIntegration
import be.bluexin.rpg.utilities.CastCommand
import be.bluexin.rpg.utilities.Command
import be.bluexin.rpg.utilities.ResetCommand
import be.bluexin.saomclib.SAOMCLib
import com.saomc.saoui.SAOCore
import moe.plushie.armourers_workshop.common.lib.LibModInfo
import net.minecraftforge.fml.common.Loader
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.SidedProxy
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import net.minecraftforge.fml.common.event.FMLServerStartingEvent
import noppes.npcs.CustomNpcs
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
    const val VERSION = "GRADLE:VERSION"
    const val DEPS = "required-after:saomclib@[${SAOMCLib.VERSION},);required-after:librarianlib@[4.16,)" +
            ";required-after:forge@[14.23.4.2718,);" // No dep on saoui cuz it's client-only

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
        proxy.preInit(event)

        if (Loader.isModLoaded(CustomNpcs.MODID)) CNPCEventHandler
        if (Loader.isModLoaded(SAOCore.MODID)) SAOUIEventHandler
        if (Loader.isModLoaded(LibModInfo.ID)) AWIntegration
    }

    @Mod.EventHandler
    fun init(event: FMLInitializationEvent) {
        proxy.init(event)
    }

    @Mod.EventHandler
    fun postInit(event: FMLPostInitializationEvent) {
        proxy.postInit(event)
    }

    @Mod.EventHandler
    fun serverStart(event: FMLServerStartingEvent) {
        event.registerServerCommand(Command)
        event.registerServerCommand(CastCommand)
        event.registerServerCommand(ResetCommand)
        if (event.server.isDedicatedServer) BlueRPGDataFixer.setup(event.server.dataFixer)
        // TODO: register fixer for client & server here (was I drunk?)
    }
}