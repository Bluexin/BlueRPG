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

import be.bluexin.rpg.CommonProxy
import be.bluexin.rpg.utilities.DynamicConfig
import com.google.gson.GsonBuilder
import java.io.File
import java.io.FileReader
import java.io.FileWriter

val config: CustomConfig by lazy {
    val f = File(CommonProxy.customConfDir, "custom_config.json")

    val gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    return@lazy if (f.exists()) FileReader(f).use {
        gson.fromJson(it, CustomConfig::class.java)
    } else CustomConfig().also {
        FileWriter(f).use { fw ->
            gson.toJson(it, fw)
        }
    }
}

data class CustomConfig(
    val dynamicItems: DynamicConfig = DynamicConfig()
)