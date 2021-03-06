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

package be.bluexin.rpg.devutil

import be.bluexin.rpg.BlueRPG
import be.bluexin.rpg.stats.SecondaryStat
import com.teamwizardry.librarianlib.features.helpers.getTagList
import com.teamwizardry.librarianlib.features.kotlin.get
import com.teamwizardry.librarianlib.features.kotlin.set
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.datafix.DataFixer
import net.minecraft.util.datafix.FixTypes
import net.minecraft.util.datafix.IFixableData
import net.minecraftforge.common.util.CompoundDataFixer
import net.minecraftforge.common.util.ModFixs

object BlueRPGDataFixer {
    private const val DATA_VERSION = 5
    private lateinit var fixer: ModFixs

    fun setup(fixer: DataFixer) {
        this.fixer = (fixer as CompoundDataFixer).init(BlueRPG.MODID, DATA_VERSION)

        this.fixer.registerFix(FixTypes.PLAYER, PlayerCaps001())
        this.fixer.registerFix(FixTypes.PLAYER, PlayerClasses001())
        this.fixer.registerFix(FixTypes.ITEM_INSTANCE, GearCaps001())
        this.fixer.registerFix(FixTypes.PLAYER, PlayerBaseStats001())
    }

    private class PlayerCaps001 : IFixableData {
        override fun fixTagCompound(compound: NBTTagCompound): NBTTagCompound {
            this.convertCaps(compound["ForgeCaps"] as? NBTTagCompound ?: return compound, "player_stats", "pet_storage")
            return compound
        }

        private fun convertCaps(caps: NBTTagCompound, vararg keys: String) =
            keys.forEach { convertCap(caps, "${BlueRPG.MODID}:$it") }

        private fun convertCap(caps: NBTTagCompound, key: String) {
            val cap = caps[key] as? NBTTagCompound
            if (cap != null) caps[key] = cap[key] ?: cap
        }

        override fun getFixVersion() = 1
    }

    private class PlayerClasses001 : IFixableData {
        override fun fixTagCompound(compound: NBTTagCompound): NBTTagCompound {
            (compound["ForgeCaps"] as? NBTTagCompound)?.removeTag("bluerpg:player_class")
            return compound
        }

        override fun getFixVersion() = 3
    }

    private class GearCaps001 : IFixableData {
        override fun fixTagCompound(compound: NBTTagCompound): NBTTagCompound {
            (compound["tag"] as? NBTTagCompound)?.removeTag("capabilities")
            return compound
        }

        override fun getFixVersion() = 4
    }

    private class PlayerBaseStats001 : IFixableData {
        override fun fixTagCompound(compound: NBTTagCompound): NBTTagCompound {
            val l = compound.getTagList("Attributes", NBTTagCompound::class.java) ?: return compound

            val toFix = arrayOf(SecondaryStat.REGEN, SecondaryStat.SPIRIT)
            l.forEach {
                it as NBTTagCompound
                val s = toFix.find { s -> s.attribute.name == it.getString("Name") }
                if (s != null) it.setDouble("Base", s.baseValue)
            }

            return compound
        }

        override fun getFixVersion() = 5
    }
}