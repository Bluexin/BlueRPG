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

package be.bluexin.rpg.skills

import be.bluexin.rpg.BlueRPG
import be.bluexin.rpg.devutil.RpgProjectile
import be.bluexin.rpg.devutil.RpgProjectileRender
import be.bluexin.rpg.devutil.createResourceLocationKey
import be.bluexin.rpg.skills.glitter.ProjectileCore
import be.bluexin.rpg.skills.glitter.ProjectileCore.renderParticles
import be.bluexin.rpg.skills.glitter.TrailSystem
import be.bluexin.saomclib.onClient
import be.bluexin.saomclib.onServer
import com.teamwizardry.librarianlib.features.base.entity.ThrowableEntityMod
import com.teamwizardry.librarianlib.features.helpers.vec
import com.teamwizardry.librarianlib.features.kotlin.createFloatKey
import com.teamwizardry.librarianlib.features.kotlin.createIntKey
import com.teamwizardry.librarianlib.features.kotlin.managedValue
import com.teamwizardry.librarianlib.features.saving.Savable
import com.teamwizardry.librarianlib.features.saving.Save
import io.netty.buffer.ByteBuf
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.runBlocking
import net.minecraft.client.renderer.entity.RenderManager
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.RayTraceResult
import net.minecraft.world.World
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import java.lang.StrictMath.pow
import java.util.*
import kotlin.collections.HashSet

@Savable
class SkillProjectileEntity : ThrowableEntityMod, RpgProjectile, IEntityAdditionalSpawnData {

    internal companion object {
        // TODO: move all these to spawnData
        private val RANGE = SkillProjectileEntity::class.createFloatKey()

        private val COLOR_1 = SkillProjectileEntity::class.createIntKey()
        private val COLOR_2 = SkillProjectileEntity::class.createIntKey()

        private val TRAIL_SYSTEM = SkillProjectileEntity::class.createResourceLocationKey()
    }

    private val alreadyHit = HashSet<Entity>()

    @Save
    var initialX = 0.0
    @Save
    var initialY = 0.0
    @Save
    var initialZ = 0.0

    @Save
    override var computedDamage = 0.0

    @Save
    override var knockback: Int = 0

    var context: SkillContext? = null

    var range by managedValue(RANGE)
    var color1 by managedValue(COLOR_1)
    var color2 by managedValue(COLOR_2)
    var trailSystemKey by managedValue(TRAIL_SYSTEM)
    val trailSystem by lazy { TrailSystem[trailSystemKey] }

    private var result: SendChannel<Target>? = null

    private var filter: Condition? = null

    private var precise = false

    var passtrough: Boolean = false

    private val spawnedParticles = LinkedList<DoubleArray>()

    @Suppress("unused")
    constructor(world: World) : super(world)

    constructor(
        world: World,
        context: SkillContext,
        origin: TargetWithPosition,
        range: Double,
        result: SendChannel<Target>,
        filter: Condition? = null,
        precise: Boolean = false,
        passtrough: Boolean = false,
        width: Float = .25f,
        height: Float = .25f
    ) : super(world, origin.x, origin.y, origin.z) {
        this.context = context
        this.result = result
        this.range = pow(range, 2.0).toFloat()
        this.thrower = context.caster
        this.filter = filter
        this.precise = precise
        this.passtrough = passtrough
        this.setSize(width, height)
    }

    override fun readSpawnData(additionalData: ByteBuf) {
        this.passtrough = additionalData.readBoolean()
    }

    override fun writeSpawnData(buffer: ByteBuf) {
        buffer.writeBoolean(this.passtrough)
    }

    override fun entityInit() { // Warning: this runs before CTOR
        super.entityInit()
        this.getDataManager().register(RANGE, 15f)
        this.getDataManager().register(COLOR_1, 0xFF0000)
        this.getDataManager().register(COLOR_2, 0xFFB10B)
        this.getDataManager().register(TRAIL_SYSTEM, ResourceLocation(BlueRPG.MODID, "ice"))
    }

    override fun realShoot(shooter: TargetWithLookVec, pitchOffset: Float, velocity: Float, inaccuracy: Float) {
        initialX = super.posX
        initialY = super.posY
        initialZ = super.posZ
        val lookVec = shooter.lookVec
        if (shooter is LivingHolder<*>) thrower = shooter.it
        this.shoot(lookVec.x, lookVec.y, lookVec.z, velocity, inaccuracy)

        if (shooter is TargetWithMovement) {
            this.motionX += shooter.motionX
            this.motionZ += shooter.motionZ

            if (shooter is TargetWithCollision && !shooter.onGround) {
                this.motionY += shooter.motionY
            }
        }
    }

