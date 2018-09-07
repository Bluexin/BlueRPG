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

package be.bluexin.rpg.modplugins

import be.bluexin.rpg.stats.stats
import com.saomc.saoui.api.events.EventInitStatsProvider
import com.saomc.saoui.api.info.IPlayerStatsProvider
import net.minecraft.entity.player.EntityPlayer
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object SAOUIEventHandler {

    init {
        MinecraftForge.EVENT_BUS.register(this)
    }

    @SubscribeEvent
    fun initStats(event: EventInitStatsProvider) {
        event.implementation = object : IPlayerStatsProvider {

            private val defaultImpl = event.implementation

            override fun getStatsString(player: EntityPlayer) = defaultImpl.getStatsString(player)

            override fun getLevel(player: EntityPlayer) = player.stats.level.level_a

            override fun getExpPct(player: EntityPlayer) = player.stats.level.progression
        }
    }
}