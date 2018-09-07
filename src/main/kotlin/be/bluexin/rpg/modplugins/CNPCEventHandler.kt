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
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noppes.npcs.api.NpcAPI
import noppes.npcs.api.event.QuestEvent

object CNPCEventHandler {

    init {
        NpcAPI.Instance().events().register(this)
    }

    @SubscribeEvent
    fun questComplete(event: QuestEvent.QuestTurnedInEvent) {
        event.player.mcEntity.stats.level += event.expReward.toLong()
    }
}