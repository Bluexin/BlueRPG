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

package be.bluexin.rpg.skills.glitter

import be.bluexin.rpg.BlueRPG
import be.bluexin.rpg.skills.glitter.ext.LifetimeColorInterpBinding
import be.bluexin.rpg.util.RNG
import com.teamwizardry.librarianlib.features.kotlin.randomNormal
import com.teamwizardry.librarianlib.features.kotlin.times
import com.teamwizardry.librarianlib.features.math.interpolate.numeric.InterpFloatInOut
import com.teamwizardry.librarianlib.features.particle.functions.InterpColorHSV
import com.teamwizardry.librarianlib.features.particlesystem.BlendMode
import com.teamwizardry.librarianlib.features.particlesystem.ParticleSystem
import com.teamwizardry.librarianlib.features.particlesystem.bindings.InterpBinding
import com.teamwizardry.librarianlib.features.particlesystem.modules.BasicPhysicsUpdateModule
import com.teamwizardry.librarianlib.features.particlesystem.modules.SpriteRenderModule
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.Vec3d
import java.awt.Color
import kotlin.random.Random

object AoEBurst : ParticleSystem() {
    private val rng = Random(RNG.nextLong())

    override fun configure() {
        val size = bind(1)
        val position = bind(3)
        val previousPosition = bind(3)
        val velocity = bind(3)
        val fromColor = bind(4)
        val toColor = bind(4)

        updateModules += BasicPhysicsUpdateModule(
            position,
            previousPosition,
            velocity,
            gravity = -.005,
            enableCollision = true,
            bounciness = .4f,
            damping = .2f,
            friction = .1f
        )
        renderModules += SpriteRenderModule(
            sprite = ResourceLocation(BlueRPG.MODID, "textures/particles/sparkle_blurred.png"),
            blendMode = BlendMode.NORMAL,
            previousPosition = previousPosition,
            position = position,
            color = LifetimeColorInterpBinding(
                lifetime,
                age,
                ::InterpColorHSV,
                fromColor,
                toColor
            ),
            size = size,
            alphaMultiplier = InterpBinding(lifetime, age, interp = InterpFloatInOut(3, 10, 5))
//                    alphaMultiplier = EaseBinding(lifetime, age, easing = EasingInOut(3, 10, 5), bindingSize = 1)
        )
    }

    fun spawn(lifetime: Int, position: Vec3d, velocity: Vec3d, from: Color, to: Color, size: Double) {
        this.addParticle(
            lifetime.toDouble(),
            size, // size(1)
            position.x, position.y, position.z, // position(3)
            position.x, position.y, position.z, // previousPosition(3)
            velocity.x, velocity.y, velocity.z, // velocity(3)
            from.red / 255.0, from.green / 255.0, from.blue / 255.0, from.alpha / 255.0, // fromColor(4)
            to.red / 255.0, to.green / 255.0, to.blue / 255.0, to.alpha / 255.0 // toColor(4)
        )
    }

    fun burst(origin: Vec3d, from: Color, to: Color) {
        repeat(250) {
            val spread = .6
            spawn(
                rng.nextInt(10, 15),
                origin,
                randomNormal() * spread * rng.nextDouble(.2, 1.2),
                from,
                to,
                rng.nextDouble(.25, .5)
            )
        }
    }
}