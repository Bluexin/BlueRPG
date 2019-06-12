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

package be.bluexin.rpg.stats

import be.bluexin.rpg.skills.ApplyBuff
import com.teamwizardry.librarianlib.core.common.RegistrationHandler
import com.teamwizardry.librarianlib.features.helpers.VariantHelper
import com.teamwizardry.librarianlib.features.helpers.currentModId
import net.minecraft.potion.Potion
import net.minecraft.potion.PotionEffect
import net.minecraft.util.ResourceLocation

/**
 * The mathematical behavior is as follows:
 *  - Operation 0: Increment X by Amount
 *  - Operation 1: Increment Y by X * Amount
 *  - Operation 2: Y = Y * (1 + Amount) (equivalent to Increment Y by Y * Amount)
 * The game first sets X = Base, then executes all Operation 0 modifiers, then sets Y = X,
 * then executes all Operation 1 modifiers, and finally executes all Operation 2 modifiers.
 * See https://minecraft.gamepedia.com/Attribute#Operations
 */
enum class Operation {

    /**
     * Operation 0: Additive.
     * Adds all of the modifiers' amounts to the current value of the attribute.
     *
     * For example, modifying an attribute with {Amount:2,Operation:0} and {Amount:4,Operation:0} with a Base of 3 results in 9 (3 + 2 + 4 = 9).
     */
    ADD,

    /**
     * Operation 1: Multiplicative.
     * Multiplies the current value of the attribute by (1 + x), where x is the sum of the modifiers' amounts.
     *
     * For example, modifying an attribute with {Amount:2,Operation:1} and {Amount:4,Operation:1} with a Base of 3 results in 21 (3 * (1 + 2 + 4) = 21).
     */
    MULTIPLY_ADDITIVE,

    /**
     * Operation 2: Multiplicative.
     * For every modifier, multiplies the current value of the attribute by (1 + x), where x is the amount of the particular modifier.
     * Functions the same as Operation 1 if there is only a single modifier with operation 1 or 2.
     * However, for multiple modifiers it will multiply the modifiers rather than adding them.
     *
     * For example, modifying an attribute with {Amount:2,Operation:2} and {Amount:4,Operation:2} with a Base of 3 results in 45 (3 * (1 + 2) * (1 + 4) = 45).
     */
    MULTIPLY_EXPONENTIAL;

    /**
     * Get the vanilla operation
     */
    val op: Int get() = ordinal
}

/**
 * Contains information for buffs, to use with [BuffType]
 */
data class BuffInfo(
    /**
     * The stat to apply on
     */
    val stat: Stat,
    /**
     * A **unique** identifier for this effect.
     * An easy way to generate these is trough [some online tools](https://www.uuidgenerator.net/version4).
     */
    val uuid: String,
    /**
     * Will be multiplied by the effect's level to modify the stat
     */
    val amountPerLevel: Double,
    /**
     * How to affect the stat.
     * @see Operation documentation for more info
     */
    val operation: Operation
)

/**
 * Buff type for use in skills and consumables.
 * Reusing these will prevent overlap, handle with care :)
 */
class BuffType(name: String, vararg stats: BuffInfo) : Potion(false, 2293580) {
    private val modid: String = currentModId

    init {
        RegistrationHandler.register(this, ResourceLocation(modid, VariantHelper.toSnakeCase(name)))
        setPotionName("$modid.potion." + VariantHelper.toSnakeCase(name))

        stats.forEach {
            registerPotionAttributeModifier(it.stat.attribute, it.uuid, it.amountPerLevel, it.operation.op)
        }
    }

    override fun hasStatusIcon() = false
}

/**
 * To use in skills trough [ApplyBuff]
 */
data class Buff(
    /**
     * What [Potion] to use.
     * [BuffType] is a valid subtype.
     */
    val type: Potion,
    /**
     * Duration of this buff, in ticks
     */
    val durationTicks: Int,
    /**
     * Level of this buff.
     * Take care, these start from 0 !
     */
    val amplifier: Int,
    /**
     * Whether this buff comes from ambient effects (like beacons)
     */
    val isAmbient: Boolean = true
) {
    val toVanilla get() = PotionEffect(type, durationTicks, amplifier, isAmbient, false)
}