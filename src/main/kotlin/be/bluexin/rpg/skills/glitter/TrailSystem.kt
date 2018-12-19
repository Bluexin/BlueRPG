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
import be.bluexin.rpg.skills.glitter.ext.EasingInOut
import be.bluexin.rpg.skills.glitter.ext.ScaledBinding
import be.bluexin.rpg.util.RNG
import be.bluexin.rpg.util.randomNormal
import com.teamwizardry.librarianlib.core.client.ClientTickHandler
import com.teamwizardry.librarianlib.features.helpers.vec
import com.teamwizardry.librarianlib.features.particle.functions.InterpColorHSV
import com.teamwizardry.librarianlib.features.particlesystem.BlendMode
import com.teamwizardry.librarianlib.features.particlesystem.ParticleSystem
import com.teamwizardry.librarianlib.features.particlesystem.ParticleUpdateModule
import com.teamwizardry.librarianlib.features.particlesystem.bindings.CallbackBinding
import com.teamwizardry.librarianlib.features.particlesystem.bindings.ConstantBinding
import com.teamwizardry.librarianlib.features.particlesystem.bindings.EaseBinding
import com.teamwizardry.librarianlib.features.particlesystem.modules.AccelerationUpdateModule
import com.teamwizardry.librarianlib.features.particlesystem.modules.BasicPhysicsUpdateModule
import com.teamwizardry.librarianlib.features.particlesystem.modules.SpriteRenderModule
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.Vec3d
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import java.awt.Color
import kotlin.collections.set
import kotlin.random.Random

@SideOnly(Side.CLIENT)
interface TrailSystem {
    fun spawn(lifetime: Int, position: Vec3d, velocity: Vec3d, color1: Color, color2: Color, size: Double)

