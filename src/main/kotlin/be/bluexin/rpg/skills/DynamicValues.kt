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

package be.bluexin.rpg.skills

import be.bluexin.rpg.BlueRPG
import be.bluexin.rpg.stats.FixedStat
import be.bluexin.rpg.stats.PrimaryStat
import be.bluexin.rpg.stats.SecondaryStat
import be.bluexin.rpg.stats.Stat
import be.bluexin.rpg.util.RNG
import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import gnu.jel.CompilationException
import gnu.jel.CompiledExpression
import gnu.jel.Evaluator
import gnu.jel.Library
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.random.Random

data class ExpressionData(
    val cacheType: CacheType,
    val expression: String
)

data class Holder<T : Target>(
    @get:JvmName("caster")
    val caster: LivingHolder<*>,
    @get:JvmName("target")
    val target: T
) {
    val rng @JvmName("rng") get() = RNG
}

abstract class Expression<T : Target>(protected val expression: CompiledExpression, val text: String) {
    abstract fun updateCache(holder: Holder<T>)
    abstract val cacheType: CacheType
}

abstract class ObjectExpression<T : Target, Result>(expression: CompiledExpression, text: String) :
    Expression<T>(expression, text) {
    abstract operator fun invoke(holder: Holder<Target>): Result
}

open class DoubleExpression<T : Target>(expression: CompiledExpression, text: String) :
    Expression<T>(expression, text) {
    override fun updateCache(holder: Holder<T>) = Unit
    open operator fun invoke(holder: Holder<T>): Double = expression.evaluate_double(arrayOf(holder))
    override val cacheType get() = CacheType.NONE
}

class StaticDoubleExpression<T : Target>(expression: CompiledExpression, text: String) :
    DoubleExpression<T>(expression, text) {
    private var cache: Double = .0

    override fun updateCache(holder: Holder<T>) {
        this.cache = expression.evaluate_double(arrayOf(holder))
    }

    override fun invoke(holder: Holder<T>): Double = this.cache
    override val cacheType get() = CacheType.STATIC
}

enum class CacheType(
    private val genericProvider: (Class<*>, CompiledExpression, String) -> ObjectExpression<Target, Any>,
    private val doubleProvider: (CompiledExpression, String) -> DoubleExpression<Target>
) {

    /**
     * Values will be cached whenever they're first queried, and never updated
     */
    STATIC({ _, _, _ -> TODO("Not implemented") }, ::StaticDoubleExpression),

    /**
     * Values will not be cached
     */
    NONE({ _, _, _ -> TODO("Not implemented") }, ::DoubleExpression);

    // TODO: per cast cache?

    @Suppress("UNCHECKED_CAST")
    fun <T : Target, Result> cacheExpression(expr: CompiledExpression, text: String, clazz: Class<Result>) =
        genericProvider(clazz, expr, text) as ObjectExpression<T, Result>

    @Suppress("UNCHECKED_CAST")
    fun <T : Target> cacheDoubleExpression(expr: CompiledExpression, text: String) =
        doubleProvider(expr, text) as DoubleExpression<T>
}

object LibHelper {
    fun <T : Target> compileDouble(v: ExpressionData) = try {
        v.cacheType.cacheDoubleExpression<T>(
            Evaluator.compile(v.expression, LIB, java.lang.Double.TYPE), v.expression
        )
    } catch (ce: CompilationException) {
        val sb = StringBuilder("An error occurred during skill loading. See more info below.\n")
            .append("–––COMPILATION ERROR :\n")
            .append(ce.message).append('\n')
            .append("                       ")
            .append(v.expression).append('\n')
        val column = ce.column // Column, where error was found
        for (i in 0 until column + 23 - 1) sb.append(' ')
        sb.append('^')
        val w = StringWriter()
        ce.printStackTrace(PrintWriter(w))
        sb.append('\n').append(w)
        BlueRPG.LOGGER.fatal(sb.toString())
        throw IllegalStateException()
    } catch (e: Exception) {
        val w = StringWriter()
        e.printStackTrace(PrintWriter(w))
        BlueRPG.LOGGER.fatal("An error occurred while compiling '${v.expression}'.\n$w")
        throw IllegalStateException()
    }

    private val LIB: Library by lazy {
        val staticLib = arrayOf(
            PrimaryStat::class.java,
            SecondaryStat::class.java,
            FixedStat::class.java,
            MultiCondition.LinkMode::class.java,
            Status::class.java
        )
        val dynLib = arrayOf(Holder::class.java)
        val dotClasses = arrayOf(
            String::class.java,
            Stat::class.java,
            List::class.java,
            PlayerHolder::class.java,
            LivingHolder::class.java,
            DefaultHolder::class.java,
            PosHolder::class.java,
            BlockPosHolder::class.java,
            WorldPosHolder::class.java,
            Random::class.java
        )
        Library(staticLib, dynLib, dotClasses, null, null)
    }
}

object ExpressionAdapterFactory : TypeAdapterFactory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any?> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? =
        if (DoubleExpression::class.java.isAssignableFrom(type.rawType)) DoubleExpressionAdapter as TypeAdapter<T>
        else null
}

object DoubleExpressionAdapter : TypeAdapter<DoubleExpression<Target>>() {
    private val dataAdapter by lazy { Gson().getAdapter(ExpressionData::class.java) }

    override fun write(out: JsonWriter, value: DoubleExpression<Target>?) {
        if (value == null) {
            out.nullValue()
            return
        }
        dataAdapter.write(out, ExpressionData(value.cacheType, value.text))
    }

    override fun read(reader: JsonReader): DoubleExpression<Target>? {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            return null
        }
        return LibHelper.compileDouble(dataAdapter.read(reader))
    }
}
