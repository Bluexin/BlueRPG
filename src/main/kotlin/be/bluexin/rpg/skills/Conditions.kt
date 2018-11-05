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
import com.teamwizardry.librarianlib.features.saving.NamedDynamic
import com.teamwizardry.librarianlib.features.saving.Savable
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.runBlocking
import net.minecraft.entity.EntityLivingBase
import net.minecraft.inventory.EntityEquipmentSlot
import net.minecraft.item.Item
import net.minecraft.potion.Potion

@Savable
@NamedDynamic("t:t")
interface Condition<TARGET : Target> {
    operator fun invoke(caster: EntityLivingBase, target: TARGET): Boolean
}

@Savable
@NamedDynamic("t:l")
object IsLiving : Condition<Target> {
    override fun invoke(caster: EntityLivingBase, target: Target) = target.it is EntityLivingBase

    @Suppress("UNCHECKED_CAST")
    val living
        get() = this as Condition<LivingHolder<*>>
}

@Savable
@NamedDynamic("t:h")
object HasPosition : Condition<Target> {
    override fun invoke(caster: EntityLivingBase, target: Target) = target is TargetWithPosition
}

@Savable
@NamedDynamic("t:m")
data class MultiCondition<TARGET : Target>(val c1: Condition<TARGET>, val c2: Condition<TARGET>, val mode: LinkMode) :
    Condition<TARGET> {
    override fun invoke(caster: EntityLivingBase, target: TARGET) = when (mode) {
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
data class Inverted<TARGET : Target>(val c1: Condition<TARGET>) : Condition<TARGET> {
    override fun invoke(caster: EntityLivingBase, target: TARGET) = !c1(caster, target)
}

@Savable
@NamedDynamic("t:r")
data class Random<TARGET : Target>(val chance: Double) : Condition<TARGET> {
    private val rng = produce(capacity = 5) {
        val rng = XoRoRNG()
        while (isActive) send(rng.nextDouble())
    }

    override fun invoke(caster: EntityLivingBase, target: TARGET): Boolean {
        val r = rng.poll() ?: runBlocking { rng.receive() }
        return r < chance
    }
}

@Savable
@NamedDynamic("t:g")
data class RequireGear<TARGET : TargetWithGear>(val slot: EntityEquipmentSlot, val item: Item) : Condition<TARGET> {
    override fun invoke(caster: EntityLivingBase, target: TARGET) = target.getItemStackFromSlot(slot).item == item
}

@Savable
@NamedDynamic("t:s")
data class RequireStatus<TARGET : TargetWithStatus>(val status: Status) : Condition<TARGET> {
    override fun invoke(caster: EntityLivingBase, target: TARGET) = target.isStatusFor(status, caster.holder)
}

@Savable
@NamedDynamic("t:p")
data class RequirePotion<TARGET : TargetWithEffects>(val effect: Potion, val level: Int = 0) : Condition<TARGET> {
    override fun invoke(caster: EntityLivingBase, target: TARGET) =
        target.getPotionEffect(effect)?.amplifier ?: -1 >= level
}