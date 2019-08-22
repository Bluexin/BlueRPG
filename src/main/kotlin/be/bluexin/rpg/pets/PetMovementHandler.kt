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

package be.bluexin.rpg.pets

import be.bluexin.rpg.devutil.Localizable
import be.bluexin.rpg.devutil.RNG
import be.bluexin.rpg.devutil.delegate
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.SharedMonsterAttributes
import net.minecraft.entity.ai.EntityAISwimming
import net.minecraft.entity.ai.EntityJumpHelper
import net.minecraft.entity.ai.EntityMoveHelper
import net.minecraft.pathfinding.PathNavigate
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d

enum class PetMovementType(private val handler: (PetEntity) -> PetMovementHandler) : Localizable {
    WALK(::WalkHandler),
    HOP(::HopHandler),
    BOUNCE(::BounceHandler),
    FLOAT(::FloatHandler),
    FLAP(::FlapHandler);

    operator fun invoke(pet: PetEntity) = this.handler(pet)
}

abstract class PetMovementHandler(val pet: PetEntity) {

    val moveHelper: EntityMoveHelper by pet::getMoveHelper.delegate
    val jumpHelper: EntityJumpHelper by pet::getJumpHelper.delegate
    val navigator: PathNavigate by pet::getNavigator.delegate

    abstract fun onLivingUpdate()

    open fun handleRotationFloat(partialTicks: Float) = this.pet.ticksExisted + partialTicks

    open fun preRenderCallback(partialTicks: Float) = Unit

    open fun createMoveHelper() = EntityMoveHelper(pet)

    open fun createJumpHelper() = EntityJumpHelper(pet)

    open fun updateAITasks() = Unit

    open fun registerAI() {
        this.pet.tasks.addTask(1, EntityAISwimming(this.pet))
    }

    open fun getJumpUpwardsMotion() = 0.42f

    open fun jump() = Unit
}

open class WalkHandler(pet: PetEntity) : PetMovementHandler(pet) {
    override fun onLivingUpdate() = Unit
}

open class HopHandler(pet: PetEntity) : PetMovementHandler(pet) {
    private var jumpTicks: Int = 0
    private var jumpDuration: Int = 0
    private var wasOnGround: Boolean = false
    private var currentMoveTypeDuration: Int = 0

    override fun onLivingUpdate() {
        if (this.jumpTicks != this.jumpDuration) {
            ++this.jumpTicks
        } else if (this.jumpDuration != 0) {
            this.jumpTicks = 0
            this.jumpDuration = 0
            this.pet.setJumping(false)
        }
    }

    override fun createMoveHelper() = HopMoveHelper(pet)
    override fun createJumpHelper() = HopJumpHelper(pet)

    override fun updateAITasks() {
        if (this.currentMoveTypeDuration > 0) {
            --this.currentMoveTypeDuration
        }

        if (this.pet.onGround) {
            if (!this.wasOnGround) {
                this.pet.setJumping(false)
                this.checkLandingDelay()
            }

            val jumper = this.jumpHelper as HopJumpHelper

            if (!jumper.isJumping) {
                if (this.moveHelper.isUpdating && this.currentMoveTypeDuration == 0) {
                    val path = this.navigator.path
                    var vec3d = Vec3d(this.moveHelper.x, this.moveHelper.y, this.moveHelper.z)

                    if (path != null && path.currentPathIndex < path.currentPathLength) {
                        vec3d = path.getPosition(pet)
                    }

                    this.calculateRotationYaw(vec3d.x, vec3d.z)
                    this.startJumping()
                }
            } else if (!jumper.canJump()) {
                this.enableJumpControl()
            }
        }

        this.wasOnGround = this.pet.onGround
    }

    private fun updateMoveTypeDuration() {
        if (this.pet.moveHelper.speed < 2.2) {
            this.currentMoveTypeDuration = 10
        } else {
            this.currentMoveTypeDuration = 1
        }
    }

    private fun checkLandingDelay() {
        this.updateMoveTypeDuration()
        this.disableJumpControl()
    }

    private fun startJumping() {
        this.pet.setJumping(true)
        this.jumpDuration = 10
        this.jumpTicks = 0
    }

    private fun enableJumpControl() = (this.jumpHelper as HopJumpHelper).setCanJump(true)

    private fun disableJumpControl() = (this.jumpHelper as HopJumpHelper).setCanJump(false)

