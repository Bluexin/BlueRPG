@file:Suppress("unused")

package be.bluexin.rpg.events

import be.bluexin.rpg.stats.Stat
import net.minecraft.entity.player.EntityPlayer
import net.minecraftforge.event.entity.player.PlayerEvent
import net.minecraftforge.fml.common.eventhandler.Cancelable
import net.minecraftforge.fml.common.eventhandler.Event

@Event.HasResult
@Cancelable
class StatChangeEvent(val player: EntityPlayer, val stat: Stat, val oldValue: Int, var newValue: Int): PlayerEvent(player)

@Event.HasResult
@Cancelable
class ExperienceChangeEvent(val player: EntityPlayer, val oldValue: Long, var newValue: Long): PlayerEvent(player)

class LevelUpEvent(val player: EntityPlayer, val oldValue: Int, val newValue: Int): PlayerEvent(player)