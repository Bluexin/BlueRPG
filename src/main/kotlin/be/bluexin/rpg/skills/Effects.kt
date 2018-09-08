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

import be.bluexin.rpg.util.runMainThread
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.launch
import net.minecraft.entity.EntityLivingBase
import net.minecraft.potion.PotionEffect
import net.minecraft.util.DamageSource
import kotlin.math.abs

interface Effect {
    operator fun invoke(targets: Channel<EntityLivingBase>)
}

data class Damage(val value: Double) : Effect { // TODO: take caster stats into account
    override fun invoke(targets: Channel<EntityLivingBase>) {
        launch {
            for (e in targets) e.server?.runMainThread {
                if (value >= 0) e.attackEntityFrom(DamageSource("skill.test"), value.toFloat())
                else e.heal(abs(value.toFloat()))
            }
        }
    }
}

data class Status(val effect: PotionEffect) : Effect {
    override fun invoke(targets: Channel<EntityLivingBase>) {
        launch {
            for (e in targets) e.server?.runMainThread {
                e.addPotionEffect(effect)
            }
        }
    }
}