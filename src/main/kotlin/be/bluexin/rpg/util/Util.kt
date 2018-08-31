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

import com.teamwizardry.librarianlib.features.kotlin.localize
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.Event

fun fire(event: Event) = !MinecraftForge.EVENT_BUS.post(event)

val RNG = XoRoRNG()

fun <T: Any> Array<T>.random(): T = this[RNG.nextInt(this.size)]

interface Localizable {
    val name: String

    val localized: String
        get() = "rpg.$key.name".localize()

    val key: String
        get() = name.toLowerCase()
}