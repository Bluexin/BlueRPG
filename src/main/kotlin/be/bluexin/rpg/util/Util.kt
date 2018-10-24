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
import com.teamwizardry.librarianlib.features.kotlin.localize
import kotlinx.coroutines.experimental.channels.SendChannel
import kotlinx.coroutines.experimental.launch
import net.minecraft.entity.EntityLivingBase
import net.minecraft.util.IThreadListener
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.Event
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

fun fire(event: Event) = !MinecraftForge.EVENT_BUS.post(event)

val RNG = XoRoRNG()

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
    if (!offer(it)) launch {
        send(it)
    }
}

fun <T> SendChannel<T>.offerOrSendAndClose(it: T) {
    if (offer(it)) close()
    else launch {
        send(it)
        close()
    }
}

class BoundPropertyDelegateReadOnly<in R, T>(val prop: () -> T) : ReadOnlyProperty<R, T> {
    override fun getValue(thisRef: R, property: KProperty<*>): T {
        return prop()
    }
}

val <T> (() -> T).delegate: ReadOnlyProperty<Any, T>
    get() = BoundPropertyDelegateReadOnly(this)
