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

import be.bluexin.rpg.BlueRPG
import be.bluexin.rpg.CommonProxy
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.teamwizardry.librarianlib.features.kotlin.associateInPlace
import java.io.File
import java.io.FileReader
import java.io.FileWriter

object RarityConfiguration {
    private lateinit var f: File

    private var rarityWeights =
        TokenType.values().associateInPlace { t -> Rarity.values().associateInPlace { t.rarityWeights.getValue(it) } }
        set(value) {
            for ((type, map) in value) type.rarityWeights = map.withDefault { 0 }
            field = value.mapValues { (_, m) ->
                Rarity.values().associateInPlace { m.getOrDefault(it, 0) }
            }
        }

    private interface TrickingDumbGson : Map<Rarity, Int>

    internal fun preInit() = try {
        f = File(CommonProxy.customConfDir, "rarity_weights.json")
        reload()
    } catch (e: Exception) {
        BlueRPG.LOGGER.warn("Unable to load formulae file", e)
    }

    fun reload() = try {
        val gson = GsonBuilder()
            .setPrettyPrinting()
            .enableComplexMapKeySerialization()
            .create()

        if (!f.exists()) FileWriter(f).use { gson.toJson(rarityWeights, it) }
        else this.rarityWeights = FileReader(f).use {
            gson.fromJson<Map<TokenType, TrickingDumbGson>>(
                it, object : TypeToken<Map<TokenType, TrickingDumbGson>>() {}.type
            )
        }
    } catch (e: Exception) {
        BlueRPG.LOGGER.warn("Unable to load formulae file", e)
    }
}

