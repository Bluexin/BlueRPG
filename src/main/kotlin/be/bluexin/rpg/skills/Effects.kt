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

import be.bluexin.rpg.DamageHandler
import be.bluexin.rpg.util.runMainThread
import com.teamwizardry.librarianlib.features.saving.NamedDynamic
import com.teamwizardry.librarianlib.features.saving.Savable
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.selects.select
import net.minecraft.entity.EntityLivingBase
import net.minecraft.potion.PotionEffect
import net.minecraft.util.EntityDamageSource
import java.util.*
import kotlin.math.abs

@Savable
@NamedDynamic("e:e")
interface Effect<TARGET : Target> {
    operator fun invoke(caster: EntityLivingBase, targets: ReceiveChannel<TARGET>)
}

@Savable
@NamedDynamic("e:d")
data class Damage<TARGET>(val value: Double) : Effect<TARGET>
        where TARGET : TargetWithHealth, TARGET : TargetWithWorld { // TODO: take caster stats into account
    override fun invoke(caster: EntityLivingBase, targets: ReceiveChannel<TARGET>) {
        launch {
            for (e in targets) e.world.minecraftServer?.runMainThread {
                if (value >= 0) e.attack(DamageHandler.RpgDamageSource(EntityDamageSource("skill.test", caster)), value.toFloat())
                else e.heal(abs(value.toFloat()))
            }
        }
    }
}

@Savable
@NamedDynamic("e:b") // TODO: (de)serialization of PotionEffects
data class Buff<TARGET>(val effect: PotionEffect) : Effect<TARGET>
        where TARGET : TargetWithEffects, TARGET : TargetWithWorld {
    override fun invoke(caster: EntityLivingBase, targets: ReceiveChannel<TARGET>) {
        launch {
            for (e in targets) e.world.minecraftServer?.runMainThread {
                e.addPotionEffect(effect)
            }
        }
    }
}

@Savable
@NamedDynamic("e:s")
data class Skill<TARGET: Target, RESULT: Target>(val targeting: Targeting<TARGET, RESULT>, val effect: Effect<RESULT>) : Effect<TARGET> {
    override fun invoke(caster: EntityLivingBase, targets: ReceiveChannel<TARGET>) {
        launch {
            val p = produce(capacity = Channel.UNLIMITED) {
                for (e in targets) {
                    val c = Channel<RESULT>(capacity = Channel.UNLIMITED)
                    targeting(caster, e, c)
                    send(c)
                }
            }

            val result = Channel<RESULT>(capacity = Channel.UNLIMITED)
            effect(caster, result)
            val chs = LinkedList<Channel<RESULT>>()
            val toAdd = LinkedList<Channel<RESULT>>()
            var flag = true
            while (flag || chs.isNotEmpty()) {
                select<Unit> {
                    if (flag) p.onReceiveOrNull {
                        if (it != null) toAdd += it
                        else flag = false
                    }
                    val iter = chs.iterator()
                    while (iter.hasNext()) {
                        iter.next().onReceiveOrNull {
                            if (it != null) result.send(it)
                            else iter.remove()
                        }
                    }
                }
                chs += toAdd
                toAdd.clear()
            }

            result.close()
        }
    }
}

@Savable
@NamedDynamic("e:m")
data class MultiEffect<TARGET: Target>(val effects: Array<Effect<TARGET>>) : Effect<TARGET> {

    override fun invoke(caster: EntityLivingBase, targets: ReceiveChannel<TARGET>) {
        launch {
            val channels = Array<Channel<TARGET>>(effects.size) {
                Channel(capacity = Channel.UNLIMITED)
            }
            for (e in targets) channels.forEach { it.send(e) }
            channels.forEach { it.close() }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MultiEffect<*>) return false

        if (!Arrays.equals(effects, other.effects)) return false

        return true
    }

    override fun hashCode(): Int {
        return Arrays.hashCode(effects)
    }
}