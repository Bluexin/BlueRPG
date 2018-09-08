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

import be.bluexin.saomclib.party.IParty
import net.minecraft.inventory.EntityEquipmentSlot
import net.minecraft.item.ItemStack
import net.minecraft.potion.Potion
import net.minecraft.potion.PotionEffect
import net.minecraft.util.DamageSource
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import java.util.*

enum class Status {
    SELF,
    FRIENDLY,
    AGGRESSIVE,
    PARTY;

    operator fun invoke(caster: TargetWithStatus, target: TargetWithStatus): Boolean =
            when (this) {
                SELF -> caster == target
                FRIENDLY -> SELF(caster, target) || PARTY(caster, target)
                AGGRESSIVE -> !FRIENDLY(caster, target) // TODO: this should be improved (check cnpc api)
                PARTY -> {
                    val p = (caster as? TargetWithParty)?.party
                    if (p != null) p == (target as? TargetWithParty)?.party
                    else false
                }
            }
}

interface Target {
    val it: Any
}

interface TargetWithStatus : Target {
    fun isStatusFor(status: Status, other: TargetWithStatus) = status(other, this)
}

interface TargetWithParty : Target {
    val party: IParty?
}

interface TargetWithEffects : Target {
    fun getPotionEffect(effect: Potion): PotionEffect?
    fun addPotionEffect(effect: PotionEffect)
}

interface TargetWithPosition : Target {
    val pos: Vec3d

    val x get() = pos.x
    val y get() = pos.y
    val z get() = pos.z

    fun getDistanceSq(other: TargetWithPosition) = pos.squareDistanceTo(other.pos)
}

interface TargetWithLookVec : Target {
    val lookVec: Vec3d
}

interface TargetWithWorld : Target {
    val world: World
}

interface TargetWithMovement : Target {
    val movement: Vec3d

    val motionX get() = movement.x
    val motionY get() = movement.y
    val motionZ get() = movement.z
}

interface TargetWithCollision : Target {
    val onGround: Boolean
}

interface TargetWithHealth : Target {
    fun attack(source: DamageSource, amount: Float): Boolean
    fun heal(amount: Float)
}

interface TargetWithGear : Target {
    fun getItemStackFromSlot(slot: EntityEquipmentSlot): ItemStack
}

interface TargetWithUuid : Target {
    val uuid: UUID
}