    private fun calculateRotationYaw(x: Double, z: Double) {
        this.pet.rotationYaw =
                (MathHelper.atan2(z - this.pet.posZ, x - this.pet.posX)
                        * (180.0 / Math.PI)).toFloat() - 90.0f
    }

    override fun getJumpUpwardsMotion(): Float {
        return if (!this.pet.collidedHorizontally && (!this.moveHelper.isUpdating || this.moveHelper.y <= this.pet.posY + 0.5)) {
            val path = this.navigator.path
            if (path != null && path.currentPathIndex < path.currentPathLength) if (path.getPosition(this.pet).y > this.pet.posY + 0.5) return 0.5f
            if (this.moveHelper.speed <= 0.6) 0.2f else 0.3f
        } else 0.5f
    }

    override fun jump() {
        if (this.moveHelper.speed > 0.0) {
            val d1 = this.pet.motionX * this.pet.motionX + this.pet.motionZ * this.pet.motionZ
            if (d1 < 0.010000000000000002) this.pet.moveRelative(0.0f, 0.0f, 1.0f, 0.1f)
        }

        if (!this.pet.world.isRemote) this.pet.world.setEntityState(this.pet, 1.toByte())
    }

    class HopMoveHelper(private val pet: PetEntity) : EntityMoveHelper(pet) {
        private var nextJumpSpeed: Double = 0.0

        fun setMovementSpeed(newSpeed: Double) {
            this.pet.navigator.setSpeed(newSpeed)
            this.pet.moveHelper.setMoveTo(this.pet.moveHelper.x, this.pet.moveHelper.y, this.pet.moveHelper.z, newSpeed)
        }

        override fun onUpdateMoveHelper() {
            if (this.pet.onGround && !this.pet.isJumping && !(this.pet.jumpHelper as HopJumpHelper).isJumping)
                this.setMovementSpeed(0.0)
            else if (this.isUpdating) this.setMovementSpeed(this.nextJumpSpeed)

            super.onUpdateMoveHelper()
        }

        /**
         * Sets the speed and location to move to
         */
        override fun setMoveTo(x: Double, y: Double, z: Double, speedIn: Double) {
            var speed = speedIn
            if (this.pet.isInWater) speed = 1.5
            super.setMoveTo(x, y, z, speed)
            if (speed > 0.0) this.nextJumpSpeed = speed
        }
    }

    inner class HopJumpHelper(private val pet: PetEntity) : EntityJumpHelper(pet) {
        private var canJump: Boolean = false

        val isJumping: Boolean
            get() = super.isJumping

        fun canJump(): Boolean {
            return this.canJump
        }

        fun setCanJump(canJumpIn: Boolean) {
            this.canJump = canJumpIn
        }

        /**
         * Called to actually make the entity jump if isJumping is true.
         */
        override fun doJump() {
            if (super.isJumping) {
                this.startJumping()
                super.isJumping = false
            }
        }

        fun startJumping() {
            this.pet.setJumping(true)
            this@HopHandler.jumpDuration = 10
            this@HopHandler.jumpTicks = 0
        }
    }
}

open class BounceHandler(pet: PetEntity) : PetMovementHandler(pet) {
    var squishAmount: Float = 0f
    var squishFactor: Float = 0f
    var prevSquishFactor: Float = 0f
    private var wasOnGround: Boolean = false

    override fun onLivingUpdate() {
        this.squishFactor += (this.squishAmount - this.squishFactor) * 0.5f
        this.prevSquishFactor = this.squishFactor

        if (this.pet.onGround && !this.wasOnGround) {
            // Spawn particles and play sound
            /*var i = this.getSlimeSize()
            for (j in 0 until i * 8) {
                val f = this.rand.nextFloat() * (Math.PI.toFloat() * 2f)
                val f1 = this.rand.nextFloat() * 0.5f + 0.5f
                val f2 = MathHelper.sin(f) * i.toFloat() * 0.5f * f1
                val f3 = MathHelper.cos(f) * i.toFloat() * 0.5f * f1
                val world = this.world
                val enumparticletypes = this.getParticleType()
                val d0 = this.posX + f2.toDouble()
                val d1 = this.posZ + f3.toDouble()
                world.spawnParticle(enumparticletypes, d0, this.getEntityBoundingBox().minY, d1, 0.0, 0.0, 0.0)
            }

            this.playSound(this.getSquishSound(), this.getSoundVolume(), ((this.rand.nextFloat() - this.rand.nextFloat()) * 0.2f + 1.0f) / 0.8f)*/
            this.squishAmount = -0.5f
        } else if (!this.pet.onGround && this.wasOnGround) {
            this.squishAmount = 1.0f
        }

        this.wasOnGround = this.pet.onGround
        this.squishAmount *= 0.6f
    }

