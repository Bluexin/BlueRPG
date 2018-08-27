package be.bluexin.rpg.stats

import be.bluexin.rpg.RPG
import be.bluexin.rpg.events.StatChangeEvent
import be.bluexin.rpg.fire
import com.teamwizardry.librarianlib.features.kotlin.localize
import com.teamwizardry.librarianlib.features.saving.Save
import com.teamwizardry.librarianlib.features.saving.SaveInPlace
import net.minecraft.entity.player.EntityPlayer
import net.minecraftforge.fml.common.eventhandler.Event
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import java.lang.ref.WeakReference

@SaveInPlace
class StatsCollection(private val reference: WeakReference<out Any>, @Save private var collection: HashMap<Stats, Int> = HashMap()) {
    // TODO: move collection to `val` and `withDefault` (instead of getOrDefault) once liblib updates

    operator fun get(stat: Stats): Int = collection.getOrDefault(stat, 0)
    operator fun set(stat: Stats, value: Int): Boolean {
        RPG.LOGGER.warn("Set $stat to $value")
        val evt = with(reference.get()) {
            if (this is EntityPlayer) {
                StatChangeEvent(this, stat, collection.getOrDefault(stat, 0), value)
            } else null
        }
        return if (evt == null || (fire(evt) && evt.result != Event.Result.DENY)) {
            if (evt?.newValue ?: value != 0) collection[stat] = evt?.newValue ?: value
            else collection.remove(stat)
            dirty()
            true
        } else false

    }

    operator fun invoke() = collection.asSequence()

    operator fun iterator() = (collection as Map<Stats, Int>).iterator()

    fun copy() = StatsCollection(reference, HashMap(collection))

    internal var dirty = false
        private set

    internal fun clean() {
        dirty = false
    }

    internal fun dirty() {
        dirty = true
    }
}

enum class Stats {
    STRENGTH,
    CONSTITUTION,
    DEXTERITY,
    INTELLIGENCE,
    WISDOM,
    CHARISMA;

    @SideOnly(Side.CLIENT)
    fun longName(): String {
        return "rpg.${name.toLowerCase()}.long".localize()
    }

    @SideOnly(Side.CLIENT)
    fun shortName(): String {
        return "rpg.${name.toLowerCase()}.short".localize()
    }
}