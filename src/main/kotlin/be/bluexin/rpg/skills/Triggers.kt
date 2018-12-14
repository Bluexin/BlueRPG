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

@Savable
@NamedDynamic("tr:t")
interface Trigger {
    fun startUsing(entity: EntityLivingBase): Boolean
    fun stopUsing(entity: EntityLivingBase, time: Int): Boolean
    val castTimeTicks: Int
}

@Savable
@NamedDynamic("tr:c")
data class Use(override val castTimeTicks: Int) : Trigger {
    override fun startUsing(entity: EntityLivingBase) = this.castTimeTicks == 0

    override fun stopUsing(entity: EntityLivingBase, time: Int) = this.castTimeTicks != 0 && time >= this.castTimeTicks
}