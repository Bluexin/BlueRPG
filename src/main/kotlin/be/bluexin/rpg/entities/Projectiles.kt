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

import com.teamwizardry.librarianlib.features.base.entity.ArrowEntityMod
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.projectile.EntityArrow
import net.minecraft.item.ItemStack
import net.minecraft.world.World

class EntityRpgArrow : ArrowEntityMod {

    var initialX = 0.0
    var initialY = 0.0
    var initialZ = 0.0

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

        if (!world.isRemote && this.getDistanceSq(initialX, initialY, initialZ) > 200) {
            this.setDead()
            world.removeEntity(this)
        }
    }
}