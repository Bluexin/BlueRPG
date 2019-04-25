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

package be.bluexin.rpg.gear

import be.bluexin.rpg.skills.glitter.RaritySystem.renderParticles
import be.bluexin.saomclib.onClient
import com.teamwizardry.librarianlib.features.base.entity.ItemEntityMod
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityItem
import net.minecraft.item.ItemStack
import net.minecraft.world.World

class RPGItemEntity : ItemEntityMod {

    @Suppress("unused")
    constructor(world: World) : super(world)

    constructor(world: World, x: Double, y: Double, z: Double, stack: ItemStack) : super(world, x, y, z, stack)
    constructor(world: World, oldEntity: Entity, stack: ItemStack) : this(
        world,
        oldEntity.posX,
        oldEntity.posY,
        oldEntity.posZ,
        stack
    ) {
        this.motionX = oldEntity.motionX
        this.motionY = oldEntity.motionY
        this.motionZ = oldEntity.motionZ
        if (oldEntity is EntityItem) this.pickupDelay = oldEntity.pickupDelay
    }

    override fun onUpdate() {
        super.onUpdate()

        world onClient {
            renderParticles()
        }
    }
}