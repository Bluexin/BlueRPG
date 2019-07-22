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

package be.bluexin.rpg.devutil

import com.teamwizardry.librarianlib.features.kotlin.createKey
import net.minecraft.entity.Entity
import net.minecraft.network.PacketBuffer
import net.minecraft.network.datasync.DataParameter
import net.minecraft.network.datasync.DataSerializer
import net.minecraft.network.datasync.DataSerializers
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
inline fun <reified T : Enum<T>> KClass<out Entity>.createEnumKey(): DataParameter<T> =
    createKey(enumSerializers[T::class.java] as DataSerializer<T>)

open class EnumDataSerializer<T : Enum<T>>(private val clazz: Class<T>) : DataSerializer<T> {
    override fun createKey(id: Int) = DataParameter(id, this)

    override fun copyValue(value: T) = value

    override fun write(buf: PacketBuffer, value: T) {
        buf.writeEnumValue(value)
    }

    override fun read(buf: PacketBuffer): T = buf.readEnumValue(clazz)
}

val enumSerializers = mutableMapOf<Class<*>, EnumDataSerializer<*>>()

inline fun <reified T : Enum<T>> registerDataSerializer() {
    enumSerializers[T::class.java] = EnumDataSerializer(T::class.java).also(DataSerializers::registerSerializer)
}
