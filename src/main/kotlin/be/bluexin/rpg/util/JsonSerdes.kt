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

package be.bluexin.rpg.util

import be.bluexin.rpg.BlueRPG
import be.bluexin.rpg.gear.*
import be.bluexin.rpg.stats.*
import com.google.gson.*
import net.minecraft.util.ResourceLocation
import java.lang.reflect.Type

object StatDeserializer : JsonDeserializer<Stat> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Stat? {
        val s = json.asString
        try {
            return PrimaryStat.valueOf(s)
        } catch (_: Exception) {
        }
        try {
            return SecondaryStat.valueOf(s)
        } catch (_: Exception) {
        }
        try {
            return FixedStat.valueOf(s)
        } catch (_: Exception) {
        }
        try {
            return TrickStat.valueOf(s)
        } catch (_: Exception) {
        }
        BlueRPG.LOGGER.warn("Invalid Stat: $s")
        return null
    }
}

object GearTypeDeserializer : JsonDeserializer<GearType> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): GearType? {
        val s = json.asString
        try {
            return MeleeWeaponType.valueOf(s)
        } catch (_: Exception) {
        }
        try {
            return RangedWeaponType.valueOf(s)
        } catch (_: Exception) {
        }
        try {
            return OffHandType.valueOf(s)
        } catch (_: Exception) {
        }
        try {
            return ArmorType.valueOf(s)
        } catch (_: Exception) {
        }
        BlueRPG.LOGGER.warn("Invalid GearType: $s")
        return null
    }
}

object ResourceLocationSerde : JsonSerializer<ResourceLocation>, JsonDeserializer<ResourceLocation> {
    override fun serialize(src: ResourceLocation, typeOfSrc: Type, context: JsonSerializationContext) =
        JsonPrimitive(src.toString())

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ) = ResourceLocation(json.asString)
}
