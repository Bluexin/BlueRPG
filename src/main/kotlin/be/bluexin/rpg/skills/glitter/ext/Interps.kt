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

import com.teamwizardry.librarianlib.features.animator.Easing
import com.teamwizardry.librarianlib.features.math.interpolate.InterpFunction
import com.teamwizardry.librarianlib.features.particlesystem.ReadParticleBinding
import java.awt.Color
import kotlin.math.max
import kotlin.math.min

class LifetimeColorInterpBinding(
    private val lifetime: ReadParticleBinding,
    private val age: ReadParticleBinding,
    private val interp: (Color, Color) -> InterpFunction<Color>,
    val from: ReadParticleBinding,
    val to: ReadParticleBinding
) : ReadParticleBinding {

    init {
        lifetime.require(1)
        age.require(1)
        from.require(4)
        to.require(4)
    }

    override var contents = DoubleArray(4)

    override fun load(particle: DoubleArray) {
        lifetime.load(particle)
        age.load(particle)
        from.load(particle)
        to.load(particle)

        val fraction = (age.contents[0] / lifetime.contents[0]).toFloat()
        val fromColor = Color(
            from.contents[0].toFloat(),
            from.contents[1].toFloat(),
            from.contents[2].toFloat(),
            from.contents[3].toFloat()
        )
        val toColor = Color(
            to.contents[0].toFloat(),
            to.contents[1].toFloat(),
            to.contents[2].toFloat(),
            to.contents[3].toFloat()
        )

        val r = interp(fromColor, toColor).get(fraction).getRGBComponents(null)
        contents = DoubleArray(4) { r[it].toDouble() }
    }
}

/**
 * Linearly increases from 0 to 1 over the first [fadeInFraction] of an interval.
 * Holds 1 until the last [fadeOutFraction] of that interval, and then decreases to 0.
 */
class EasingInOut(val fadeInFraction: Float, val fadeOutFraction: Float) : Easing() {

    /**
     * Goes from 0 to 1 in [fadeIn] units of time, holds 1 for [normal] units, then goes to 0 in [fadeOut] units.
     *
     * [fadeIn], [normal], and [fadeOut] are proportions
     */
    constructor(fadeIn: Int, normal: Int, fadeOut: Int) : this(
        fadeIn.toFloat() / (fadeIn + normal + fadeOut),
        fadeOut.toFloat() / (fadeIn + normal + fadeOut)
    )

    override fun invoke(progress: Float): Float {
        val fadeInResult = progress / fadeInFraction
        val fadeOutResult = (1 - progress) / fadeOutFraction
        return max(0f, min(1f, min(fadeInResult, fadeOutResult)))
    }
}
