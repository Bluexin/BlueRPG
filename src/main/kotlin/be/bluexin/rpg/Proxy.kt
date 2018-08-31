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

@file:Suppress("unused")

package be.bluexin.rpg

import be.bluexin.rpg.gear.*
import be.bluexin.rpg.items.DebugExpItem
import be.bluexin.rpg.items.DebugStatsItem
import be.bluexin.rpg.stats.GearStats
import be.bluexin.rpg.stats.PlayerStats
import be.bluexin.rpg.stats.TokenStats
import be.bluexin.saomclib.capabilities.CapabilitiesHandler
import com.teamwizardry.librarianlib.features.base.ModCreativeTab
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.common.capabilities.CapabilityManager
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

open class CommonProxy {
    open fun preInit(event: FMLPreInitializationEvent) {
        classLoadItems()

        CapabilitiesHandler.registerEntityCapability(PlayerStats::class.java, PlayerStats.Storage) { it is EntityPlayer }
        // Not using SAOMCLib for this one because we don't want it autoregistered
        CapabilityManager.INSTANCE.register(GearStats::class.java, GearStats.Storage) { GearStats(ItemStack.EMPTY) }
        CapabilityManager.INSTANCE.register(TokenStats::class.java, TokenStats.Storage) { TokenStats(ItemStack.EMPTY) }

        MinecraftForge.EVENT_BUS.register(CommonEventHandler)

        NameGenerator.preInit(event)
    }

    private fun classLoadItems() {
        object: ModCreativeTab() {
            override val iconStack: ItemStack
                get() = ItemStack(ItemGearToken[TokenType.TOKEN])

            init {
                registerDefaultTab()
            }
        }
        ItemGearToken
        DebugStatsItem
        DebugExpItem
        ItemArmor
        ItemMeleeWeapon
        ItemRangedWeapon
        ItemOffHand
    }
}

@SideOnly(Side.CLIENT)
class ClientProxy: CommonProxy() {
    override fun preInit(event: FMLPreInitializationEvent) {
        super.preInit(event)

        MinecraftForge.EVENT_BUS.register(ClientEventHandler)
    }
}

@SideOnly(Side.SERVER)
class ServerProxy: CommonProxy() {
    override fun preInit(event: FMLPreInitializationEvent) {
        super.preInit(event)

        MinecraftForge.EVENT_BUS.register(ServerEventHandler)
    }
}