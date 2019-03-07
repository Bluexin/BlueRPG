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

package be.bluexin.rpg.util

import be.bluexin.rpg.gear.IRPGGear
import be.bluexin.rpg.gear.WeaponType
import be.bluexin.rpg.skills.TargetWithBoundingBox
import be.bluexin.rpg.skills.TargetWithLookVec
import be.bluexin.rpg.skills.TargetWithPosition
import com.google.common.collect.Multimap
import com.teamwizardry.librarianlib.features.helpers.aabb
import com.teamwizardry.librarianlib.features.helpers.vec
import com.teamwizardry.librarianlib.features.kotlin.createKey
import com.teamwizardry.librarianlib.features.kotlin.localize
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraft.network.PacketBuffer
import net.minecraft.network.datasync.DataParameter
import net.minecraft.util.IThreadListener
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.RayTraceResult
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.Event
import kotlin.properties.ReadOnlyProperty
import kotlin.random.Random
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

fun fire(event: Event) = !MinecraftForge.EVENT_BUS.post(event)

val RNG = Random(Random.nextLong())

/**
 * Generates a random number between [min] and [max] (both inclusive).
 */
data class Roll(val min: Int, val max: Int) {
    operator fun invoke() = RNG.nextInt(min, max + 1)

    fun roll() = this()
}

fun <T> Array<T>.random(): T = this[RNG.nextInt(this.size)]
fun <T> List<T>.random(): T = this[RNG.nextInt(this.size)]

operator fun <K, V> Multimap<K, V>.set(key: K, value: V) {
    this.put(key, value)
}

val EntityLivingBase.allowBlock: Boolean
    get() {
        val mainhand = heldItemMainhand.item as? IRPGGear
        val offhand = heldItemOffhand.item as? IRPGGear
        return mainhand?.type?.allowBlock == true || ((mainhand?.type as? WeaponType)?.twoHander != true && offhand?.type?.allowBlock == true)
    }

val EntityLivingBase.allowParry: Boolean
    get() {
        val mainhand = heldItemMainhand.item as? IRPGGear
        val offhand = heldItemOffhand.item as? IRPGGear
        return mainhand?.type?.allowParry == true || ((mainhand?.type as? WeaponType)?.twoHander != true && offhand?.type?.allowParry == true)
    }

interface Localizable {
    val name: String

    val localized: String
        get() = "rpg.$key.name".localize()

    val key: String
        get() = name.toLowerCase()
}

inline fun IThreadListener.runMainThread(crossinline block: () -> Unit) {
    if (this.isCallingFromMinecraftThread) block()
    else this.addScheduledTask { block() }
}

fun <T> SendChannel<T>.offerOrSend(it: T) {
    if (!offer(it)) GlobalScope.launch {
        send(it)
    }
}

fun <T> SendChannel<T>.offerOrSendAndClose(it: T) {
    if (offer(it)) close()
    else GlobalScope.launch {
        send(it)
        close()
    }
}

operator fun IInventory.get(i: Int): ItemStack = this.getStackInSlot(i)
operator fun IInventory.set(i: Int, it: ItemStack) = this.setInventorySlotContents(i, it)

class BoundPropertyDelegateReadOnly<in R, out T>(val prop: () -> T) : ReadOnlyProperty<R, T> {
    override operator fun getValue(thisRef: R, property: KProperty<*>) = prop()
}

val <T> (() -> T).delegate: ReadOnlyProperty<Any, T>
    get() = BoundPropertyDelegateReadOnly(this)

infix fun IntRange.offset(by: Int) = IntRange(this.start + by, this.endInclusive + by)
infix fun LongRange.offset(by: Long) = LongRange(this.start + by, this.endInclusive + by)
infix fun CharRange.offset(by: Int) = CharRange(this.start + by, this.endInclusive + by)

inline fun KClass<out Entity>.createResourceLocationKey(): DataParameter<ResourceLocation> =
    createKey(writer = { packetBuffer, resourceLocationIn ->
        packetBuffer.writeResourceLocation(
            resourceLocationIn
        )
    }, reader = PacketBuffer::readResourceLocation)

private const val fpi = Math.PI.toFloat()

fun randomNormal(pitchDistribution: Float): Vec3d {
    val yaw = RNG.nextFloat() * 2 * fpi
    val ppi = fpi * pitchDistribution
    val pitch = RNG.nextFloat() * ppi - ppi / 2
    val multiplier = MathHelper.cos(pitch)

    return vec(MathHelper.sin(yaw) * multiplier, MathHelper.cos(yaw) * multiplier, MathHelper.sin(pitch))
}

fun randomNormal(): Vec3d {
    val yaw = RNG.nextFloat() * 2 * fpi
    val pitch = RNG.nextFloat() * fpi - fpi / 2
    val multiplier = MathHelper.cos(pitch)

    return vec(MathHelper.sin(yaw) * multiplier, MathHelper.cos(yaw) * multiplier, MathHelper.sin(pitch))
}

fun getEntityLookedAt(
    from: TargetWithPosition,
    world: World,
    pos: RayTraceResult?,
    maxDistance: Double = 32.0
): Entity? {
    var foundEntity: Entity? = null
    var distance = maxDistance
    val positionVector = from.pos
    if (pos != null) distance = pos.hitVec.distanceTo(positionVector)

    val lookVector = if (from is TargetWithLookVec) from.lookVec else Vec3d.ZERO
    val reachVector =
        positionVector.add(lookVector.x * maxDistance, lookVector.y * maxDistance, lookVector.z * maxDistance)
    var lookedEntity: Entity? = null
    val entitiesInBoundingBox = world.getEntitiesWithinAABBExcludingEntity(
        from.it as? Entity, ((from as? TargetWithBoundingBox)?.boundingBox ?: aabb(
            Vec3d.ZERO, Vec3d.ZERO
        )).expand(lookVector.x * maxDistance, lookVector.y * maxDistance, lookVector.z * maxDistance).expand(
            1.0,
            1.0,
            1.0
        )
    )
    var minDistance = distance
    val var14 = entitiesInBoundingBox.iterator()

    while (true) {
        do {
            do {
                if (!var14.hasNext()) {
                    return foundEntity
                }
                val next = var14.next()
                if (next.canBeCollidedWith()) {
                    val collisionBorderSize = next.collisionBorderSize
                    val hitbox = next.entityBoundingBox.expand(
                        collisionBorderSize.toDouble(),
                        collisionBorderSize.toDouble(),
                        collisionBorderSize.toDouble()
                    )
                    val interceptPosition = hitbox.calculateIntercept(positionVector, reachVector)
                    if (hitbox.contains(positionVector)) {
                        if (0.0 < minDistance || minDistance == 0.0) {
                            lookedEntity = next
                            minDistance = 0.0
                        }
                    } else if (interceptPosition != null) {
                        val distanceToEntity = positionVector.distanceTo(interceptPosition.hitVec)
                        if (distanceToEntity < minDistance || minDistance == 0.0) {
                            lookedEntity = next
                            minDistance = distanceToEntity
                        }
                    }
                }
            } while (lookedEntity == null)
        } while (minDistance >= distance && pos != null)

        foundEntity = lookedEntity
    }
}
