package be.bluexin.rpg.stats

import be.bluexin.rpg.RPG
import be.bluexin.rpg.events.ExperienceChangeEvent
import be.bluexin.rpg.events.LevelUpEvent
import be.bluexin.rpg.fire
import be.bluexin.saomclib.capabilities.AbstractCapability
import be.bluexin.saomclib.capabilities.AbstractEntityCapability
import be.bluexin.saomclib.capabilities.Key
import be.bluexin.saomclib.onServer
import com.teamwizardry.librarianlib.features.saving.AbstractSaveHandler
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
class PlayerStats : AbstractEntityCapability() {

    @Save
    lateinit var level: Level
        private set

    @Save
    var attributePoints = LEVELUP_ATTRIBUTES * 3
        set(value) {
            field = value
            dirty()
        }

    @Save
    lateinit var baseStats: StatsCollection

    operator fun get(stat: Stats): Int {
        return baseStats[stat] // TODO: add equipment stats once that's in
    }

    override fun setup(param: Any): AbstractCapability {
        super.setup(param)

        val wr = WeakReference(param as EntityPlayer)
        level = Level(wr)
        baseStats = StatsCollection(wr)

        return this
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
        override fun readNBT(capability: Capability<PlayerStats>, instance: PlayerStats, side: EnumFacing?, nbt: NBTBase) {
            val nbtTagCompound = nbt as? NBTTagCompound ?: return
            AbstractSaveHandler.readAutoNBT(instance, nbtTagCompound.getTag(KEY.toString()), false)
        }

        override fun writeNBT(capability: Capability<PlayerStats>, instance: PlayerStats, side: EnumFacing?): NBTBase {
            return NBTTagCompound().also { it.setTag(KEY.toString(), AbstractSaveHandler.writeAutoNBT(instance, false)) }
        }
    }

    companion object {
        @Key
        val KEY = ResourceLocation(RPG.MODID, "player_stats")

        @CapabilityInject(PlayerStats::class)
        lateinit var Capability: Capability<PlayerStats>
            internal set

        const val LEVELUP_ATTRIBUTES = 2
    }
}

@SaveInPlace
class Level(private val player: WeakReference<EntityPlayer>) {

    @Save
    var level = 1
        private set(value) {
            if (value <= LEVEL_CAP && field != value) {
                val old = field
                field = value
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

    @Save
    var exp = 0L
        private set(value) {
            if (value == field) return
            val actualValue = max(value, 0)
            val evt = with(player.get()) {
                return@with if (this != null) ExperienceChangeEvent(this, field, actualValue) else null
            }
            if (evt != null && fire(evt) && evt.result != Event.Result.DENY) {
                field = evt.newValue
                dirty()
                checkLevelup()
            }
        }

    var toNext: Long = 100
        get() = if (level < LEVEL_CAP) {
            if (lastComputedLevel == level) field
            else {
                field = 50L * level * level + 50 // TODO: find nice formula
                lastComputedLevel = level
                field
            }
        } else -1L
        private set

    private var lastComputedLevel = 1

    operator fun plusAssign(exp: Long) {
        this.exp += exp
    }

    private fun checkLevelup() {
        val toNext = this.toNext
        if (level < LEVEL_CAP && exp >= toNext) {
            exp -= toNext
            ++level
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

    private companion object {
        const val LEVEL_CAP = 100
    }
}

val EntityPlayer.stats get() = this.getCapability(PlayerStats.Capability, null)!!

infix fun EntityPlayer.exp(exp: Long) {
    this.stats.level += exp
}

infix fun EntityPlayer.exp(exp: Int) {
    this.stats.level += exp.toLong()
}