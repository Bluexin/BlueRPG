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

import be.bluexin.rpg.DamageHandler
import be.bluexin.rpg.util.runMainThread
import com.teamwizardry.librarianlib.features.kotlin.plus
import com.teamwizardry.librarianlib.features.saving.NamedDynamic
import com.teamwizardry.librarianlib.features.saving.Savable
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.filter
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import net.minecraft.entity.EntityLivingBase
import net.minecraft.potion.PotionEffect
import net.minecraft.util.EntityDamageSource
import net.minecraft.util.math.Vec3d
import java.util.*
import kotlin.math.abs

// Can't use ReceiveChannel#map in these till jvm has union types

@Savable
@NamedDynamic("e:e")
interface Effect {
    operator fun invoke(caster: EntityLivingBase, targets: ReceiveChannel<Target>)
}

@Savable
@NamedDynamic("e:d")
data class Damage(val value: DoubleExpression<TargetWithHealth>) : Effect {
    override fun invoke(caster: EntityLivingBase, targets: ReceiveChannel<Target>) {
        GlobalScope.launch {
            for (e in targets.filter { it is TargetWithHealth && it is TargetWithWorld }) {
                e as TargetWithWorld
                e as TargetWithHealth
                e.world.minecraftServer?.runMainThread {
                    val value = this@Damage.value(Holder(caster.holder, e)).toFloat()
                    if (value >= 0) e.attack(
                        DamageHandler.RpgDamageSource(EntityDamageSource("skill.test", caster)),
                        value
                    )
                    else e.heal(abs(value))
                }
            }
        }
    }
}

@Savable
@NamedDynamic("e:b") // TODO: (de)serialization of PotionEffects
data class Buff(val effect: (caster: EntityLivingBase, target: TargetWithEffects) -> PotionEffect) : Effect {
    override fun invoke(caster: EntityLivingBase, targets: ReceiveChannel<Target>) {
        GlobalScope.launch {
            for (e in targets.filter { it is TargetWithEffects && it is TargetWithWorld }) {
                e as TargetWithWorld
                e as TargetWithEffects
                e.world.minecraftServer?.runMainThread {
                    e.addPotionEffect(effect(caster, e))
                }
            }
        }
    }
}

@Savable
@NamedDynamic("e:v")
data class Velocity(val additionalVelocity: (caster: EntityLivingBase, target: TargetWithMovement) -> Vec3d) : Effect {
    override fun invoke(caster: EntityLivingBase, targets: ReceiveChannel<Target>) {
        GlobalScope.launch {
            for (e in targets.filter { it is TargetWithMovement && it is TargetWithWorld }) {
                e as TargetWithWorld
                e as TargetWithMovement
                e.world.minecraftServer?.runMainThread {
                    e.movement += additionalVelocity(caster, e)
                }
            }
        }
    }
}

@Savable
@NamedDynamic("e:s")
data class Skill(
    val targeting: Targeting,
    val condition: Condition?,
    val effect: Effect
) : Effect {
    override fun invoke(caster: EntityLivingBase, targets: ReceiveChannel<Target>) {
        GlobalScope.launch {
            val p = produce(capacity = Channel.UNLIMITED) {
                for (e in targets) {
                    val c = Channel<Target>(capacity = Channel.UNLIMITED)
                    targeting(caster, e, c)
                    send(c)
                }
            }

            val result = Channel<Target>(capacity = Channel.UNLIMITED)
            effect(
                caster,
                if (condition == null) result else result.filter {
                    @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
                    condition!!(caster, it)
                })
            val chs = LinkedList<Channel<Target>>()
            val toAdd = LinkedList<Channel<Target>>()
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
data class MultiEffect(val effects: Array<Effect>) : Effect {

    override fun invoke(caster: EntityLivingBase, targets: ReceiveChannel<Target>) {
        GlobalScope.launch {
            val channels = Array<Channel<Target>>(effects.size) {
                Channel(capacity = Channel.UNLIMITED)
            }
            effects.forEachIndexed { i, effect -> effect(caster, channels[i]) }
            for (e in targets) channels.forEach { it.send(e) }
            channels.forEach { it.close() }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MultiEffect) return false

        if (!Arrays.equals(effects, other.effects)) return false

        return true
    }

    override fun hashCode(): Int {
        return Arrays.hashCode(effects)
    }
}