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

package be.bluexin.rpg.events

import be.bluexin.rpg.stats.Stat
import net.minecraft.entity.player.EntityPlayer
import net.minecraftforge.event.entity.player.PlayerEvent
import net.minecraftforge.fml.common.eventhandler.Cancelable
import net.minecraftforge.fml.common.eventhandler.Event

@Event.HasResult
@Cancelable
class StatChangeEvent(val player: EntityPlayer, val stat: Stat, val oldValue: Int, var newValue: Int) :
    PlayerEvent(player)

@Event.HasResult
@Cancelable
class ExperienceChangeEvent(val player: EntityPlayer, val oldValue: Long, var newValue: Long) : PlayerEvent(player)

class LevelUpEvent(val player: EntityPlayer, val oldValue: Int, val newValue: Int) : PlayerEvent(player)