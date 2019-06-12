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

@file:Suppress("UNCHECKED_CAST")

package be.bluexin.rpg.skills

import be.bluexin.rpg.BlueRPG
import be.bluexin.rpg.PacketGlitter
import be.bluexin.rpg.PacketLightning
import be.bluexin.rpg.entities.EntitySkillProjectile
import be.bluexin.rpg.util.getEntityLookedAt
import be.bluexin.rpg.util.offerOrSendAndClose
import be.bluexin.rpg.util.runMainThread
import com.google.common.base.Predicate
import com.teamwizardry.librarianlib.features.helpers.aabb
import com.teamwizardry.librarianlib.features.kotlin.minus
import com.teamwizardry.librarianlib.features.kotlin.plus
import com.teamwizardry.librarianlib.features.kotlin.times
import com.teamwizardry.librarianlib.features.network.PacketHandler
import com.teamwizardry.librarianlib.features.network.sendToAllAround
import com.teamwizardry.librarianlib.features.saving.NamedDynamic
import com.teamwizardry.librarianlib.features.saving.Savable
import com.teamwizardry.librarianlib.features.utilities.RaycastUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import net.minecraft.entity.EntityLivingBase
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.Vec3d
import java.lang.StrictMath.pow
import java.util.*

@Savable
@NamedDynamic("t:t")
interface Targeting {
    operator fun invoke(context: SkillContext, from: Target, result: SendChannel<Target>)
    val range: Double
}

@Savable
@NamedDynamic("t:p")
data class Projectile(
    override val range: Double = 15.0,
    val velocity: Float = 1f,
    val inaccuracy: Float = 1f,
    val condition: Condition? = null,
    val precise: Boolean = false,
    val color1: Int = 0,
    val color2: Int = 0,
    val trailSystem: ResourceLocation = ResourceLocation(BlueRPG.MODID, "none")
) : Targeting {
    override operator fun invoke(context: SkillContext, from: Target, result: SendChannel<Target>) {
        if (from is TargetWithWorld && from is TargetWithPosition) from.world.minecraftServer!!.runMainThread {
            from.world.spawnEntity(EntitySkillProjectile(
                from.world,
                context,
                from,
                range,
                result,
                condition,
                precise
            ).apply {
                color1 = this@Projectile.color1
                color2 = this@Projectile.color2
                trailSystemKey = this@Projectile.trailSystem
                if (from is TargetWithLookVec) realShoot(from, 0.0f, velocity, inaccuracy)
            })
        }
    }
}

@Savable
@NamedDynamic("t:s")
data class Self(
    val color1: Int = 0,
    val color2: Int = 0,
    val glitter: PacketGlitter.Type = PacketGlitter.Type.AOE
) : Targeting {
    override operator fun invoke(context: SkillContext, from: Target, result: SendChannel<Target>) {
        if (from is TargetWithPosition && from is TargetWithWorld) PacketHandler.NETWORK.sendToAllAround(
            PacketGlitter(glitter, from.feet, color1, color2, .4),
            from.world,
            from.pos,
            64.0
        )
        result.offerOrSendAndClose(from)
    }

    override val range: Double
        get() = 0.0
}

@Savable
@NamedDynamic("t:r")
data class Raycast(
    override val range: Double = 3.0
) : Targeting {
    override operator fun invoke(context: SkillContext, from: Target, result: SendChannel<Target>) {
        if (from is TargetWithPosition && from is TargetWithLookVec && from is TargetWithWorld) GlobalScope.launch {
            val r = RaycastUtils.raycast(from.world, from.pos, from.lookVec, range)
            val e = getEntityLookedAt(from, from.world, r, range)
            val t: TargetWithPosition = if (e is EntityLivingBase) e.holder
            else PosHolder(r?.hitVec ?: (from.pos + from.lookVec * range))
            result.offerOrSendAndClose(t)
            PacketHandler.NETWORK.sendToAllAround(
                PacketLightning(from.pos, t.pos),
                from.world,
                from.pos,
                64.0
            )
        }
    }
}

@Savable
@NamedDynamic("t:c")
data class Channelling(
    val delayMillis: Long, val procs: Int, val targeting: Targeting
) : Targeting by targeting {
    override operator fun invoke(context: SkillContext, from: Target, result: SendChannel<Target>) {
        GlobalScope.launch {
            val p = produce(capacity = Channel.UNLIMITED) {
                repeat(procs) {
                    if (it != 0) delay(delayMillis)
                    val c = Channel<Target>(capacity = Channel.UNLIMITED)
                    targeting(context, from, c)
                    send(c)
                }
            }

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
@NamedDynamic("t:a")
data class AoE(
    override val range: Double = 3.0, val shape: Shape = Shape.CIRCLE,
    val color1: Int = 0,
    val color2: Int = 0
) : Targeting {
    override operator fun invoke(context: SkillContext, from: Target, result: SendChannel<Target>) {
        if (from is TargetWithPosition && from is TargetWithWorld) {
            PacketHandler.NETWORK.sendToAllAround(
                PacketGlitter(PacketGlitter.Type.AOE, from.pos, color1, color2, range / 5),
                from.world,
                from.pos,
                64.0
            )
            val dist = pow(range, 2.0)
            val entPos = from.pos
            val w = Vec3d(range, range, range)
            val minPos = entPos - w
            val maxPos = entPos + w
            val e = from.world.getEntitiesWithinAABB(
                EntityLivingBase::class.java, aabb(minPos, maxPos),
                if (shape == Shape.SQUARE) null else Predicate<EntityLivingBase> {
                    from.getDistanceSq(LivingHolder(it!!)) <= dist
                }
            )
            GlobalScope.launch {
                e.forEach { result.send(LivingHolder(it!!)) }
                result.close()
            }
        }
    }

    enum class Shape {
        SQUARE,
        CIRCLE
    }
}

@Savable
@NamedDynamic("t:i")
data class Chain(
    override val range: Double = 3.0, val maxTargets: Int = 5, val delayMillis: Long = 500, val repeat: Boolean = false,
    val condition: Condition? = null, val includeFrom: Boolean = false
) : Targeting {
    override operator fun invoke(context: SkillContext, from: Target, result: SendChannel<Target>) {
        if (from is TargetWithPosition && from is TargetWithWorld) GlobalScope.launch {
            val w = Vec3d(range, range, range)
            val dist = pow(range, 2.0)
            val targets = LinkedHashSet<Target>()
            targets.add(from)
            if (includeFrom) result.send(from)
            var previousTarget: TargetWithPosition = from
            repeat(maxTargets) { c ->
                if (c != 0) delay(delayMillis)
                val entPos = previousTarget.pos
                val minPos = entPos - w
                val maxPos = entPos + w
                val es = try {
                    from.world.getEntitiesWithinAABB(EntityLivingBase::class.java, aabb(minPos, maxPos)) {
                        val h = LivingHolder(it!!)
                        @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
                        h != previousTarget && previousTarget.getDistanceSq(h) <= dist &&
                                (repeat || h !in targets) && (condition == null || (condition!!)(context, h))
                    }
                } catch (_: Exception) {
                    listOf<EntityLivingBase>()
                }
                val e = es.minBy { previousTarget.getDistanceSq(LivingHolder(it)) }
                if (e != null) {
                    val h = LivingHolder(e)
                    if (!repeat) targets += h
                    result.send(h)
                    PacketHandler.NETWORK.sendToAllAround(
                        PacketLightning(previousTarget.pos, h.pos),
                        from.world,
                        from.pos,
                        64.0
                    )
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
