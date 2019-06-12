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

package be.bluexin.rpg.skills

import be.bluexin.saomclib.onServer
import com.teamwizardry.librarianlib.features.saving.Savable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.filter
import net.minecraft.entity.EntityLivingBase

@Savable
data class Processor(
    val trigger: Trigger,
    val targeting: Targeting,
    val condition: Condition?,
    val effect: Effect
) {

    fun startUsing(context: SkillContext): Boolean =
        if (trigger.startUsing(context)) {
            context.caster.world onServer { this.cast(context) }
            true
        } else false

    fun stopUsing(context: SkillContext, timeChanneled: Int) =
        if (trigger.stopUsing(context, timeChanneled)) {
            context.caster.world onServer { this.cast(context) }
            true
        } else false

    private fun cast(context: SkillContext) {
        val channel = Channel<Target>(capacity = Channel.UNLIMITED)
        targeting(context, context.caster.holder, channel)
        effect(context, condition?.let { c -> channel.filter { c(context, it) } } ?: channel)
    }
}

data class SkillContext(
    val caster: EntityLivingBase,
    val level: Int
)