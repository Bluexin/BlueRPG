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

package be.bluexin.rpg.skills

import be.bluexin.saomclib.capabilities.getPartyCapability
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.potion.Potion
import net.minecraft.potion.PotionEffect
import net.minecraft.util.DamageSource
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
    TargetWithMovement, TargetWithCollision, TargetWithHealth, TargetWithUuid {
    override fun getPotionEffect(effect: Potion) = it.getActivePotionEffect(effect)
    override fun addPotionEffect(effect: PotionEffect) = it.addPotionEffect(effect)
    override val pos: Vec3d get() = it.positionVector
    override val lookVec: Vec3d get() = it.lookVec
    override val world: World get() = it.world
    override val movement: Vec3d get() = Vec3d(it.motionX, it.motionY, it.motionZ)
    override val onGround: Boolean get() = it.onGround
    override fun attack(source: DamageSource, amount: Float) = it.attackEntityFrom(source, amount)
    override fun heal(amount: Float) = it.heal(amount)
    override val uuid: UUID get() = it.persistentID

    /* Optimizing these */
    override val x: Double
        get() = it.posX
    override val y: Double
        get() = it.posY + it.eyeHeight - 0.10000000149011612
    override val z: Double
        get() = it.posZ
    override val motionX: Double
        get() = it.motionX
    override val motionY: Double
        get() = it.motionY
    override val motionZ: Double
        get() = it.motionZ
}

class PlayerHolder(player: EntityPlayer) : LivingHolder<EntityPlayer>(player), TargetWithParty {
    override val party get() = it.getPartyCapability().party
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