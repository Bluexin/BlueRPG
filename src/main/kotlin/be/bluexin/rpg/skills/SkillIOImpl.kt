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

import be.bluexin.rpg.stats.Stat
import be.bluexin.rpg.stats.get
import be.bluexin.saomclib.capabilities.getPartyCapability
import com.teamwizardry.librarianlib.features.kotlin.motionVec
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.EntityEquipmentSlot
import net.minecraft.item.ItemStack
import net.minecraft.potion.Potion
import net.minecraft.potion.PotionEffect
import net.minecraft.util.DamageSource
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import java.util.*

open class DefaultHolder<T : Any>(override val it: T) : TargetWithStatus {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DefaultHolder<*>) return false

        if (it != other.it) return false

        return true
    }

    override fun hashCode(): Int {
        return it.hashCode()
    }
}

open class LivingHolder<T : EntityLivingBase>(living: T) :
    DefaultHolder<T>(living), TargetWithEffects, TargetWithLookVec, TargetWithPosition, TargetWithWorld,
    TargetWithMovement, TargetWithCollision, TargetWithHealth, TargetWithUuid, TargetWithGear, TargetWithBoundingBox,
    TargetWithStats {
    override fun getPotionEffect(effect: Potion) = it.getActivePotionEffect(effect)
    override fun addPotionEffect(effect: PotionEffect) = it.addPotionEffect(effect)
    override val pos: Vec3d get() = it.getPositionEyes(1f)
    override val feet: Vec3d get() = it.positionVector
    override val lookVec: Vec3d get() = it.lookVec
    override val yaw get() = it.rotationYaw
    override val pitch get() = it.rotationPitch
    override val world: World get() = it.world
    override var movement: Vec3d
        get() = it.motionVec
        set(value) {
            it.motionVec = value
            it.velocityChanged = true
        }
    override val onGround: Boolean get() = it.onGround
    override fun attack(source: DamageSource, amount: Float) = it.attackEntityFrom(source, amount)
    override fun heal(amount: Float) = it.heal(amount)
    override val health: Float get() = it.health
    override val maxHealth: Float get() = it.maxHealth
    override val uuid: UUID get() = it.persistentID
    override val boundingBox: AxisAlignedBB get() = it.entityBoundingBox

    override fun getItemStackFromSlot(slot: EntityEquipmentSlot): ItemStack = it.getItemStackFromSlot(slot)

    /* Optimizing these */
    override val x: Double
        get() = it.posX
    override val y: Double
        get() = it.posY + it.eyeHeight - 0.10000000149011612
    override val z: Double
        get() = it.posZ
    override var motionX: Double
        get() = it.motionX
        set(value) {
            it.motionX = value
            it.velocityChanged = true
        }
    override var motionY: Double
        get() = it.motionY
        set(value) {
            it.motionY = value
            it.velocityChanged = true
        }
    override var motionZ: Double
        get() = it.motionZ
        set(value) {
            it.motionZ = value
            it.velocityChanged = true
        }

    override fun teleport(x: Double, y: Double, Z: Double) {
        it.attemptTeleport(x, y, z)
    }

    override fun get(stat: Stat) = .0
}

class PlayerHolder(player: EntityPlayer) : LivingHolder<EntityPlayer>(player), TargetWithParty, TargetWithStats {
    override val party get() = it.getPartyCapability().party
    override fun get(stat: Stat) = it[stat]
}

class ProjectileHolder(projectile: SkillProjectileEntity) : DefaultHolder<SkillProjectileEntity>(projectile),
    TargetWithPosition, TargetWithWorld,
    TargetWithMovement, TargetWithCollision, TargetWithBoundingBox {
    override val pos: Vec3d get() = it.positionVector
    override val boundingBox: AxisAlignedBB get() = it.entityBoundingBox
    override val world: World get() = it.world
    override var movement: Vec3d
        get() = it.motionVec
        set(value) {
            it.motionVec = value
            it.velocityChanged = true
        }

    override val onGround: Boolean get() = it.onGround

    /* Optimizing these */
    override val x: Double get() = it.posX
    override val y: Double get() = it.posY
    override val z: Double get() = it.posZ
    override var motionX: Double
        get() = it.motionX
        set(value) {
            it.motionX = value
            it.velocityChanged = true
        }
    override var motionY: Double
        get() = it.motionY
        set(value) {
            it.motionY = value
            it.velocityChanged = true
        }
    override var motionZ: Double
        get() = it.motionZ
        set(value) {
            it.motionZ = value
            it.velocityChanged = true
        }

    override fun teleport(x: Double, y: Double, Z: Double) {
        it.posX = x
        it.posY = y
        it.posZ = z
        // TODO: this will probably not work without some additional sync, idk
    }
}

val EntityLivingBase.holder
    get() = if (this is EntityPlayer) PlayerHolder(this) else LivingHolder(this)

open class PosHolder(pos: Vec3d) : DefaultHolder<Vec3d>(pos), TargetWithPosition {
    override val pos get() = it
}

open class BlockPosHolder(pos: BlockPos) : PosHolder(Vec3d(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5))

open class WorldPosHolder(world: World, pos: Vec3d) : DefaultHolder<Pair<World, Vec3d>>(world to pos),
    TargetWithPosition, TargetWithWorld {
    constructor(world: World, pos: BlockPos) : this(world, Vec3d(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5))

    override val world: World get() = it.first
    override val pos: Vec3d get() = it.second
}