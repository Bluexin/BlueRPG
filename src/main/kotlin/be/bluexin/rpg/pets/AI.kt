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

package be.bluexin.rpg.pets


import net.minecraft.block.material.Material
import net.minecraft.block.state.BlockFaceShape
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.ai.EntityAIBase
import net.minecraft.init.MobEffects
import net.minecraft.pathfinding.PathNavigate
import net.minecraft.pathfinding.PathNavigateFlying
import net.minecraft.pathfinding.PathNavigateGround
import net.minecraft.pathfinding.PathNodeType
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.MathHelper
import net.minecraft.world.World

open class EntityAIFollowOwner(
    private val pet: EntityPet,
    private val followSpeed: Double,
    protected var minDist: Float,
    protected var maxDist: Float
) : EntityAIBase() {
    private var owner: EntityLivingBase? = null
    protected var world: World = pet.world
    private val petPathfinder: PathNavigate = pet.navigator
    private var timeToRecalcPath: Int = 0
    private var oldWaterCost: Float = 0.toFloat()

    init {
        this.mutexBits = 3

        if (pet.navigator !is PathNavigateGround && pet.navigator !is PathNavigateFlying) {
            throw IllegalArgumentException("Unsupported mob type for FollowOwnerGoal")
        }
    }

    /**
     * Returns whether the EntityAIBase should begin execution.
     */
    override fun shouldExecute(): Boolean {
        val owner = this.pet.owner

        return when {
            owner == null -> false
            owner.isSpectator -> false
            this.pet.getDistanceSq(owner) < (this.minDist * this.minDist).toDouble() -> false
            else -> {
                this.owner = owner
                true
            }
        }
    }

    /**
     * Returns whether an in-progress EntityAIBase should continue executing
     */
    override fun shouldContinueExecuting(): Boolean {
        return !this.petPathfinder.noPath() && this.owner!!.getDistanceSq(this.pet) > this.maxDist * this.maxDist
    }

    /**
     * Execute a one shot task or start executing a continuous task
     */
    override fun startExecuting() {
        this.timeToRecalcPath = 0
        this.oldWaterCost = this.pet.getPathPriority(PathNodeType.WATER)
        this.pet.setPathPriority(PathNodeType.WATER, 0.0f)
    }

    /**
     * Reset the task's internal state. Called when this task is interrupted by another one
     */
    override fun resetTask() {
        this.owner = null
        this.petPathfinder.clearPath()
        this.pet.setPathPriority(PathNodeType.WATER, this.oldWaterCost)
    }

    /**
     * Keep ticking a continuous task that has already been started
     */
    override fun updateTask() {
        val owner = this.owner
        if (owner != null && --this.timeToRecalcPath <= 0) {
            this.timeToRecalcPath = 10

            if (!this.petPathfinder.tryMoveToEntityLiving(owner, this.followSpeed)) {
                if (!this.pet.leashed && !this.pet.isRiding) {
                    if (this.pet.getDistanceSq(owner) >= 144.0) {
                        val i = MathHelper.floor(owner.posX) - 2
                        val j = MathHelper.floor(owner.posZ) - 2
                        val k = MathHelper.floor(owner.entityBoundingBox.minY)

                        repeat(5) { l ->
                            repeat(5) { i1 ->
                                if ((l < 1 || i1 < 1 || l > 3 || i1 > 3)
                                    && this.isTeleportFriendlyBlock(i, j, k, l, i1)
                                ) {
                                    this.pet.setLocationAndAngles(
                                        ((i + l).toFloat() + 0.5f).toDouble(),
                                        k.toDouble(),
                                        ((j + i1).toFloat() + 0.5f).toDouble(),
                                        this.pet.rotationYaw,
                                        this.pet.rotationPitch
                                    )
                                    this.petPathfinder.clearPath()
                                    return
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    protected open fun isTeleportFriendlyBlock(x: Int, z: Int, y: Int, xOffset: Int, zOffset: Int): Boolean {
        val blockpos = BlockPos(x + xOffset, y - 1, z + zOffset)
        val iblockstate = this.world.getBlockState(blockpos)
        return iblockstate.getBlockFaceShape(this.world, blockpos, EnumFacing.DOWN) == BlockFaceShape.SOLID
                && iblockstate.canEntitySpawn(this.pet)
                && this.world.isAirBlock(blockpos.up())
                && this.world.isAirBlock(blockpos.up(2))
    }
}

open class EntityAIFollowOwnerFlying(entityPet: EntityPet, followSpeedIn: Double, minDistIn: Float, maxDistIn: Float) :
    EntityAIFollowOwner(entityPet, followSpeedIn, minDistIn, maxDistIn) {

    override fun isTeleportFriendlyBlock(x: Int, z: Int, y: Int, xOffset: Int, zOffset: Int): Boolean {
        val pos = BlockPos(x + xOffset, y - 1, z + zOffset)
        val iblockstate = this.world.getBlockState(pos)
        return (iblockstate.isSideSolid(this.world, pos, EnumFacing.UP)
                || iblockstate.material === Material.LEAVES)
                && this.world.isAirBlock(BlockPos(x + xOffset, y, z + zOffset))
                && this.world.isAirBlock(BlockPos(x + xOffset, y + 1, z + zOffset))
    }
}

internal class AISlimeFloat(private val pet: EntityPet) : EntityAIBase() {

    init {
        this.mutexBits = 5
        (pet.navigator as PathNavigateGround).canSwim = true
    }

    /**
     * Returns whether the EntityAIBase should begin execution.
     */
    override fun shouldExecute(): Boolean {
        return this.pet.isInWater || this.pet.isInLava
    }

    /**
     * Keep ticking a continuous task that has already been started
     */
    override fun updateTask() {
        if (this.pet.rng.nextFloat() < 0.8f) {
            this.pet.jumpHelper.setJumping()
        }

        (this.pet.moveHelper as BounceHandler.BounceMoveHelper).speed = 1.2
    }
}

internal class AISlimeHop(private val pet: EntityPet) : EntityAIBase() {

    init {
        this.mutexBits = 5
    }

    /**
     * Returns whether the EntityAIBase should begin execution.
     */
    override fun shouldExecute() = true

    /**
     * Keep ticking a continuous task that has already been started
     */
    override fun updateTask() {
        (this.pet.moveHelper as BounceHandler.BounceMoveHelper).speed = 1.0
    }
}

internal class AIPetFaceOwner(private val pet: EntityPet) : EntityAIBase() {
    init {
        this.mutexBits = 2
    }

    /**
     * Returns whether the EntityAIBase should begin execution.
     */
    override fun shouldExecute(): Boolean {
        return this.pet.attackTarget == null
                && (this.pet.onGround
                || this.pet.isInWater
                || this.pet.isInLava
                || this.pet.isPotionActive(MobEffects.LEVITATION)
                )
    }

    /**
     * Keep ticking a continuous task that has already been started
     */
    override fun updateTask() {
        this.pet.faceEntity(this.pet.owner!!, 10.0f, 10.0f)
        (this.pet.moveHelper as BounceHandler.BounceMoveHelper).setDirection(this.pet.rotationYaw, false)
    }
}