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

import be.bluexin.rpg.entities.EntitySkillProjectile
import be.bluexin.rpg.util.runMainThread
import com.google.common.base.Predicate
import com.teamwizardry.librarianlib.features.kotlin.minus
import com.teamwizardry.librarianlib.features.kotlin.plus
import com.teamwizardry.librarianlib.features.utilities.RaycastUtils
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.selects.select
import net.minecraft.entity.EntityLivingBase
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import java.lang.StrictMath.pow
import java.util.*

interface Target {
    operator fun invoke(entity: EntityLivingBase, result: Channel<EntityLivingBase>)
    val range: Double
}

data class Projectile(override val range: Double = 15.0, val velocity: Float = 1f, val inaccuracy: Float = 1f) : Target {
    override operator fun invoke(entity: EntityLivingBase, result: Channel<EntityLivingBase>) {
        entity.server!!.runMainThread {
            entity.world.spawnEntity(EntitySkillProjectile(entity.world, entity, range, result).apply {
                realShoot(entity, entity.rotationPitch, entity.rotationYaw, 0.0f, velocity, inaccuracy)
            })
        }
    }
}

data class Caster(val unused: Boolean = false) : Target {
    override operator fun invoke(entity: EntityLivingBase, result: Channel<EntityLivingBase>) {
        launch {
            result.send(entity)
            result.close()
        }
    }

    override val range: Double
        get() = 0.0
}

data class Raycast(override val range: Double = 3.0) : Target {
    override operator fun invoke(entity: EntityLivingBase, result: Channel<EntityLivingBase>) {
        launch {
            val e = RaycastUtils.getEntityLookedAt(entity, range)
            if (e is EntityLivingBase) result.send(e)
            result.close()
        }
    }
}

data class Channelling(val delayMillis: Long, val procs: Int, val target: Target) : Target by target {
    override operator fun invoke(entity: EntityLivingBase, result: Channel<EntityLivingBase>) {
        launch {
            val p = produce(capacity = Channel.UNLIMITED) {
                repeat(procs) {
                    if (it != 0) delay(delayMillis)
                    val c = Channel<EntityLivingBase>(capacity = Channel.UNLIMITED)
                    target.invoke(entity, c)
                    send(c)
                }
            }

            val chs = LinkedList<Channel<EntityLivingBase>>()
            var flag = true
            while (flag || chs.isNotEmpty()) {
                select<Unit> {
                    if (flag) p.onReceiveOrNull {
                        if (it != null) chs += it
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
            }

            result.close()
        }
    }
}

data class AoE(override val range: Double = 3.0, val shape: Shape = Shape.CIRCLE) : Target {
    override operator fun invoke(entity: EntityLivingBase, result: Channel<EntityLivingBase>) {
        val dist = pow(range, 2.0)
        val entPos = entity.position
        val w = BlockPos(range, range, range)
        val minPos = entPos - w
        val maxPos = entPos + w
        val e = entity.world.getEntitiesWithinAABB(EntityLivingBase::class.java, AxisAlignedBB(minPos, maxPos),
                if (shape == Shape.SQUARE) null else Predicate<EntityLivingBase> {
                    entity.getDistanceSq(it!!) <= dist
                }
        )
        launch {
            e.forEach { result.send(it) }
            result.close()
        }
    }

    enum class Shape {
        SQUARE,
        CIRCLE
    }
}

data class Chain(override val range: Double = 3.0, val maxTargets: Int = 5, val delayMillis: Long = 500, val repeat: Boolean = false) : Target {
    override operator fun invoke(entity: EntityLivingBase, result: Channel<EntityLivingBase>) {
        val w = BlockPos(range, range, range)
        val dist = pow(range, 2.0)
        launch {
            val targets = LinkedHashSet<EntityLivingBase>()
            var previousTarget = entity
            repeat(maxTargets) { c ->
                if (c != 0) delay(delayMillis)
                val entPos = previousTarget.position
                val minPos = entPos - w
                val maxPos = entPos + w
                val es = entity.world.getEntitiesWithinAABB(EntityLivingBase::class.java, AxisAlignedBB(minPos, maxPos)) {
                    it != previousTarget && previousTarget.getDistanceSq(it!!) <= dist && (repeat || it !in targets)
                }
                val e = es.minBy { previousTarget.getDistanceSq(it) }
                if (e != null) {
                    if (!repeat) targets += e
                    result.send(e)
                    previousTarget = e
                } else {
                    result.close()
                    return@launch
                }
            }
            result.close()
        }
    }
}
