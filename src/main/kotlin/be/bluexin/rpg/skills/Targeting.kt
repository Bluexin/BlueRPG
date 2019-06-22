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

import be.bluexin.rpg.entities.EntitySkillProjectile
import be.bluexin.rpg.gear.WeaponAttribute
import be.bluexin.rpg.stats.get
import be.bluexin.rpg.util.getEntityLookedAt
import be.bluexin.rpg.util.offerOrSendAndClose
import be.bluexin.rpg.util.runMainThread
import com.google.common.base.Predicate
import com.teamwizardry.librarianlib.features.helpers.aabb
import com.teamwizardry.librarianlib.features.kotlin.minus
import com.teamwizardry.librarianlib.features.kotlin.plus
import com.teamwizardry.librarianlib.features.kotlin.times
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
import net.minecraft.util.math.Vec3d
import java.lang.StrictMath.pow
import java.util.*

@Savable
@NamedDynamic("t:t")
interface Targeting {
    operator fun invoke(context: SkillContext, from: Target, result: SendChannel<Pair<Target, Target>>)
    val range: (context: SkillContext) -> Double
}

@Savable
@NamedDynamic("t:p")
data class Projectile(
    override val range: (context: SkillContext) -> Double = { 15.0 },
    val args: (context: SkillContext) -> Args = { Args() },
    val projectileInfo: ProjectileInfo = ProjectileInfo(),
    val clientInfo: TargetingInfo<Projectile>? = null
) : Targeting {
    override operator fun invoke(context: SkillContext, from: Target, result: SendChannel<Pair<Target, Target>>) {
        if (from is TargetWithWorld && from is TargetWithPosition) from.world.minecraftServer!!.runMainThread {
            val r = Channel<Target>(Channel.UNLIMITED)
            val (velocity, inaccuracy, condition, precise, passtrough, width, height) = args(context)
            val p = EntitySkillProjectile(
                from.world,
                context,
                from,
                range(context),
                r,
                condition,
                precise,
                passtrough, width, height
            ).apply {
                color1 = projectileInfo.color1
                color2 = projectileInfo.color2
                trailSystemKey = projectileInfo.trailSystem
                if (from is TargetWithLookVec) realShoot(from, 0.0f, velocity, inaccuracy)
            }
            from.world.spawnEntity(p)
            GlobalScope.launch {
                val h = ProjectileHolder(p)
                for (e in r) result.send(h to e)
                result.close()
            }
            clientInfo(this, context, from)
        }
    }

    data class Args(
        val velocity: Float = 1f,
        val inaccuracy: Float = 1f,
        val condition: Condition? = null,
        val precise: Boolean = false,
        val passtrough: Boolean = false,
        val width: Float = .25f,
        val height: Float = .25f
    )

    constructor(
        range: Double = 15.0,
        args: (context: SkillContext) -> Args = { Args() },
        projectileInfo: ProjectileInfo = ProjectileInfo(),
        clientInfo: TargetingInfo<Projectile>? = null
    )
            : this({ range }, args, projectileInfo, clientInfo)

    constructor(
        range: Double = 15.0,
        args: Args = Args(),
        projectileInfo: ProjectileInfo = ProjectileInfo(),
        clientInfo: TargetingInfo<Projectile>? = null
    ) : this({ range }, { args }, projectileInfo, clientInfo)
}

@Savable
@NamedDynamic("t:s")
data class Self(
    val clientInfo: TargetingInfo<Self>? = null
) : Targeting {
    override operator fun invoke(context: SkillContext, from: Target, result: SendChannel<Pair<Target, Target>>) {
        clientInfo(this, context, from)
        result.offerOrSendAndClose(from to from)
    }

    override val range: (SkillContext) -> Double get() = { .0 }
}

