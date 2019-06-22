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
import be.bluexin.rpg.stats.Buff
import be.bluexin.rpg.util.runMainThread
import com.teamwizardry.librarianlib.features.kotlin.plus
import com.teamwizardry.librarianlib.features.saving.NamedDynamic
import com.teamwizardry.librarianlib.features.saving.Savable
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import net.minecraft.util.EntityDamageSource
import net.minecraft.util.math.Vec3d
import java.util.*
import kotlin.math.abs
import com.fantasticsource.dynamicstealth.server.threat.Threat as DSThreat

// Can't use ReceiveChannel#map in these till jvm has union types

@Savable
@NamedDynamic("e:e")
interface Effect {
    operator fun invoke(
        context: SkillContext,
        targets: ReceiveChannel<Pair<Target, Target>>
    )
}

@Savable
@NamedDynamic("e:d")
data class Damage(
    val clientInfo: OnHitInfo? = null,
    val value: (context: SkillContext, target: TargetWithHealth) -> Double
) : Effect {
    override fun invoke(
        context: SkillContext,
        targets: ReceiveChannel<Pair<Target, Target>>
    ) {
        GlobalScope.launch {
            for ((from, target) in targets) {
                if (target is TargetWithWorld && target is TargetWithHealth) {
                    target.world.minecraftServer?.runMainThread {
                        val value = this@Damage.value(context, target).toFloat()
                        if (value > 0) target.attack(
                            DamageHandler.RpgDamageSource(EntityDamageSource("skill.test", context.caster)),
                            value
                        ) else if (value < 0) target.heal(abs(value))
                    }
                }

                clientInfo(context, from, target)
            }
        }
    }
}

@Savable
@NamedDynamic("e:b")
data class ApplyBuff(
    val clientInfo: OnHitInfo? = null,
    val effect: (context: SkillContext, target: TargetWithEffects) -> Buff?
) : Effect {
    override fun invoke(
        context: SkillContext,
        targets: ReceiveChannel<Pair<Target, Target>>
    ) {
        GlobalScope.launch {
            for ((_, e) in targets) {
                if (e is TargetWithWorld && e is TargetWithEffects) e.world.minecraftServer?.runMainThread {
                    effect(context, e)?.toVanilla?.apply(e::addPotionEffect)
                }
            }
        }
    }
}

@Savable
@NamedDynamic("e:v")
data class Velocity(
    val clientInfo: OnHitInfo? = null,
    val additionalVelocity: (context: SkillContext, target: TargetWithMovement) -> Vec3d
) : Effect {
    override fun invoke(
        context: SkillContext,
        targets: ReceiveChannel<Pair<Target, Target>>
    ) {
        GlobalScope.launch {
            for ((_, e) in targets) {
                if (e is TargetWithWorld && e is TargetWithMovement) e.world.minecraftServer?.runMainThread {
                    e.movement += additionalVelocity(context, e)
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
    val effect: Effect,
    val clientInfo: OnHitInfo? = null
) : Effect {
    override fun invoke(
        context: SkillContext,
        targets: ReceiveChannel<Pair<Target, Target>>
    ) {
        GlobalScope.launch {
            for ((_, e) in targets) {
                launch {
                    val c = Channel<Pair<Target, Target>>(capacity = Channel.UNLIMITED)
                    targeting(context, e, c)
                    effect(context, c)
                }
            }
        }
    }
}

@Savable
@NamedDynamic("e:m")
data class MultiEffect(val effects: Array<Effect>) : Effect {
    override fun invoke(
        context: SkillContext,
        targets: ReceiveChannel<Pair<Target, Target>>
    ) {
        GlobalScope.launch {
            val channels = Array<Channel<Pair<Target, Target>>>(effects.size) {
                Channel(capacity = Channel.UNLIMITED)
            }
            effects.forEachIndexed { i, effect -> effect(context, channels[i]) }
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

data class Threat(val amount: (context: SkillContext, target: Target) -> Double) : Effect {
    override fun invoke(context: SkillContext, targets: ReceiveChannel<Pair<Target, Target>>) {
        GlobalScope.launch {
            for ((from, target) in targets) {
                if (from is LivingHolder<*> && target is LivingHolder<*>) context.caster.world.minecraftServer?.runMainThread {
                    DSThreat.apply(target.it, from.it, amount(context, target), DSThreat.THREAT_TYPE.GEN_ATTACKED)
                }
            }
        }
    }
}

object Taunt : Effect {
    override fun invoke(context: SkillContext, targets: ReceiveChannel<Pair<Target, Target>>) {
        GlobalScope.launch {
            for ((from, target) in targets) {
                if (from is LivingHolder<*> && target is LivingHolder<*>) context.caster.world.minecraftServer?.runMainThread {
                    DSThreat.apply(target.it, from.it, Double.MAX_VALUE, DSThreat.THREAT_TYPE.GEN_ATTACKED)
                    DSThreat.apply(target.it, from.it, Double.MAX_VALUE, DSThreat.THREAT_TYPE.GEN_ATTACKED)
                }
            }
        }
    }
}

data class Free(
    val clientInfo: OnHitInfo? = null,
    val block: (context: SkillContext, from: Target, target: Target) -> Unit
) : Effect {
    override fun invoke(context: SkillContext, targets: ReceiveChannel<Pair<Target, Target>>) {
        GlobalScope.launch {
            for ((from, target) in targets) {
                block(context, from, target)
                clientInfo(context, from, target)
            }
        }
    }
}