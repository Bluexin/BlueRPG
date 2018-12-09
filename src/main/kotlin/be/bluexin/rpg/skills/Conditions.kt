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

import com.teamwizardry.librarianlib.features.saving.NamedDynamic
import com.teamwizardry.librarianlib.features.saving.Savable
import net.minecraft.entity.EntityLivingBase
import net.minecraft.inventory.EntityEquipmentSlot
import net.minecraft.item.Item
import net.minecraft.potion.Potion
import kotlin.random.Random

@Savable
@NamedDynamic("t:t")
interface Condition {
    operator fun invoke(caster: EntityLivingBase, target: Target): Boolean
}

@Savable
@NamedDynamic("t:l")
object IsLiving : Condition {
    override fun invoke(caster: EntityLivingBase, target: Target) = target.it is EntityLivingBase
}

@Savable
@NamedDynamic("t:h")
object HasPosition : Condition {
    override fun invoke(caster: EntityLivingBase, target: Target) = target is TargetWithPosition
}

@Savable
@NamedDynamic("t:m")
data class MultiCondition(val c1: Condition, val c2: Condition, val mode: LinkMode) :
    Condition {
    override fun invoke(caster: EntityLivingBase, target: Target) = when (mode) {
        MultiCondition.LinkMode.AND -> c1(caster, target) && c2(caster, target)
        MultiCondition.LinkMode.OR -> c1(caster, target) || c2(caster, target)
        MultiCondition.LinkMode.XOR -> c1(caster, target) xor c2(caster, target)
    }

    enum class LinkMode {
        AND,
        OR,
        XOR
    }
}

@Savable
@NamedDynamic("t:i")
data class Inverted(val c1: Condition) : Condition {
    override fun invoke(caster: EntityLivingBase, target: Target) = !c1(caster, target)
}

@Savable
@NamedDynamic("t:r")
data class Random(val chance: () -> Double) : Condition {
    private val rng = Random(Random.nextLong())

    override fun invoke(caster: EntityLivingBase, target: Target) = rng.nextDouble() < chance()
}

@Savable
@NamedDynamic("t:g")
data class RequireGear(val slot: EntityEquipmentSlot, val item: Item) : Condition {
    override fun invoke(caster: EntityLivingBase, target: Target) =
        target is TargetWithGear && target.getItemStackFromSlot(slot).item == item
}

@Savable
@NamedDynamic("t:s")
data class RequireStatus(val status: Status) : Condition {
    override fun invoke(caster: EntityLivingBase, target: Target) =
        target is TargetWithStatus && target.isStatusFor(status, caster.holder)
}

@Savable
@NamedDynamic("t:p")
data class RequirePotion(val effect: Potion, val level: Int = 0) : Condition {
    override fun invoke(caster: EntityLivingBase, target: Target) =
        target is TargetWithEffects && target.getPotionEffect(effect)?.amplifier ?: -1 >= level
}