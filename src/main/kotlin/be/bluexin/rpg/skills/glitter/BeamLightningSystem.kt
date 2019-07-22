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

package be.bluexin.rpg.skills.glitter

import be.bluexin.rpg.devutil.RNG
import com.teamwizardry.librarianlib.features.helpers.vec
import com.teamwizardry.librarianlib.features.kotlin.div
import com.teamwizardry.librarianlib.features.kotlin.minus
import com.teamwizardry.librarianlib.features.kotlin.plus
import com.teamwizardry.librarianlib.features.particlesystem.ParticleSystem
import com.teamwizardry.librarianlib.features.particlesystem.bindings.CallbackBinding
import com.teamwizardry.librarianlib.features.particlesystem.modules.GlLineBeamRenderModule
import com.teamwizardry.librarianlib.features.particlesystem.modules.VelocityUpdateModule
import net.minecraft.util.math.Vec3d
import java.awt.Color
import kotlin.random.Random

object BeamLightningSystem : ParticleSystem() {

    private val rng = Random(RNG.nextLong())

    override fun configure() {
        // bind values in the particle array
        val isEnd = bind(1)
        val position = bind(3)
        val previousPosition = bind(3)
        val velocity = bind(3)
        val color = bind(4)

        updateModules.add(VelocityUpdateModule(position, velocity, previousPosition))
        // the beam draw a GL_LINE from each point to the next until it encounters a particle where isEnd != 0.0,
        // at which point it will start over a new line
        renderModules.add(GlLineBeamRenderModule(
            isEnd = isEnd, blend = true,
            previousPosition = previousPosition, position = position,
            color = color, size = 2f,
            alpha = CallbackBinding(1) { particle, contents ->
                age.load(particle)
                lifetime.load(particle)
                contents[0] = 1.0 - (age.contents[0] / lifetime.contents[0])
            }
        ))
    }

    fun spawn(lifetime: Double, isEnd: Boolean, pos: Vec3d, velocity: Vec3d, color: Color) {
        this.addParticle(
            lifetime,
            if (isEnd) 1.0 else 0.0,
            pos.x, pos.y, pos.z,
            pos.x, pos.y, pos.z,
            velocity.x, velocity.y, velocity.z,
            color.red / 255.0, color.green / 255.0, color.blue / 255.0, color.alpha / 255.0
        )
    }

    fun lightItUp(origin: Vec3d, destination: Vec3d) {
        // details here aren't important. Just generate a list of points
        repeat(rng.nextInt(3, 5)) {
            val points: List<Vec3d> = generateLightning(origin, destination, 4)
            points.forEachIndexed { i, point ->
                BeamLightningSystem.spawn(
                    10.0, // make particle last 10 ticks
                    i == points.size - 1, // if this is the last point, set isEnd to true (1.0)
                    point, // the position along the lightning bolt
                    vec(
                        0,
                        i * 0.05 / points.size,
                        0
                    ), // give the bolts an upward velocity proportional to their distance
                    Color.WHITE // make the lightning white
                )
            }
        }
    }

    private fun generateLightning(
        start: Vec3d,
        end: Vec3d,
        iterations: Int,
        list: MutableList<Vec3d> = mutableListOf()
    ): List<Vec3d> {
        if (iterations == 0) return list

        var center = (start + end) / 2
        val distance = (start - end).length() / 8
        center += vec(
            rng.nextDouble(-distance, distance),
            rng.nextDouble(-distance, distance),
            rng.nextDouble(-distance, distance)
        )

        val isFirst = list.isEmpty()
        if (isFirst) list.add(start)
        generateLightning(start, center, iterations - 1, list)
        list.add(center)
        generateLightning(center, end, iterations - 1, list)
        if (isFirst) list.add(end)

        return list
    }
}
