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
import be.bluexin.rpg.util.RNG
import be.bluexin.rpg.util.Resources
import com.teamwizardry.librarianlib.features.animator.Easing
import com.teamwizardry.librarianlib.features.kotlin.randomNormal
import com.teamwizardry.librarianlib.features.kotlin.times
import com.teamwizardry.librarianlib.features.math.interpolate.InterpFunction
import com.teamwizardry.librarianlib.features.math.interpolate.StaticInterp
import com.teamwizardry.librarianlib.features.math.interpolate.numeric.InterpFloatInOut
import com.teamwizardry.librarianlib.features.math.rotate
import com.teamwizardry.librarianlib.features.math.rotationMatrix
import com.teamwizardry.librarianlib.features.particle.ParticleBuilder
import com.teamwizardry.librarianlib.features.particle.ParticleSpawner
import com.teamwizardry.librarianlib.features.particle.functions.InterpColorHSV
import com.teamwizardry.librarianlib.features.particle.spawn
import com.teamwizardry.librarianlib.features.particlesystem.BlendMode
import com.teamwizardry.librarianlib.features.particlesystem.ParticleSystem
import com.teamwizardry.librarianlib.features.particlesystem.ReadParticleBinding
import com.teamwizardry.librarianlib.features.particlesystem.bindings.InterpBinding
import com.teamwizardry.librarianlib.features.particlesystem.modules.BasicPhysicsUpdateModule
import com.teamwizardry.librarianlib.features.particlesystem.modules.SpriteRenderModule
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import java.awt.Color
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

object GlitterAoE {
    private val rng = Random(RNG.nextLong())

    fun test(world: World, center: Vec3d, from: Color = Color(0xFF0000), to: Color = Color(0xFFBF00)) {
//        BlueRPG.LOGGER.warn("Spawn @$center")
        val p = ParticleBuilder(200).apply {
            setColor(Color.CYAN)
            setRender(Resources.PARTICLE)
            setMotion(Vec3d.ZERO)
            setAcceleration(Vec3d.ZERO)
        }
        ParticleSpawner.spawn(p, world, StaticInterp(center), 0)

        val core = ParticleBuilder(10).apply {
            setAlphaFunction(object : InterpFunction<Float> {
                override fun get(i: Float) = min(1f, max(0f, 1f - Easing.easeInQuint(i)))
            })
            setRender(Resources.PARTICLE)
//            setCollision(true)
            setColorFunction(InterpColorHSV(from, to))
//            setMovementMode(EnumMovementMode.TOWARD_POINT)
            setMotion(Vec3d(0.0, 0.05, 0.0))
            setAcceleration(Vec3d.ZERO)
        }

        ParticleSpawner.spawn(core, world, StaticInterp(center), 600, 5) { _, build ->
            build.setLifetime(rng.nextInt(10/* + 50*/) + 5)
            build.setScaleFunction(object : InterpFunction<Float> {
                private val start = rng.nextFloat() / 2 + 1.5f
                private val finish = rng.nextFloat() / 2 + 0.5f

                override operator fun get(i: Float) =
                    min(1f, max(0f, 1f - Easing.easeInQuint(i))) * (finish - start) + start
            })
            val d = rng.nextDouble() / 2 + 0.5
            val target =
                rotationMatrix(rng.nextDouble() * 2 * PI, rng.nextDouble() * 2 * PI, rng.nextDouble() * 2 * PI).rotate(
                    Vec3d(d, d, d)
                )
            build.setPositionFunction(InterpEasing3d(Easing.BezierEasing(0.175, 0.885, 0.32, 1.275), target))
        }
    }

    fun test2(world: World, origin: Vec3d, from: Color = Color(0xFFDD0B), to: Color = Color(0xFF0000)) {
        repeat(250) {
            val spread = .6
            Glitter.spawn(
                rng.nextInt(10, 15).toDouble(),
                origin,
                randomNormal() * spread * rng.nextDouble(.2, 1.2),
                from,
                to,
                rng.nextDouble() / 4 + 0.25
            )
        }
    }
}

class InterpEasing(val easing: Easing, val from: Float = 0f, val to: Float = 1f) : InterpFunction<Float> {
    override fun get(i: Float): Float {
        return min(1f, from + easing(i) * (to - from))
    }
}

class InterpEasing3d(val easing: Easing, private val targetRelative: Vec3d) : InterpFunction<Vec3d> {
    override fun get(i: Float): Vec3d {
        return targetRelative * easing(i)
    }
}

object Glitter : ParticleSystem() {
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
            gravity = -0.005,
            enableCollision = true,
            bounciness = 0.4f,
            damping = 0.2f,
            friction = 0.1f
        )
        renderModules += SpriteRenderModule(
            sprite = ResourceLocation(BlueRPG.MODID, "textures/particles/sparkle_blurred.png"),
            blendMode = BlendMode.NORMAL,
            previousPosition = previousPosition,
            position = position,
            color = LifetimeColorInterpBinding(lifetime, age, ::InterpColorHSV, fromColor, toColor),
            size = size,
            alphaMultiplier = InterpBinding(lifetime, age, interp = InterpFloatInOut(3, 10, 5))
        )
    }

    fun spawn(lifetime: Double, position: Vec3d, velocity: Vec3d, from: Color, to: Color, size: Double) {
        this.addParticle(
            lifetime,
            size, // size(1)
            position.x, position.y, position.z, // position(3)
            position.x, position.y, position.z, // previousPosition(3)
            velocity.x, velocity.y, velocity.z, // velocity(3)
            from.red / 255.0, from.green / 255.0, from.blue / 255.0, from.alpha / 255.0, // fromColor(4)
            to.red / 255.0, to.green / 255.0, to.blue / 255.0, to.alpha / 255.0 // toColor(4)
        )
    }
}

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
        BlueRPG.LOGGER.info("Fraction: $fraction (age = ${age.contents[0]}, lifetime = ${lifetime.contents[0]})")
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