    override fun onImpact(result: RayTraceResult) {
        if (!this.isDead) world onServer {
            val r = this.result
            if (r != null) {
                val e = result.entityHit
                if (e is EntityLivingBase) {
                    @Suppress("UNCHECKED_CAST")
                    val h = (if (precise) WorldPosHolder(world, positionVector) else LivingHolder(e))
                    val f = filter
                    val c = context
                    if (f == null || c == null || f(c, h)) {
                        runBlocking { r.send(h) }
                        if (!passtrough) setDead()
                    }
                } else {
                    val v = result.hitVec
                    @Suppress("UNCHECKED_CAST")
                    val h = WorldPosHolder(world, v)
                    val f = filter
                    val c = context
                    if (f == null || c == null || f(c, h)) {
                        runBlocking { r.send(h) }
                        if (!passtrough) setDead()
                    }
                }
            }
        }
    }

    override fun getGravityVelocity(): Float {
        return 0.001f
    }

    override fun setDead() {
        if (isDead) return
        world onClient {
            ProjectileCore.killParticles(this.spawnedParticles)
        }
        result?.close()
        super.setDead()
    }

    override fun onUpdate() {
        //region super<EntityThrowable>::onUpdate
        this.lastTickPosX = this.posX
        this.lastTickPosY = this.posY
        this.lastTickPosZ = this.posZ
        //endregion

        //region super<Entity>::onUpdate
        if (!this.world.isRemote) {
            this.setFlag(6, this.isGlowing)
        }

        this.onEntityUpdate()
        //endregion

        //region super<EntityThrowable>::onUpdate
        if (this.throwableShake > 0) --this.throwableShake

        if (this.inGround) {
            if (this.world.getBlockState(BlockPos(this.xTile, this.yTile, this.zTile)).block === this.inTile) {
                if (++this.ticksInGround == 1200) this.setDead()
                return
            }

            this.inGround = false
            this.motionX *= (this.rand.nextFloat() * 0.2f).toDouble()
            this.motionY *= (this.rand.nextFloat() * 0.2f).toDouble()
            this.motionZ *= (this.rand.nextFloat() * 0.2f).toDouble()
            this.ticksInGround = 0
            this.ticksInAir = 0
        } else ++this.ticksInAir
        //endregion

        world onServer {
            val pos = this.positionVector
            val posWithMove = vec(this.posX + this.motionX, this.posY + this.motionY, this.posZ + this.motionZ)
            val hits = this.world.getEntitiesWithinAABBExcludingEntity(
                this,
                this.entityBoundingBox.expand(this.motionX, this.motionY, this.motionZ).grow(1.0)
            ).asSequence()
                .filter { it !== this.thrower }
                .filter { it !in this.alreadyHit }
                .filter(Entity::canBeCollidedWith)
                .mapNotNull {
                    val axisalignedbb = it.entityBoundingBox.grow(0.30000001192092896)
                    val r = axisalignedbb.calculateIntercept(pos, posWithMove)
                    r?.entityHit = it
                    r
                }.ifEmpty {
                    sequenceOf(this.world.rayTraceBlocks(pos, posWithMove)).filterNotNull()
                }

            hits.forEach {
                if (!net.minecraftforge.event.ForgeEventFactory.onProjectileImpact(this, it)) this.onImpact(it)
            }

            this.alreadyHit += hits.mapNotNull { it.entityHit }.toList()
        }

        //region super<EntityThrowable>::onUpdate
        this.posX += this.motionX
        this.posY += this.motionY
        this.posZ += this.motionZ
        val f = MathHelper.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ)
        this.rotationYaw = (MathHelper.atan2(this.motionX, this.motionZ) * (180.0 / Math.PI)).toFloat()

        this.rotationPitch = (MathHelper.atan2(this.motionY, f.toDouble()) * (180.0 / Math.PI)).toFloat()
        while (this.rotationPitch - this.prevRotationPitch < -180.0f) this.prevRotationPitch -= 360.0f
        while (this.rotationPitch - this.prevRotationPitch >= 180.0f) this.prevRotationPitch += 360.0f
        while (this.rotationYaw - this.prevRotationYaw < -180.0f) this.prevRotationYaw -= 360.0f
        while (this.rotationYaw - this.prevRotationYaw >= 180.0f) this.prevRotationYaw += 360.0f

        this.rotationPitch = this.prevRotationPitch + (this.rotationPitch - this.prevRotationPitch) * 0.2f
        this.rotationYaw = this.prevRotationYaw + (this.rotationYaw - this.prevRotationYaw) * 0.2f

        this.motionX *= .99
        this.motionY *= .99
        this.motionZ *= .99

        if (!this.hasNoGravity()) this.motionY -= this.gravityVelocity.toDouble()

        this.setPosition(this.posX, this.posY, this.posZ)
        //endregion

        world onClient {
            renderParticles(spawnedParticles)
            if (initialX == 0.0) {
                initialX = super.posX
                initialY = super.posY
                initialZ = super.posZ
            }
        }

        if (this.inWater || this.getDistanceSq(initialX, initialY, initialZ) > range) this.setDead()
    }
}

@SideOnly(Side.CLIENT)
class SkillProjectileRender(renderManager: RenderManager) : RpgProjectileRender<SkillProjectileEntity>(renderManager)