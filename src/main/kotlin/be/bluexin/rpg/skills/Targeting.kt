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

@file:Suppress("UNCHECKED_CAST")

package be.bluexin.rpg.skills

import be.bluexin.rpg.entities.EntitySkillProjectile
import be.bluexin.rpg.util.offerOrSendAndClose
import be.bluexin.rpg.util.runMainThread
import com.google.common.base.Predicate
import com.teamwizardry.librarianlib.features.kotlin.minus
import com.teamwizardry.librarianlib.features.kotlin.plus
import com.teamwizardry.librarianlib.features.saving.NamedDynamic
import com.teamwizardry.librarianlib.features.saving.Savable
import com.teamwizardry.librarianlib.features.utilities.RaycastUtils
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.SendChannel
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.selects.select
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.Vec3d
import java.lang.StrictMath.pow
import java.util.*

// TODO Some of these need to be ported to the new system to handle targeting both blocks & entities

@Savable
@NamedDynamic("t:t")
interface Targeting<FROM : Target, RESULT : Target> {
    operator fun invoke(caster: EntityLivingBase, from: FROM, result: SendChannel<RESULT>)
    val range: Double
}

@Savable
@NamedDynamic("t:p")
data class Projectile<FROM, RESULT>(
        override val range: Double = 15.0, val velocity: Float = 1f, val inaccuracy: Float = 1f,
        val condition: Condition<RESULT>? = null
) : Targeting<FROM, RESULT> where FROM : TargetWithLookVec, FROM : TargetWithPosition, FROM : TargetWithWorld,
                                  RESULT : TargetWithPosition, RESULT : TargetWithWorld {
    override operator fun invoke(caster: EntityLivingBase, from: FROM, result: SendChannel<RESULT>) {
        from.world.minecraftServer!!.runMainThread {
            from.world.spawnEntity(EntitySkillProjectile(
                    from.world, if (caster is EntityPlayer) PlayerHolder(caster) else null, from, range, result, condition
            ).apply {
                realShoot(from, 0.0f, velocity, inaccuracy)
            })
        }
    }
}

@Savable
@NamedDynamic("t:s")
data class Self<T : Target>(val unused: Boolean = false) : Targeting<T, T> {
    override operator fun invoke(caster: EntityLivingBase, from: T, result: SendChannel<T>) {
        result.offerOrSendAndClose(from)
    }

    override val range: Double
        get() = 0.0

    fun <FROM : T, RESULT : Target> cast() = this as Targeting<FROM, RESULT>
}

@Savable
@NamedDynamic("t:r")
data class Raycast<FROM, RESULT>(
        override val range: Double = 3.0, val condition: Condition<RESULT>? = null
) : Targeting<FROM, RESULT> where FROM : TargetWithLookVec, FROM : TargetWithPosition, FROM : TargetWithWorld,
                                  RESULT : TargetWithPosition, RESULT : TargetWithWorld {
    override operator fun invoke(caster: EntityLivingBase, from: FROM, result: SendChannel<RESULT>) {
        launch {
            val e = RaycastUtils.getEntityLookedAt((from as LivingHolder<*>).it, range)
            if (e is EntityLivingBase) result.send(e.holder as RESULT)
            result.close()
        }
    }
}

@Savable
@NamedDynamic("t:c")
data class Channelling<FROM : Target, RESULT : Target>(
        val delayMillis: Long, val procs: Int, val targeting: Targeting<FROM, RESULT>
) : Targeting<FROM, RESULT> by targeting {
    override operator fun invoke(caster: EntityLivingBase, from: FROM, result: SendChannel<RESULT>) {
        launch {
            val p = produce(capacity = Channel.UNLIMITED) {
                repeat(procs) {
                    if (it != 0) delay(delayMillis)
                    val c = Channel<RESULT>(capacity = Channel.UNLIMITED)
                    targeting(caster, from, c)
                    send(c)
                }
            }

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
@NamedDynamic("t:a")
data class AoE<FROM, RESULT>(
        override val range: Double = 3.0, val shape: Shape = Shape.CIRCLE
) : Targeting<FROM, RESULT> where FROM : TargetWithLookVec, FROM : TargetWithPosition, FROM : TargetWithWorld,
                                  RESULT : TargetWithPosition, RESULT : TargetWithWorld {
    override operator fun invoke(caster: EntityLivingBase, from: FROM, result: SendChannel<RESULT>) {
        val dist = pow(range, 2.0)
        val entPos = from.pos
        val w = Vec3d(range, range, range)
        val minPos = entPos - w
        val maxPos = entPos + w
        val e = from.world.getEntitiesWithinAABB(EntityLivingBase::class.java, AxisAlignedBB(minPos, maxPos),
                if (shape == Shape.SQUARE) null else Predicate<EntityLivingBase> {
                    from.getDistanceSq(LivingHolder(it!!)) <= dist
                }
        )
        launch {
            e.forEach { result.send(LivingHolder(it!!) as RESULT) }
            result.close()
        }
    }

    enum class Shape {
        SQUARE,
        CIRCLE
    }
}

@Savable
@NamedDynamic("t:i")
data class Chain<T>(
        override val range: Double = 3.0, val maxTargets: Int = 5, val delayMillis: Long = 500, val repeat: Boolean = false,
        val condition: Condition<T>? = null
) : Targeting<T, T>
        where T : TargetWithPosition, T : TargetWithWorld {
    override operator fun invoke(caster: EntityLivingBase, from: T, result: SendChannel<T>) {
        val w = Vec3d(range, range, range)
        val dist = pow(range, 2.0)
        launch {
            val targets = LinkedHashSet<T>()
            var previousTarget = from
            repeat(maxTargets) { c ->
                if (c != 0) delay(delayMillis)
                val entPos = previousTarget.pos
                val minPos = entPos - w
                val maxPos = entPos + w
                val es = try {
                    from.world.getEntitiesWithinAABB(EntityLivingBase::class.java, AxisAlignedBB(minPos, maxPos)) {
                        val h = LivingHolder(it!!) as T
                        h != previousTarget && previousTarget.getDistanceSq(h) <= dist &&
                                (repeat || h !in targets) && (condition == null || condition!!(caster, h))
                    }
                } catch (_: Exception) {
                    listOf<EntityLivingBase>()
                }
                val e = es.minBy { previousTarget.getDistanceSq(LivingHolder(it!!)) }
                if (e != null) {
                    val h = LivingHolder(e) as T
                    if (!repeat) targets += h
                    result.send(h)
                    previousTarget = h
                } else {
                    result.close()
                    return@launch
                }
            }
            result.close()
        }
    }
}
