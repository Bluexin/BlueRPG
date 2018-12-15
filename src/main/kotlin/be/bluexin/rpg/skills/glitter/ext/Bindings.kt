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

package be.bluexin.rpg.skills.glitter.ext

import com.teamwizardry.librarianlib.features.particlesystem.ReadParticleBinding

/**
 * Binding expressed as scaled value of another binding.
 */
class ScaledBinding(
    /**
     * Binding to read from.
     */
    val origin: ReadParticleBinding,
    /**
     * Scale to multiply the original value with.
     */
    val scale: Double
) : ReadParticleBinding {
    override var contents = DoubleArray(origin.contents.size)

    override fun load(particle: DoubleArray) {
        origin.load(particle)
        repeat(contents.size) {
            contents[it] = origin.contents[it] * scale
        }
    }

    init {
        origin.require(contents.size)
    }
}