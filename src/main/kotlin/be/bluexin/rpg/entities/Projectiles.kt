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

package be.bluexin.rpg.entities

import be.bluexin.rpg.util.Resources
import be.bluexin.saomclib.onClient
import be.bluexin.saomclib.onServer
import com.teamwizardry.librarianlib.features.base.entity.ArrowEntityMod
import com.teamwizardry.librarianlib.features.base.entity.ThrowableEntityMod
import com.teamwizardry.librarianlib.features.helpers.vec
import com.teamwizardry.librarianlib.features.kotlin.div
import com.teamwizardry.librarianlib.features.kotlin.minus
import com.teamwizardry.librarianlib.features.kotlin.motionVec
import com.teamwizardry.librarianlib.features.kotlin.plus
import com.teamwizardry.librarianlib.features.math.interpolate.InterpFunction
import com.teamwizardry.librarianlib.features.math.interpolate.StaticInterp
import com.teamwizardry.librarianlib.features.math.interpolate.numeric.InterpFloatInOut
import com.teamwizardry.librarianlib.features.particle.ParticleBase
import com.teamwizardry.librarianlib.features.particle.ParticleBuilder
import com.teamwizardry.librarianlib.features.particle.ParticleSpawner
import com.teamwizardry.librarianlib.features.particle.functions.InterpColorHSV
import com.teamwizardry.librarianlib.features.particle.functions.TickFunction
import com.teamwizardry.librarianlib.features.particle.spawn
import com.teamwizardry.librarianlib.features.saving.Savable
import com.teamwizardry.librarianlib.features.saving.Save
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.projectile.EntityArrow
import net.minecraft.item.ItemStack
import net.minecraft.util.DamageSource
import net.minecraft.util.math.RayTraceResult
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import java.awt.Color

@Savable
class EntityRpgArrow : ArrowEntityMod {

    @Save
    var initialX = 0.0
    @Save
    var initialY = 0.0
    @Save
    var initialZ = 0.0

    @Suppress("unused")
    constructor(world: World) : super(world)

    constructor(world: World, shooter: EntityLivingBase) : super(world, shooter)

    init {
        pickupStatus = EntityArrow.PickupStatus.DISALLOWED
    }

    override fun shoot(shooter: Entity, pitch: Float, yaw: Float, p_184547_4_: Float, velocity: Float, inaccuracy: Float) {
        super.shoot(shooter, pitch, yaw, p_184547_4_, velocity, inaccuracy)
        initialX = super.posX
        initialY = super.posY
        initialZ = super.posZ
    }

    override fun getArrowStack(): ItemStack = ItemStack.EMPTY

    override fun onUpdate() {
        super.onUpdate()

        world onClient {
            if (initialX == 0.0) {
                initialX = super.posX
                initialY = super.posY
                initialZ = super.posZ
            }
        }

        if (this.getDistanceSq(initialX, initialY, initialZ) > 200) {
            this.setDead()
            world.removeEntity(this)
        }
    }
}

class EntityWandProjectile : ThrowableEntityMod {

    @Save
    var initialX = 0.0
    @Save
    var initialY = 0.0
    @Save
    var initialZ = 0.0

    @Save
    var damage = 0.0

    @Save
    var caster: EntityLivingBase? = null

    @Suppress("unused")
    constructor(world: World) : super(world)

    constructor(world: World, caster: EntityLivingBase) : super(world, caster) {
        this.caster = caster
    }

    override fun onUpdate() {
        super.onUpdate()

        world onClient {
            renderParticles()
            if (initialX == 0.0) {
                initialX = super.posX
                initialY = super.posY
                initialZ = super.posZ
            }
        }

        if (this.inWater || this.getDistanceSq(initialX, initialY, initialZ) > 200) {
            this.setDead()
            world.removeEntity(this)
        }
    }

    override fun shoot(entityThrower: Entity, rotationPitchIn: Float, rotationYawIn: Float, pitchOffset: Float, velocity: Float, inaccuracy: Float) {
        super.shoot(entityThrower, rotationPitchIn, rotationYawIn, pitchOffset, velocity, inaccuracy)
        initialX = super.posX
        initialY = super.posY
        initialZ = super.posZ
    }

    override fun onImpact(result: RayTraceResult) {
        val e = result.entityHit
        if (e != null) {
            val s = DamageSource.causeIndirectMagicDamage(this, caster ?: this)
            e.attackEntityFrom(s, damage.toFloat())
        }

        world onServer {
            setDead()
            world.removeEntity(this)
        }
    }

    @SideOnly(Side.CLIENT)
    private fun renderParticles() {
        val from = Color(0xFF0000)
        val to = Color(0xFFB10B)
        val center = positionVector + vec(0, height / 2, 0)

        val trail = ParticleBuilder(10).apply {
            setAlphaFunction(object : InterpFunction<Float> {
                override fun get(i: Float): Float {
                    val a = 0.05f
                    val b = 0.6f

                    var alpha = if (i < 1 && i > a) 1f else 0f

                    if (i <= a * 2) alpha = (i - a) / a
                    else if (i >= 1 - b) {
                        alpha = 1 - (i - (1 - b)) / b
                    }
                    return if (alpha < 0) 0f else alpha
                }
            })
            setRender(Resources.PARTICLE)
            setCollision(true)
            canBounce = true
            setColorFunction(InterpColorHSV(from, to))
        }

        trail.setMotion(Vec3d.ZERO)
        trail.setAcceleration(Vec3d.ZERO)
        ParticleSpawner.spawn(trail, world, StaticInterp(center), 6, 1) { _, b ->
            b.setScaleFunction(object : InterpFunction<Float> {
                private val start = rand.nextFloat() / 2 + 0.3f
                private val finish = 0f

                override operator fun get(i: Float): Float {
                    return Math.abs(start * (1 - i) + finish * i)
                }
            })
            b.setPositionOffset(b.positionOffset - motionVec / 6)
            b.setLifetime(rand.nextInt(20) + 20)
            b.addMotion(Vec3d(
                    rand.nextDouble() * 0.02 - 0.01,
                    rand.nextDouble() * 0.02 - 0.01,
                    rand.nextDouble() * 0.02 - 0.01
            ))
            b.addAcceleration(Vec3d(
                    rand.nextDouble() * 0.001 - 0.0005,
                    rand.nextDouble() * 0.001 - 0.0005,
                    rand.nextDouble() * 0.001 - 0.0005
            ))
        }

        val core = ParticleBuilder(10).apply {
            setAlphaFunction(InterpFloatInOut(0.3f, 0.6f))
            setRender(Resources.PARTICLE)
            setCollision(false)
            canBounce = false
            setColorFunction(InterpColorHSV(from, to))
            setAcceleration(Vec3d.ZERO)
        }

        core.setMotion(motionVec)
        core.setTick(object : TickFunction {
            override fun tick(particle: ParticleBase) {
                if (isDead) particle.setExpired()
            }
        })
        ParticleSpawner.spawn(core, world, StaticInterp(center), 6, 1) { _, build ->
            build.setLifetime(rand.nextInt(15) + 5)
            build.setScaleFunction(object : InterpFunction<Float> {
                private val start = rand.nextFloat() / 2 + 1.5f
                private val finish = 0f

                override operator fun get(i: Float): Float {
                    return Math.abs(start * (1 - i) + finish * i)
                }
            })
        }
    }

    override fun getGravityVelocity(): Float {
        return 0.001f
    }

}