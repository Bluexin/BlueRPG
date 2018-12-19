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
import be.bluexin.rpg.entities.EntitySkillProjectile
import be.bluexin.rpg.skills.glitter.ext.EasingInOut
import be.bluexin.rpg.skills.glitter.ext.LifetimeColorInterpBinding
import be.bluexin.rpg.util.RNG
import be.bluexin.rpg.util.randomNormal
import com.teamwizardry.librarianlib.features.animator.Easing
import com.teamwizardry.librarianlib.features.helpers.vec
import com.teamwizardry.librarianlib.features.kotlin.*
import com.teamwizardry.librarianlib.features.particle.functions.InterpColorHSV
import com.teamwizardry.librarianlib.features.particlesystem.BlendMode
import com.teamwizardry.librarianlib.features.particlesystem.ParticleSystem
import com.teamwizardry.librarianlib.features.particlesystem.bindings.ConstantBinding
import com.teamwizardry.librarianlib.features.particlesystem.bindings.EaseBinding
import com.teamwizardry.librarianlib.features.particlesystem.modules.BasicPhysicsUpdateModule
import com.teamwizardry.librarianlib.features.particlesystem.modules.SpriteRenderModule
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.Vec3d
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import java.awt.Color
import kotlin.random.Random

@SideOnly(Side.CLIENT)
object ProjectileCore : ParticleSystem() {

    private val rng = Random(RNG.nextLong())

    fun EntitySkillProjectile.renderParticles(store: MutableList<DoubleArray>) {
        val pos = positionVector + vec(0, height / 2, 0)
        val motionVec = motionVec
        val motionPartial = motionVec / 6
        val color1 = Color(this.color1)
        val color2 = Color(this.color2)
        repeat(6) {
            store += ProjectileCore.spawn(
                rng.nextInt(5, 20),
                pos - motionPartial * it,
                motionVec,
                color1, color2,
                rng.nextDouble(.25, .38)
            )
        }

        val spread = .04
        repeat(6) {
            trailSystem.spawn(
                rng.nextInt(15, 30),
                pos - motionPartial * it,
                randomNormal() * spread * rng.nextDouble(.2, 1.2),
                color1, color2,
                rng.nextDouble(.05, .125)
            )
        }
    }

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
            gravity = .0,
            enableCollision = false,
            damping = .03f
        )
        renderModules += SpriteRenderModule(
            sprite = ResourceLocation(BlueRPG.MODID, "textures/particles/sparkle_blurred.png"),
            blendMode = BlendMode.ADDITIVE,
            previousPosition = previousPosition,
            position = position,
            color = LifetimeColorInterpBinding(
                lifetime,
                age,
                ::InterpColorHSV,
                fromColor,
                toColor
            ),
            size = EaseBinding(
                lifetime, age,
                origin = size,
                target = ConstantBinding(.0),
                easing = Easing.linear,
                bindingSize = 1
            ),
            alphaMultiplier = EaseBinding(lifetime, age, easing = EasingInOut(0, 1, 6), bindingSize = 1)
        )
    }

    fun spawn(lifetime: Int, position: Vec3d, velocity: Vec3d, from: Color, to: Color, size: Double) =
        this.addParticle(
            lifetime.toDouble(),
            size, // size(1)
            position.x, position.y, position.z, // position(3)
            position.x - velocity.x, position.y - velocity.y, position.z - velocity.z, // previousPosition(3)
            velocity.x, velocity.y, velocity.z, // velocity(3)
            from.red / 255.0, from.green / 255.0, from.blue / 255.0, from.alpha / 255.0, // fromColor(4)
            to.red / 255.0, to.green / 255.0, to.blue / 255.0, to.alpha / 255.0 // toColor(4)
        )

    fun killParticles(particles: List<DoubleArray>) {
        for (particle in particles) age.set(particle, Double.POSITIVE_INFINITY)
    }
}