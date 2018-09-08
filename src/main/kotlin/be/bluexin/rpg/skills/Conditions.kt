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

import be.bluexin.rpg.util.XoRoRNG
import be.bluexin.saomclib.capabilities.getPartyCapability
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.runBlocking
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.EntityEquipmentSlot
import net.minecraft.item.Item
import net.minecraft.potion.Potion

interface Condition {
    operator fun invoke(caster: EntityLivingBase, target: EntityLivingBase): Boolean
}

data class Random(val chance: Double) : Condition {
    private val rng = produce(capacity = 5) {
        val rng = XoRoRNG()
        while (isActive) send(rng.nextDouble())
    }

    override fun invoke(caster: EntityLivingBase, target: EntityLivingBase): Boolean {
        val r = rng.poll() ?: runBlocking { rng.receive() }
        return r < chance
    }
}

data class RequiresGear(val slot: EntityEquipmentSlot, val item: Item) : Condition {
    override fun invoke(caster: EntityLivingBase, target: EntityLivingBase) = target.getItemStackFromSlot(slot).item == item
}

enum class RequireStatus : Condition {
    SELF {
        override fun invoke(caster: EntityLivingBase, target: EntityLivingBase) = caster == target
    },
    FRIENDLY {
        override fun invoke(caster: EntityLivingBase, target: EntityLivingBase): Boolean {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    },
    AGGRESSIVE {
        override fun invoke(caster: EntityLivingBase, target: EntityLivingBase): Boolean {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    },
    PARTY {
        override fun invoke(caster: EntityLivingBase, target: EntityLivingBase): Boolean {
            return caster is EntityPlayer && target is EntityPlayer &&
                    caster.getPartyCapability().party?.contains(target) ?: false
        }
    };
}

data class RequirePotion(val effect: Potion, val level: Int = 0, val invert: Boolean = false) : Condition {
    override fun invoke(caster: EntityLivingBase, target: EntityLivingBase) = target.getActivePotionEffect(effect)?.amplifier ?: -1 >= level == !invert
}