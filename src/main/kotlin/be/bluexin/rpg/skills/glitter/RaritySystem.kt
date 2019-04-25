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

import be.bluexin.rpg.BlueRPG
import be.bluexin.rpg.gear.RPGItemEntity
import be.bluexin.rpg.stats.stats
import be.bluexin.rpg.util.RNG
import be.bluexin.rpg.util.randomNormalWithOutgoingSpeed
import com.teamwizardry.librarianlib.features.helpers.vec
import com.teamwizardry.librarianlib.features.kotlin.motionVec
import com.teamwizardry.librarianlib.features.kotlin.plus
import com.teamwizardry.librarianlib.features.kotlin.times
import com.teamwizardry.librarianlib.features.math.interpolate.numeric.InterpFloatInOut
import com.teamwizardry.librarianlib.features.particlesystem.BlendMode
import com.teamwizardry.librarianlib.features.particlesystem.ParticleSystem
import com.teamwizardry.librarianlib.features.particlesystem.bindings.InterpBinding
import com.teamwizardry.librarianlib.features.particlesystem.modules.BasicPhysicsUpdateModule
import com.teamwizardry.librarianlib.features.particlesystem.modules.SpriteRenderModule
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.Vec3d
import java.awt.Color
import kotlin.random.Random

object RaritySystem : ParticleSystem() {

    private val rng = Random(RNG.nextLong())

    fun RPGItemEntity.renderParticles() {
        val stats = this.item.stats ?: return
        if (stats.rarity?.shouldGlitter != true) return
        val pos = positionVector + vec(0, height * 1.5, 0)
        val motionVec = motionVec + vec(0, .03, 0)
        val color = Color(stats.rarity!!.colorRGB)
        repeat(6) {
            val (posNormal, speedNormal) = randomNormalWithOutgoingSpeed(.8f)
            spawn(
                rng.nextInt(5, 45),
                pos + posNormal * .15,
                motionVec + speedNormal * rng.nextDouble(.01, .03),
                color,
                rng.nextDouble(.03, .1)
            )
        }
    }

    override fun configure() {
        val size = bind(1)
        val position = bind(3)
        val previousPosition = bind(3)
        val velocity = bind(3)
        val color = bind(4)

        updateModules += BasicPhysicsUpdateModule(
            position,
            previousPosition,
            velocity,
            gravity = -.0075,
            enableCollision = false,
            damping = .01f
        )
        renderModules += SpriteRenderModule(
            sprite = ResourceLocation(BlueRPG.MODID, "textures/particles/sparkle_blurred.png"),
            blendMode = BlendMode.ADDITIVE,
            previousPosition = previousPosition,
            position = position,
            color = color,
            size = size,
            alphaMultiplier = InterpBinding(lifetime, age, interp = InterpFloatInOut(3, 10, 5))
        )
    }

    fun spawn(lifetime: Int, position: Vec3d, velocity: Vec3d, color: Color, size: Double) =
        this.addParticle(
            lifetime.toDouble(),
            size, // size(1)
            position.x, position.y, position.z, // position(3)
            position.x - velocity.x, position.y - velocity.y, position.z - velocity.z, // previousPosition(3)
            velocity.x, velocity.y, velocity.z, // velocity(3)
            color.red / 255.0, color.green / 255.0, color.blue / 255.0, color.alpha / 255.0 // color(4)
        )
}