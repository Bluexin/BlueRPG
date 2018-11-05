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
import be.bluexin.rpg.events.ExperienceChangeEvent
import be.bluexin.rpg.events.LevelUpEvent
import be.bluexin.rpg.util.fire
import be.bluexin.saomclib.capabilities.AbstractCapability
import be.bluexin.saomclib.capabilities.AbstractEntityCapability
import be.bluexin.saomclib.capabilities.Key
import be.bluexin.saomclib.onServer
import com.teamwizardry.librarianlib.features.config.ConfigIntRange
import com.teamwizardry.librarianlib.features.config.ConfigProperty
import com.teamwizardry.librarianlib.features.saving.AbstractSaveHandler
import com.teamwizardry.librarianlib.features.saving.NamedDynamic
import com.teamwizardry.librarianlib.features.saving.Save
import com.teamwizardry.librarianlib.features.saving.SaveInPlace
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTBase
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraft.util.ResourceLocation
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.CapabilityInject
import net.minecraftforge.fml.common.eventhandler.Event
import java.lang.ref.WeakReference
import kotlin.math.max

@SaveInPlace
@NamedDynamic(resourceLocation = "b:ps")
class PlayerStats : AbstractEntityCapability(), StatCapability {

    @Save
    lateinit var level: Level
        internal set

    @Save
    var attributePoints = LEVELUP_ATTRIBUTES * 3
        set(value) {
            field = value
            dirty()
        }

    @Save
    lateinit var baseStats: StatsCollection
        internal set

    operator fun get(stat: Stat) = baseStats[stat]

    override fun setup(param: Any): AbstractCapability {
        super.setup(param)

        val wr = WeakReference(param as EntityPlayer)
        level = Level(wr)
        baseStats = StatsCollection(wr)

        return this
    }

    fun loadFrom(other: PlayerStats) {
        level = other.level.copy()
        attributePoints = other.attributePoints
        baseStats = other.baseStats.copy()
    }

    override fun copy() = PlayerStats().also {
        it.loadFrom(this)
    }

    internal var dirty = false
        get() = field || level.dirty || baseStats.dirty
        private set

    private fun clean() {
        dirty = false
        level.clean()
        baseStats.clean()
    }

    private fun dirty() {
        dirty = true
    }

    override fun sync() {
        super.sync()
        clean()
    }

    internal object Storage : Capability.IStorage<PlayerStats> {
        override fun readNBT(
            capability: Capability<PlayerStats>,
            instance: PlayerStats,
            side: EnumFacing?,
            nbt: NBTBase
        ) {
            val nbtTagCompound = nbt as? NBTTagCompound ?: return
            AbstractSaveHandler.readAutoNBT(instance, nbtTagCompound.getTag(KEY.toString()), false)
        }

        override fun writeNBT(capability: Capability<PlayerStats>, instance: PlayerStats, side: EnumFacing?): NBTBase {
            return NBTTagCompound().also {
                it.setTag(
                    KEY.toString(),
                    AbstractSaveHandler.writeAutoNBT(instance, false)
                )
            }
        }
    }

    companion object {
        @Key
        val KEY = ResourceLocation(BlueRPG.MODID, "player_stats")

        @CapabilityInject(PlayerStats::class)
        lateinit var Capability: Capability<PlayerStats>
            internal set

        const val LEVELUP_ATTRIBUTES = 2
    }
}

@SaveInPlace
class Level(private val player: WeakReference<EntityPlayer>) {

    // TODO: rename these to level and exp once liblib is fixed
    var level_a
        get() = _level
        private set(value) {
            if (value <= LEVEL_CAP && _level != value) {
                val old = _level
                _level = value
                dirty()
                with(player.get()) {
                    if (this != null) {
                        this.world onServer {
                            this.stats.attributePoints += PlayerStats.LEVELUP_ATTRIBUTES * (value - old)
                        }
                        fire(LevelUpEvent(this, old, value))
                    }
                }
            }
        }

    @Save("level")
    private var _level = 1

    var exp_a
        get() = _exp
        private set(value) {
            if (value == _exp) return
            val actualValue = max(value, 0)
            val evt = with(player.get()) {
                return@with if (this != null) ExperienceChangeEvent(this, _exp, actualValue) else null
            }
            if (evt != null && fire(evt) && evt.result != Event.Result.DENY) {
                _exp = evt.newValue
                dirty()
                checkLevelup()
            }
        }

    @Save("exp")
    private var _exp = 0L

    var toNext: Long = 100
        get() = if (level_a < LEVEL_CAP) {
            if (lastComputedLevel == level_a) field
            else {
                field = FormulaeConfiguration.expToNext(level_a)
                lastComputedLevel = level_a
                field
            }
        } else -1L
        private set

    val progression: Float
        get() = if (level_a == LEVEL_CAP) 1f else exp_a / toNext.toFloat()

    private var lastComputedLevel = 1

    operator fun plusAssign(exp: Long) {
        this.exp_a += exp
    }

    private fun checkLevelup() {
        val toNext = this.toNext
        while (level_a < LEVEL_CAP && exp_a >= toNext) {
            _exp -= toNext
            ++level_a
        }
    }

    internal var dirty = false
        private set

    internal fun clean() {
        dirty = false
    }

    private fun dirty() {
        dirty = true
    }

    fun loadFrom(other: Level) {
        level_a = other.level_a
        exp_a = other.exp_a
    }

    fun copy() = Level(WeakReference<EntityPlayer>(null)).also {
        it.loadFrom(this)
    }

    companion object {
        @ConfigIntRange(0, Int.MAX_VALUE)
        @ConfigProperty("general", "defaults and range are skewed ;-;")
        var LEVEL_CAP = 100
    }
}

val EntityPlayer.stats get() = this.getCapability(PlayerStats.Capability, null)!!

infix fun EntityPlayer.exp(exp: Long) {
    this.stats.level += exp
}

infix fun EntityPlayer.exp(exp: Int) {
    this.stats.level += exp.toLong()
}