    override fun preRenderCallback(partialTicks: Float) {
        val scale = 1f
        val f2 = (prevSquishFactor + (squishFactor - prevSquishFactor) * partialTicks) / (scale * 0.5f + 1.0f)
        val transform = 1.0f / (f2 + 1.0f)
        GlStateManager.scale(transform * scale, 1.0f / transform * scale, transform * scale)
    }

    override fun createMoveHelper() = BounceMoveHelper(pet)

    override fun registerAI() {
        this.pet.tasks.addTask(1, AISlimeFloat(this.pet))
        this.pet.tasks.addTask(3, AIPetFaceOwner(this.pet))
        this.pet.tasks.addTask(5, AISlimeBounce(this.pet, 5f, 3f))
    }

    class BounceMoveHelper(private val pet: PetEntity) : EntityMoveHelper(pet) {
        private var yRot: Float = 0f
        private var jumpDelay: Int = 0
        private var isAggressive: Boolean = false

        init {
            this.yRot = 180.0f * pet.rotationYaw / Math.PI.toFloat()
        }

        fun setDirection(p_179920_1_: Float, p_179920_2_: Boolean) {
            this.yRot = p_179920_1_
            this.isAggressive = p_179920_2_
        }

        fun setSpeed(speedIn: Double) {
            this.speed = speedIn
            this.action = EntityMoveHelper.Action.MOVE_TO
        }

        override fun onUpdateMoveHelper() {
            this.entity.rotationYaw = this.limitAngle(this.entity.rotationYaw, this.yRot, 90.0f)
            this.entity.rotationYawHead = this.entity.rotationYaw
            this.entity.renderYawOffset = this.entity.rotationYaw

            if (this.action != EntityMoveHelper.Action.MOVE_TO) {
                this.entity.setMoveForward(0.0f)
            } else {
                this.action = EntityMoveHelper.Action.WAIT

                if (this.entity.onGround) {
                    this.entity.aiMoveSpeed =
                            (this.speed * this.entity.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).attributeValue)
                                .toFloat()

                    if (this.jumpDelay-- <= 0) {
                        this.jumpDelay = RNG.nextInt(5) + 2

                        if (this.isAggressive) {
                            this.jumpDelay /= 3
                        }

                        this.pet.jumpHelper.setJumping()

                        /*if (this.pet.makesSoundOnJump()) {
                            this.pet.playSound(this.pet.getJumpSound(), this.pet.getSoundVolume(), ((this.pet.rng.nextFloat() - this.pet.rng.nextFloat()) * 0.2f + 1.0f) * 0.8f)
                        }*/
                    } else {
                        this.pet.moveStrafing = 0.0f
                        this.pet.moveForward = 0.0f
                        this.entity.aiMoveSpeed = 0.0f
                    }
                } else {
                    this.entity.aiMoveSpeed =
                            (this.speed * this.entity.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).attributeValue)
                                .toFloat()
                }
            }
        }
    }
}

open class FloatHandler(pet: PetEntity) : PetMovementHandler(pet) {
    override fun onLivingUpdate() {
        // TODO : implement
    }
}

open class FlapHandler(pet: PetEntity) : PetMovementHandler(pet) {
    var flap: Float = 0f
    var flapSpeed: Float = 0f
    var oFlapSpeed: Float = 0f
    var oFlap: Float = 0f
    var flapping = 1f

    override fun onLivingUpdate() {
        this.oFlap = this.flap
        this.oFlapSpeed = this.flapSpeed
        this.flapSpeed = (this.flapSpeed.toDouble() + (if (this.pet.onGround) -1 else 4).toDouble() * 0.3).toFloat()
        this.flapSpeed = MathHelper.clamp(this.flapSpeed, 0.0f, 1.0f)
        if (!this.pet.onGround && this.flapping < 1.0f) this.flapping = 1.0f
        this.flapping = (this.flapping.toDouble() * 0.9).toFloat()
        if (!this.pet.onGround && this.pet.motionY < 0.0) this.pet.motionY *= 0.6
        this.flap += this.flapping * 2.0f
    }
}