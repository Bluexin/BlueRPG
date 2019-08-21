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

package be.bluexin.rpg.extensions

import com.teamwizardry.librarianlib.features.kotlin.withRealDefault
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.util.CombatEntry
import net.minecraft.util.CombatTracker
import net.minecraft.util.DamageSource
import java.util.*

class RPGCombatTracker(fighterIn: EntityLivingBase) : CombatTracker(fighterIn) {
    private val output = hashMapOf<Entity, MutableList<CombatEntry>>().withRealDefault { LinkedList() }

    override fun trackDamage(damageSrc: DamageSource, healthIn: Float, damageAmount: Float) {
        super.trackDamage(damageSrc, healthIn, damageAmount)
        ((damageSrc.trueSource as? EntityLivingBase ?: return).combatTracker as RPGCombatTracker).trackOutput(
            this.fighter, CombatEntry(
                damageSrc, this.fighter.ticksExisted, healthIn, damageAmount, this.fallSuffix, this.fighter.fallDistance
            )
        )
    }

    private fun trackOutput(target: Entity, entry: CombatEntry) {
        this.output[target].plusAssign(entry)
        this.enterCombat()
    }

    fun enterCombat() {
        this.lastDamageTime = this.fighter.ticksExisted
        if (!this.inCombat) {
            this.inCombat = true
            this.combatStartTime = this.fighter.ticksExisted
            this.combatEndTime = this.combatStartTime
            this.fighter.sendEnterCombat()
        }
    }

    override fun reset() {
        val i = if (this.inCombat) 300 else 100

        if (!this.fighter.isEntityAlive || this.fighter.ticksExisted - this.lastDamageTime > i) {
            val flag = this.inCombat
            this.takingDamage = false
            this.inCombat = false
            this.combatEndTime = this.fighter.ticksExisted

            if (flag) this.fighter.sendEndCombat()

            this.combatEntries.clear()
            this.output.clear()
        }
    }
}
