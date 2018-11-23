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

@file:Suppress("ClassName")

package be.bluexin.rpg.util

import com.teamwizardry.librarianlib.features.saving.AbstractSaveHandler
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import kotlin.reflect.KProperty

val ItemStack.tagCompoundOrNew: NBTTagCompound
    get() {
        if (!this.hasTagCompound()) this.tagCompound = NBTTagCompound()
        return this.tagCompound!!
    }

class NBTValue<T>(val getter: NBTTagCompound.(String) -> T, val setter: NBTTagCompound.(String, T) -> Unit) {
    operator fun getValue(thisRef: NBTTagCompound, property: KProperty<*>) = thisRef.getter(property.name)

    operator fun setValue(thisRef: NBTTagCompound, property: KProperty<*>, value: T) =
        thisRef.setter(property.name, value)

    operator fun getValue(thisRef: ItemStack, property: KProperty<*>) =
        this.getValue(thisRef.tagCompoundOrNew, property)

    operator fun setValue(thisRef: ItemStack, property: KProperty<*>, value: T) =
        this.setValue(thisRef.tagCompoundOrNew, property, value)
}

val stringValue = NBTValue(getter = NBTTagCompound::getString, setter = NBTTagCompound::setString)

inline fun <reified T : Any> autoValue() = NBTValue({
    AbstractSaveHandler.readAutoNBTByClass(T::class.java, this.getCompoundTag(it), false) as T
}, { name, instance: T ->
    this.setTag(name, AbstractSaveHandler.writeAutoNBT(instance, false))
})

object intValue {
    operator fun getValue(thisRef: NBTTagCompound, property: KProperty<*>) = thisRef.getInteger(property.name)

    operator fun setValue(thisRef: NBTTagCompound, property: KProperty<*>, value: Int) =
        thisRef.setInteger(property.name, value)

    operator fun getValue(thisRef: ItemStack, property: KProperty<*>) =
        this.getValue(thisRef.tagCompoundOrNew, property)

    operator fun setValue(thisRef: ItemStack, property: KProperty<*>, value: Int) =
        this.setValue(thisRef.tagCompoundOrNew, property, value)
}

object floatValue {
    operator fun getValue(thisRef: NBTTagCompound, property: KProperty<*>) = thisRef.getFloat(property.name)

    operator fun setValue(thisRef: NBTTagCompound, property: KProperty<*>, value: Float) =
        thisRef.setFloat(property.name, value)

    operator fun getValue(thisRef: ItemStack, property: KProperty<*>) =
        this.getValue(thisRef.tagCompoundOrNew, property)

    operator fun setValue(thisRef: ItemStack, property: KProperty<*>, value: Float) =
        this.setValue(thisRef.tagCompoundOrNew, property, value)
}

object longValue {
    operator fun getValue(thisRef: NBTTagCompound, property: KProperty<*>) = thisRef.getLong(property.name)

    operator fun setValue(thisRef: NBTTagCompound, property: KProperty<*>, value: Long) =
        thisRef.setLong(property.name, value)

    operator fun getValue(thisRef: ItemStack, property: KProperty<*>) =
        this.getValue(thisRef.tagCompoundOrNew, property)

    operator fun setValue(thisRef: ItemStack, property: KProperty<*>, value: Long) =
        this.setValue(thisRef.tagCompoundOrNew, property, value)
}

object doubleValue {
    operator fun getValue(thisRef: NBTTagCompound, property: KProperty<*>) =
        thisRef.getDouble(property.name)

    operator fun setValue(thisRef: NBTTagCompound, property: KProperty<*>, value: Double) =
        thisRef.setDouble(property.name, value)

    operator fun getValue(thisRef: ItemStack, property: KProperty<*>) =
        this.getValue(thisRef.tagCompoundOrNew, property)

    operator fun setValue(thisRef: ItemStack, property: KProperty<*>, value: Double) =
        this.setValue(thisRef.tagCompoundOrNew, property, value)
}