@Savable
@NamedDynamic("t:r")
data class Raycast(
    override val range: (context: SkillContext) -> Double = { 3.0 },
    val clientInfo: TargetingInfo<Raycast>? = null
) : Targeting {
    override operator fun invoke(context: SkillContext, from: Target, result: SendChannel<Pair<Target, Target>>) {
        if (from is TargetWithPosition && from is TargetWithLookVec && from is TargetWithWorld) GlobalScope.launch {
            val range = range(context)
            val r = RaycastUtils.raycast(from.world, from.pos, from.lookVec, range)
            val e = getEntityLookedAt(from, from.world, r, range)
            val t: TargetWithPosition = if (e is EntityLivingBase) e.holder
            else PosHolder(r?.hitVec ?: (from.pos + from.lookVec * range))
            result.offerOrSendAndClose(from to t)
            clientInfo(this@Raycast, context, from)
        }
    }

    constructor(range: Double = 3.0, clientInfo: TargetingInfo<Raycast>? = null) : this({ range }, clientInfo)
}

fun Melee(clientInfo: TargetingInfo<Raycast>? = null) =
    Raycast(range = { it.caster[WeaponAttribute.RANGE] }, clientInfo = clientInfo)

@Savable
@NamedDynamic("t:c")
data class Channelling(
    val targeting: Targeting, val args: (context: SkillContext) -> Args
) : Targeting by targeting {
    override operator fun invoke(context: SkillContext, from: Target, result: SendChannel<Pair<Target, Target>>) {
        GlobalScope.launch {
            val (delayMillis, procs) = args(context)
            val p = produce(capacity = Channel.UNLIMITED) {
                repeat(procs) {
                    if (it != 0) delay(delayMillis)
                    val c = Channel<Pair<Target, Target>>(capacity = Channel.UNLIMITED)
                    targeting(context, from, c)
                    send(c)
                }
            }

            val chs = LinkedList<Channel<Pair<Target, Target>>>()
            val toAdd = LinkedList<Channel<Pair<Target, Target>>>()
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

    data class Args(
        val delayMillis: Long, val procs: Int
    )

    constructor(targeting: Targeting, args: Args) : this(targeting, { args })
}

@Savable
@NamedDynamic("t:a")
data class AoE(
    override val range: (context: SkillContext) -> Double = { 3.0 }, val shape: Shape = Shape.CIRCLE,
    val clientInfo: TargetingInfo<AoE>? = null
) : Targeting {
    override operator fun invoke(context: SkillContext, from: Target, result: SendChannel<Pair<Target, Target>>) {
        if (from is TargetWithPosition && from is TargetWithWorld) {
            clientInfo(this, context, from)
            val range = range(context)
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
                e.forEach { result.send(from to LivingHolder(it!!)) }
                result.close()
            }
        }
    }

    enum class Shape {
        SQUARE,
        CIRCLE
    }

    constructor(range: Double, shape: Shape = Shape.CIRCLE, clientInfo: TargetingInfo<AoE>? = null) : this(
        { range },
        shape,
        clientInfo
    )
}

@Savable
@NamedDynamic("t:i")
data class Chain(
    override val range: (context: SkillContext) -> Double = { 3.0 },
    val args: (context: SkillContext) -> Args = { Args() },
    val clientInfo: TargetingInfo<Chain>? = null
) : Targeting {
    override operator fun invoke(context: SkillContext, from: Target, result: SendChannel<Pair<Target, Target>>) {
        if (from is TargetWithPosition && from is TargetWithWorld) GlobalScope.launch {
            val range = range(context)
            val (maxTargets, delayMillis, repeat, condition, includeFrom) = args(context)
            val w = Vec3d(range, range, range)
            val dist = pow(range, 2.0)
            val targets = LinkedHashSet<Target>()
            targets.add(from)
            if (includeFrom) result.send(from to from)
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
                    result.send(previousTarget to h)
                    clientInfo(this@Chain, context, from)
                    previousTarget = h
                } else {
                    result.close()
                    return@launch
                }
            }
            result.close()
        }
    }

    data class Args(
        val maxTargets: Int = 5,
        val delayMillis: Long = 500,
        val repeat: Boolean = false,
        val condition: Condition? = null,
        val includeFrom: Boolean = false
    )

    constructor(
        range: Double = 3.0,
        args: (context: SkillContext) -> Args = { Args() },
        clientInfo: TargetingInfo<Chain>? = null
    )
            : this({ range }, args, clientInfo)

    constructor(range: Double = 3.0, args: Args = Args(), clientInfo: TargetingInfo<Chain>? = null)
            : this({ range }, { args }, clientInfo)
}
