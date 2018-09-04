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

package be.bluexin.rpg.stats

import be.bluexin.rpg.BlueRPG
import be.bluexin.rpg.gear.*
import be.bluexin.rpg.util.Roll
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import gnu.jel.CompilationException
import gnu.jel.Evaluator
import gnu.jel.Library
import net.minecraft.inventory.EntityEquipmentSlot
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import java.io.*
import java.lang.reflect.Type


object FormulaeConfiguration {

    operator fun invoke(stat: Stat, ilvl: Int, rarity: Rarity, gearType: GearType, slot: EntityEquipmentSlot): Roll {
        val (minE, maxE) = statFormulae[if (gearType is ArmorType) Key(gearType, slot) else Key(gearType)]?.get(stat)
                ?: return Roll(-2, -1)
        val generatorOptions = GeneratorOptions(ilvl, rarity)
        val min = minE(generatorOptions)
        val max = maxE(generatorOptions)
        return Roll(min, max)
    }

    private data class TrickingDumbGsonRead(val key: Key, val formulae: Map<Stat, Range>)
    private data class TrickingDumbGsonWrite(val key: Key, val formulae: Map<Stat, FalseRoll>)
    private data class FalseRoll(val min: String, val max: String)

    private data class Key(val type: GearType, val slot: EntityEquipmentSlot? = null)
    private data class Range(val min: ExpressionWrapper, val max: ExpressionWrapper)

    private val statFormulae = mutableMapOf<Key, MutableMap<Stat, Range>>()

    internal fun preInit(event: FMLPreInitializationEvent) = try {
        val dir = File(event.modConfigurationDirectory, BlueRPG.MODID)
        if (!dir.exists()) dir.mkdir()
        if (!dir.isDirectory) throw IllegalStateException("$dir exists and is not a directory")

        val gson = GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Stat::class.java, StatDeserializer)
                .registerTypeAdapter(GearType::class.java, GearTypeDeserializer)
                .registerTypeAdapter(ExpressionWrapper::class.java, ExpressionDeserializer)
                .create()

        val f = File(dir, "stat_formulae.json")
        if (!f.exists()) {
            var s = MeleeWeaponType.values().asSequence().map { Key(it) } +
                    RangedWeaponType.values().asSequence().map { Key(it) } +
                    OffHandType.values().asSequence().map { Key(it) }
            ArmorType.values().forEach { t ->
                s += EntityEquipmentSlot.values().asSequence().filter { it.index == it.slotIndex - 1 }.map { Key(t, it) }
            }
            val r = FalseRoll(
                    "(int) ((4 + pow(ilvl, 1.5) / 6) * pow(rarity.ordinal() / 11.0 + 1, 1.8))",
                    "(int) ((8 + pow(ilvl, 1.5) / 4) * pow(rarity.ordinal() / 9.0 + 1, 1.8))"
            )
            val m = (PrimaryStat.values().asSequence<Stat>() +
                    SecondaryStat.values().asSequence() +
                    FixedStat.values().asSequence()).map { it to r }.toMap()
            val a = s.map { TrickingDumbGsonWrite(it, m) }
            FileWriter(f).use { gson.toJson(a.toList(), it) }
        }
        val read = FileReader(f).use { gson.fromJson<List<TrickingDumbGsonRead>>(it, object : TypeToken<List<TrickingDumbGsonRead>>() {}.type) }
        read.forEach { (key, map) ->
            val o = statFormulae[key]
            if (o != null) o += map
            else statFormulae[key] = map.toMutableMap()
        }
    } catch (e: Exception) {
        BlueRPG.LOGGER.warn("Unable to load formulae file", e)
    }

    private object StatDeserializer : JsonDeserializer<Stat> {
        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Stat {
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
            throw IllegalArgumentException("Invalid Stat: $s")
        }
    }

    private object GearTypeDeserializer : JsonDeserializer<GearType> {
        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): GearType {
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
            throw IllegalArgumentException("Invalid GearType: $s")
        }
    }

    private object ExpressionDeserializer : JsonDeserializer<ExpressionWrapper> {
        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext) =
                ExpressionWrapper(json.asString)
    }

    @Suppress("unused")
    internal class GeneratorOptions(@JvmField val ilvl: Int, @JvmField val rarity: Rarity)

    private class ExpressionWrapper(expression: String) {

        private val expression = try {
            Evaluator.compile(expression, LIB, Integer.TYPE)!!
        } catch (e: CompilationException) {
            val sb = StringBuilder("An error occurred during theme loading. See more info below.\n")
                    .append("–––COMPILATION ERROR :\n")
                    .append(e.message).append('\n')
                    .append("                       ")
                    .append(expression).append('\n')
            val column = e.column
            for (i in 0 until column + 23 - 1) sb.append(' ')
            sb.append('^')
            val w = StringWriter()
            e.printStackTrace(PrintWriter(w))
            sb.append('\n').append(w)
            BlueRPG.LOGGER.warn(sb.toString())
            Evaluator.compile("-1", LIB, Integer.TYPE)!!
        }

        operator fun invoke(p1: GeneratorOptions): Int {
            return expression.evaluate_int(arrayOf(p1))
        }
    }

    private val LIB: Library by lazy {
        val staticLib = arrayOf(Rarity::class.java, StrictMath::class.java)
        val dynLib = arrayOf(GeneratorOptions::class.java)
        val dotClasses = arrayOf(Rarity::class.java)
        Library(staticLib, dynLib, dotClasses, null, null)
    }
}