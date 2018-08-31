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

package be.bluexin.rpg.util

/**
 * A port of Blackman and Vigna's xoroshiro 128+ generator; should be very fast and produce high-quality output.
 * Testing shows it is within 5% the speed of LightRNG, sometimes faster and sometimes slower, and has a larger period.
 * It's called XoRo because it involves Xor as well as Rotate operations on the 128-bit pseudo-random state.
 * <br>
 * [LightRNG] is also very fast, but relative to XoRoRNG it has a significantly shorter period (the amount of
 * random numbers it will go through before repeating), at {@code pow(2, 64)} as opposed to XorRNG and XoRoRNG's
 * {@code pow(2, 128) - 1}, but LightRNG also allows the current RNG state to be retrieved and altered with
 * {@code getState()} and {@code setState()}. For most cases, you should decide between LightRNG, XoRoRNG, and other
 * RandomnessSource implementations based on your needs for period length and state manipulation (LightRNG is also used
 * internally by almost all StatefulRNG objects). You might want significantly less predictable random results, which
 * [IsaacRNG] and [Isaac32RNG] can provide, along with a large period. You may want a very long period of
 * random numbers, which would suggest [LongPeriodRNG] as a good choice. You may want better performance on
 * 32-bit machines, which would mean [PintRNG] (for generating only ints via [PintRNG#next(int)], since
 * its [PintRNG#nextLong()] method is very slow) or [FlapRNG] (for generating ints and longs at very good
 * speed using mainly int math). You shouldn't rely on 32-bit RandomnessSources on GWT, though, because 32-bit overflow
 * behaves differently there than desktop or Android, and results will change. [LapRNG] is the fastest generator
 * we have, but has a poor period for its state size, and much worse quality than this generator.
 * <br>
 * Original version at http://xoroshiro.di.unimi.it/xoroshiro128plus.c
 * Written in 2016 by David Blackman and Sebastiano Vigna (vigna@acm.org)
 *
 * Translated to Kotlin by Bluexin
 *
 * @author Sebastiano Vigna
 * @author David Blackman
 * @author Tommy Ettinger
 */
class XoRoRNG @JvmOverloads constructor(seed: Long = (Math.random() * java.lang.Long.MAX_VALUE).toLong()) {

    private var state0: Long = 0
    private var state1: Long = 0
    var lastSeed: Long = seed
        private set

    init {
        setSeed(seed)
    }

    fun next(bits: Int): Int {
        return (nextLong() and (1L shl bits) - 1).toInt()
    }

    fun nextLong(): Long {
        val s0 = state0
        var s1 = state1
        val result = s0 + s1

        s1 = s1 xor s0
        state0 = s0 shl 55 or (s0 ushr -55) xor s1 xor (s1 shl 14) // a, b
        state1 = s1 shl 36 or (s1 ushr -36) // c
        /*
        state0 = Long.rotateLeft(s0, 55) ^ s1 ^ (s1 << 14); // a, b
        state1 = Long.rotateLeft(s1, 36); // c
        */
        return result
    }

    /**
     * Produces a copy of this RandomnessSource that, if next() and/or nextLong() are called on this object and the
     * copy, both will generate the same sequence of random numbers from the point copy() was called. This just needs to
     * copy the state so it isn't shared, usually, and produce a new value with the same exact state.

     * @return a copy of this RandomnessSource
     */
    fun copy(): XoRoRNG {
        val next = XoRoRNG(state0)
        next.state0 = state0
        next.state1 = state1
        return next
    }


    /**
     * Can return any int, positive or negative, of any size permissible in a 32-bit signed integer.
     * @return any int, all 32 bits are random
     */
    fun nextInt(): Int {
        return nextLong().toInt()
    }

    /**
     * Exclusive on the upper bound.  The lower bound is 0.
     * @param bound the upper bound; should be positive
     * *
     * @return a random int less than n and at least equal to 0
     */
    fun nextInt(bound: Int): Int {
        if (bound <= 0) return 0
        val threshold = (0x7fffffff - bound + 1) % bound
        while (true) {
            val bits = (nextLong() and 0x7fffffff).toInt()
            if (bits >= threshold)
                return bits % bound
        }
    }

    /**
     * Inclusive lower, exclusive upper.
     * @param lower the lower bound, inclusive, can be positive or negative
     * *
     * @param upper the upper bound, exclusive, should be positive, must be greater than lower
     * *
     * @return a random int at least equal to lower and less than upper
     */
    fun nextInt(lower: Int, upper: Int): Int {
        if (upper - lower <= 0) throw IllegalArgumentException("Upper bound must be greater than lower bound")
        return lower + nextInt(upper - lower)
    }

    /**
     * Exclusive on the upper bound. The lower bound is 0.
     * @param bound the upper bound; should be positive
     * *
     * @return a random long less than n
     */
    fun nextLong(bound: Long): Long {
        if (bound <= 0) return 0
        val threshold = (0x7fffffffffffffffL - bound + 1) % bound
        while (true) {
            val bits = nextLong() and 0x7fffffffffffffffL
            if (bits >= threshold)
                return bits % bound
        }
    }

    fun nextDouble(): Double {
        return (nextLong() and DOUBLE_MASK) * NORM_53
    }

    fun nextFloat(): Float {
        return ((nextLong() and FLOAT_MASK) * NORM_24).toFloat()
    }

    fun nextBoolean(): Boolean {
        return nextLong() < 0L
    }

    fun nextBytes(bytes: ByteArray) {
        var i = bytes.size
        var n = 0
        while (i != 0) {
            n = Math.min(i, 8)
            var bits = nextLong()
            while (n-- != 0) {
                bytes[--i] = bits.toByte()
                bits = bits ushr 8
            }
        }
    }

    /**
     * Sets the seed of this generator using one long, running that through LightRNG's algorithm twice to get the state.
     * @param seed the number to use as the seed
     */
    fun setSeed(seed: Long) {
        var state = seed + -7046029254386353131L
        var z = state
        z = (z xor (z ushr 30)) * -4658895280553007687L
        z = (z xor (z ushr 27)) * -7723592293110705685L
        state0 = z xor (z ushr 31)
        state += -7046029254386353131L
        z = state
        z = (z xor (z ushr 30)) * -4658895280553007687L
        z = (z xor (z ushr 27)) * -7723592293110705685L
        state1 = z xor (z ushr 31)
        lastSeed = seed
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false

        val xoRoRNG = other as XoRoRNG

        if (state0 != xoRoRNG.state0) return false
        return state1 == xoRoRNG.state1
    }

    override fun hashCode(): Int {
        var result = (state0 xor state0.ushr(32)).toInt()
        result = 31 * result + (state1 xor state1.ushr(32)).toInt()
        return result
    }

    private companion object {
        private const val DOUBLE_MASK = (1L shl 53) - 1
        private const val NORM_53 = 1.0 / (1L shl 53)
        private const val FLOAT_MASK = (1L shl 24) - 1
        private const val NORM_24 = 1.0 / (1L shl 24)

        private const val serialVersionUID = 1018744536171610262L
    }
}
