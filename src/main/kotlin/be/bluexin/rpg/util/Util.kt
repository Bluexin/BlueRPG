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

package be.bluexin.rpg.util

import be.bluexin.rpg.gear.IRPGGear
import be.bluexin.rpg.gear.WeaponType
import com.google.common.collect.Multimap
import com.teamwizardry.librarianlib.features.kotlin.createKey
import com.teamwizardry.librarianlib.features.kotlin.localize
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.PacketBuffer
import net.minecraft.network.datasync.DataParameter
import net.minecraft.util.IThreadListener
import net.minecraft.util.ResourceLocation
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