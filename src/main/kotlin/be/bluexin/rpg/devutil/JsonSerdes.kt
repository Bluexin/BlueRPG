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
import be.bluexin.rpg.gear.*
import be.bluexin.rpg.stats.*
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import com.teamwizardry.librarianlib.features.saving.Dyn
import com.teamwizardry.librarianlib.features.saving.NamedDynamic
import com.teamwizardry.librarianlib.features.saving.serializers.builtin.core.NamedDynamicRegistryManager
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

object DynamicTypeAdapterFactory : TypeAdapterFactory {
    override fun <T : Any?> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        val clazz = type.rawType
        val nd = clazz.getAnnotation(NamedDynamic::class.java)
        val (key, resolver) = when {
            nd != null -> {
                val regs = NamedDynamicRegistryManager.getRegistries(clazz)
                nd.resourceLocation to { key: String ->
                    @Suppress("UNCHECKED_CAST")
                    gson.getDelegateAdapter<T>(
                        this,
                        TypeToken.get(regs.asSequence().mapNotNull { it.get(key) }.single()) as TypeToken<T>
                    )
                }
            }
            clazz.isAnnotationPresent(Dyn::class.java) -> {
                clazz.canonicalName to { key: String ->
                    @Suppress("UNCHECKED_CAST")
                    gson.getDelegateAdapter<T>(
                        this,
                        TypeToken.get(Class.forName(key)) as TypeToken<T>?
                    )
                }
            }
            else -> return null
        }

        return DynamicTypeAdapter(gson, gson.getDelegateAdapter(this, type), key, resolver)
    }

    private class DynamicTypeAdapter<T : Any?>(
        private val gson: Gson,
        private val writeAdapter: TypeAdapter<T>,
        private val key: String,
        private val readAdapter: (String) -> TypeAdapter<T>
    ) : TypeAdapter<T>() {
        override fun write(out: JsonWriter, value: T?) {
            if (value == null) {
                out.nullValue()
                return
            }
            out.beginObject()
                .name("dynamic_key")
                .value(key)
                .name("dynamic_value")
            writeAdapter.write(out, value)
            out.endObject()
        }

        override fun read(reader: JsonReader): T? {
            if (reader.peek() == JsonToken.NULL) {
                reader.nextNull()
                return null
            }
            reader.beginObject()
            var key: String? = null
            var value: JsonElement? = null
            repeat(2) {
                when (reader.nextName()) {
                    "dynamic_key" -> key = reader.nextString()
                    "dynamic_value" -> value = gson.getAdapter(JsonElement::class.java).read(reader)
                }
            }
            reader.endObject()
            return readAdapter(key!!).fromJsonTree(value)
        }
    }

}