    companion object {

        private val systems = mutableMapOf<ResourceLocation, TrailSystem>()

        operator fun get(rl: ResourceLocation) =
            systems[rl] ?: None.apply { BlueRPG.LOGGER.warn("Invalid Trail system found: `$rl`") }

        operator fun set(rl: ResourceLocation, system: TrailSystem) {
            systems[rl] = system
        }

        fun load() {
            None
            Ice
            Embers
        }

        object None : TrailSystem {

            init {
                TrailSystem[ResourceLocation(BlueRPG.MODID, "none")] = this
            }

            override fun spawn(
                lifetime: Int,
                position: Vec3d,
                velocity: Vec3d,
                color1: Color,
                color2: Color,
                size: Double
            ) =
                Unit
        }

        object Ice : ParticleSystem(), TrailSystem {

            init {
                TrailSystem[ResourceLocation(BlueRPG.MODID, "ice")] = this
            }

            private val rng = Random(RNG.nextLong())

            override fun configure() {
                val size = bind(1)
                val position = bind(3)
                val previousPosition = bind(3)
                val velocity = bind(3)
                val rotation = bind(3)
                val previousRotation = bind(3)
                val yawRotate = bind(1)
                val pitchRotate = bind(1)
                val color = bind(4)

                updateModules += BasicPhysicsUpdateModule(
                    position,
                    previousPosition,
                    velocity,
                    gravity = .0,
                    enableCollision = false,
                    damping = .01f
                )
                updateModules += AccelerationUpdateModule(
                    velocity,
                    CallbackBinding(3) { _, contents ->
                        repeat(3) {
                            contents[it] = rng.nextDouble(-.01, .01)
                        }
                    }
                )
                updateModules += object : ParticleUpdateModule {
                    override fun update(particle: DoubleArray) {
                        yawRotate.load(particle)
                        val yaw = yawRotate.contents[0]
                        pitchRotate.load(particle)
                        val pitch = pitchRotate.contents[0]
                        rotation.load(particle)
                        previousRotation.set(particle, *rotation.contents)
                        var rotationVec = vec(rotation.contents[0], rotation.contents[1], rotation.contents[2])
                        if (yaw != .0) rotationVec = rotationVec.rotateYaw(yaw.toFloat())
                        if (pitch != .0) rotationVec = rotationVec.rotatePitch(pitch.toFloat())
                        rotation.set(particle, rotationVec.x, rotationVec.y, rotationVec.z)
                    }
                }
                val facing = CallbackBinding(3) { particle, contents ->
                    rotation.load(particle)
                    previousRotation.load(particle)

                    repeat(3) {
                        contents[it] = ClientTickHandler.interpWorldPartialTicks(
                            previousRotation.contents[it],
                            rotation.contents[it]
                        )
                    }
                }
                renderModules += SpriteRenderModule(
                    sprite = ResourceLocation(BlueRPG.MODID, "textures/particles/snowflake_1.png"),
                    blendMode = BlendMode.NORMAL,
                    previousPosition = previousPosition,
                    position = position,
                    color = color,
                    size = EaseBinding(
                        lifetime, age,
                        target = size,
                        origin = ConstantBinding(.0),
                        easing = EasingInOut(1, 7, 6),
                        bindingSize = 1
                    ),
                    facingVector = facing,
                    alphaMultiplier = EaseBinding(lifetime, age, easing = EasingInOut(3, 10, 5), bindingSize = 1),
                    depthMask = true
                )
                renderModules += SpriteRenderModule(
                    sprite = ResourceLocation(BlueRPG.MODID, "textures/particles/sparkle_blurred.png"),
                    blendMode = BlendMode.ADDITIVE,
                    previousPosition = previousPosition,
                    position = position,
                    color = color,
                    size = EaseBinding(
                        lifetime, age,
                        target = ScaledBinding(size, 1.5),
                        origin = ConstantBinding(.0),
                        easing = EasingInOut(1, 7, 6),
                        bindingSize = 1
                    ),
                    facingVector = facing,
                    alphaMultiplier = EaseBinding(
                        lifetime,
                        age,
                        easing = EasingInOut(3, 10, 5),
                        bindingSize = 1,
                        origin = ConstantBinding(.3),
                        target = ConstantBinding(.3)
                    ),
                    depthMask = false
                )
            }

            override fun spawn(
                lifetime: Int,
                position: Vec3d,
                velocity: Vec3d,
                color1: Color,
                color2: Color,
                size: Double
            ) {
                val rotation = randomNormal()
                val color = InterpColorHSV(color1, color2).get(rng.nextFloat())
                this.addParticle(
                    lifetime.toDouble(),
                    size, // size(1)
                    position.x, position.y, position.z, // position(3)
                    position.x, position.y, position.z, // previousPosition(3)
                    velocity.x, velocity.y, velocity.z, // velocity(3)
                    rotation.x, rotation.y, rotation.z, // rotation
                    rotation.x, rotation.y, rotation.z, // previousRotation
                    rng.nextDouble(.1, .2), // yawRotate
                    rng.nextDouble(.1, .2), // pitchRotate
                    color.red / 255.0, color.green / 255.0, color.blue / 255.0, color.alpha / 255.0 // color(4)
                )
            }
        }

        object Embers : ParticleSystem(), TrailSystem {

            init {
                TrailSystem[ResourceLocation(BlueRPG.MODID, "embers")] = this
            }

            private val rng = Random(RNG.nextLong())

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
                    gravity = .0,
                    enableCollision = false,
                    damping = .01f
                )
                updateModules += AccelerationUpdateModule(
                    velocity,
                    CallbackBinding(3) { _, contents ->
                        repeat(3) {
                            contents[it] = rng.nextDouble(-.01, .01)
                        }
                    }
                )
                renderModules += SpriteRenderModule(
                    sprite = ResourceLocation(BlueRPG.MODID, "textures/particles/sparkle_blurred.png"),
                    blendMode = BlendMode.ADDITIVE,
                    previousPosition = previousPosition,
                    position = position,
                    color = color,
                    size = EaseBinding(
                        lifetime, age,
                        target = size,
                        origin = ConstantBinding(.0),
                        easing = EasingInOut(1, 7, 6),
                        bindingSize = 1
                    ),
                    alphaMultiplier = EaseBinding(
                        lifetime,
                        age,
                        easing = EasingInOut(3, 10, 5),
                        bindingSize = 1
                    ),
                    depthMask = false
                )
            }

            override fun spawn(
                lifetime: Int,
                position: Vec3d,
                velocity: Vec3d,
                color1: Color,
                color2: Color,
                size: Double
            ) {
                val color = InterpColorHSV(color1, color2).get(rng.nextFloat())
                this.addParticle(
                    lifetime.toDouble(),
                    size, // size(1)
                    position.x, position.y, position.z, // position(3)
                    position.x, position.y, position.z, // previousPosition(3)
                    velocity.x, velocity.y, velocity.z, // velocity(3)
                    color.red / 255.0, color.green / 255.0, color.blue / 255.0, color.alpha / 255.0 // color(4)
                )
            }
        }

    }
}
