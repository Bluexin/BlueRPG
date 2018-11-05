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

package be.bluexin.rpg.skills

import com.teamwizardry.librarianlib.features.saving.Savable
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.produce
import net.minecraft.entity.EntityLivingBase
import java.util.*

@Savable
class Processor {
    private var chain = LinkedList<Triple<Targeting<Target, Target>, Condition<Target>?, Effect<Target>>>()

    fun process(caster: EntityLivingBase) {
        chain.forEach { (t, c, e) ->
            val channel = Channel<Target>(capacity = Channel.UNLIMITED)
            t(caster, caster.holder, channel)
            e(caster, if (c == null) channel else produce { for (it in channel) if (c(caster, it)) send(it) })
        }
    }

    fun <FROM : Target, TARGET : Target> addElement(
        targeting: Targeting<FROM, TARGET>,
        condition: Condition<TARGET>?,
        effect: Effect<TARGET>
    ) {
        @Suppress("UNCHECKED_CAST")
        chain.add(
            Triple(
                targeting as Targeting<Target, Target>,
                condition as Condition<Target>?,
                effect as Effect<Target>
            )
        )
    }

    override fun toString(): String {
        return "Processor(chain=$chain)"
    }
}