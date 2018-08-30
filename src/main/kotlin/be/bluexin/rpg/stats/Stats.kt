package be.bluexin.rpg.stats

import be.bluexin.rpg.events.StatChangeEvent
import be.bluexin.rpg.gear.GearType
import be.bluexin.rpg.gear.Rarity
import be.bluexin.rpg.util.RNG
import be.bluexin.rpg.util.fire
import com.teamwizardry.librarianlib.features.kotlin.localize
import com.teamwizardry.librarianlib.features.saving.NamedDynamic
import com.teamwizardry.librarianlib.features.saving.Savable
import com.teamwizardry.librarianlib.features.saving.Save
import com.teamwizardry.librarianlib.features.saving.SaveInPlace
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.EntityEquipmentSlot
import net.minecraftforge.fml.common.eventhandler.Event
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import java.lang.ref.WeakReference

@SaveInPlace
class StatsCollection(private val reference: WeakReference<out Any>, @Save private var collection: HashMap<Stat, Int> = HashMap()) {
    // TODO: move collection to `val` and `withDefault` (instead of getOrDefault) once liblib updates

    operator fun get(stat: Stat): Int = collection.getOrDefault(stat, 0)
    operator fun set(stat: Stat, value: Int): Boolean {
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

    operator fun iterator() = (collection as Map<Stat, Int>).iterator()

    fun copy() = StatsCollection(reference, HashMap(collection))

    internal var dirty = false
        private set

    internal fun clean() {
        dirty = false
    }

    internal fun dirty() {
        dirty = true
    }

    fun clear() = collection.clear()

    fun isEmpty() = collection.isEmpty()
}

@NamedDynamic(resourceLocation = "b:s")
@Savable
interface Stat {

    val name: String

    @SideOnly(Side.CLIENT)
    fun longName(): String {
        return "rpg.${name.toLowerCase()}.long".localize()
    }

    @SideOnly(Side.CLIENT)
    fun shortName(): String {
        return "rpg.${name.toLowerCase()}.short".localize()
    }

    fun getRoll(ilvl: Int, rarity: Rarity, gearType: GearType, slot: EntityEquipmentSlot): Int
}

@NamedDynamic(resourceLocation = "b:ps")
enum class PrimaryStat: Stat {
    STRENGTH,
    CONSTITUTION,
    DEXTERITY,
    INTELLIGENCE,
    WISDOM,
    CHARISMA;

    override fun getRoll(ilvl: Int, rarity: Rarity, gearType: GearType, slot: EntityEquipmentSlot): Int {
        return RNG.nextInt(10) + ilvl // TODO: formulae
    }
}

@NamedDynamic(resourceLocation = "b:ss")
enum class SecondaryStat: Stat {
    HEALTH,
    PSYCHE,
    REGEN,
    SPIRIT,
    REFLECT,
    BLOCK,
    DODGE,
    SPEED,
    LIFE_STEAL_CHANCE,
    LIFE_STEAL,
    MANA_LEECH_CHANCE,
    MANA_LEECH,
    CRIT_CHANCE,
    CRIT_DAMAGE,
    BONUS_TO_SKILL,
    BONUS_DAMAGE,
    RESISTANCE,
    ROOT,
    SLOW,
    COOLDOWN_REDUCTION,
    MANA_REDUCTION;

    override fun getRoll(ilvl: Int, rarity: Rarity, gearType: GearType, slot: EntityEquipmentSlot): Int {
        return RNG.nextInt(10) + ilvl // TODO: formulae
    }
}