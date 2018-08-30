package be.bluexin.rpg.gear

import be.bluexin.rpg.gear.Rarity.*
import be.bluexin.rpg.stats.PrimaryStat
import be.bluexin.rpg.stats.SecondaryStat
import be.bluexin.rpg.stats.Stat
import be.bluexin.rpg.util.Localizable
import be.bluexin.rpg.util.RNG
import com.teamwizardry.librarianlib.features.kotlin.plus
import net.minecraft.inventory.EntityEquipmentSlot
import net.minecraft.item.ItemArmor
import net.minecraft.util.text.TextFormatting
import net.minecraft.util.text.TextFormatting.*
import java.util.*

enum class Rarity(
        private val color: TextFormatting,
        private val primaryRolls: Int
) : Localizable {
    COMMON(WHITE, 1),
    UNCOMMON(GREEN, 1),
    RARE(BLUE, 2),
    EPIC(LIGHT_PURPLE, 2),
    LEGENDARY(YELLOW, 3),
    MYTHIC(RED, 3),
    GODLIKE(WHITE, 3);

    val shouldNotify by lazy { ordinal >= EPIC.ordinal }

    val rolls by lazy { ordinal + 1 }

    fun format(text: String): String {
        return color + text + RESET
    }

    fun rollStats(): Array<Stat> {
        val primaries = LinkedList(PrimaryStat.values().toList())
        val secondarySize = SecondaryStat.values().size
        return Array<Stat>(rolls) {
            if (it < primaryRolls) primaries.removeAt(RNG.nextInt(primaries.size))
            else SecondaryStat.values()[RNG.nextInt(secondarySize)]
        }
    }
}

enum class TokenType(
        private val rarityWeights: Map<Rarity, Int>
) : Localizable {
    TOKEN(mapOf(
            COMMON to 3300,
            UNCOMMON to 4100,
            RARE to 2450,
            EPIC to 150
    )),
    CRAFTED(mapOf(
            COMMON to 1000,
            UNCOMMON to 5700,
            RARE to 3700,
            EPIC to 945,
            LEGENDARY to 47,
            MYTHIC to 7,
            GODLIKE to 1
    )),
    PLUS(mapOf(
            UNCOMMON to 4000,
            RARE to 4000,
            EPIC to 1920,
            LEGENDARY to 70,
            MYTHIC to 8,
            GODLIKE to 2
    )),
    PLUS2(mapOf(
            RARE to 5000,
            EPIC to 3920,
            LEGENDARY to 1067,
            MYTHIC to 10,
            GODLIKE to 3
    ));

    private val totalWeight by lazy { rarityWeights.values.sum() }

    fun generateRarity(): Rarity {
        var r = RNG.nextInt(totalWeight)
        for ((rarity, weight) in rarityWeights) {
            r -= weight
            if (r < 0) return rarity
        }
        throw IllegalStateException("This should never happen.")
    }
}

object GearTypeGenerator {
    private val types: MutableList<Pair<GearType, Int>> = LinkedList()

    operator fun plusAssign(type: GearType) {
        for (i in 0 until type.weight) types.add(type to i)
    }

    operator fun invoke(): IRPGGear {
        val (t, i) = types[RNG.nextInt(types.size)]
        return t(i)
    }
}

interface GearType : Localizable {
    val weight: Int
        get() = 1

    operator fun invoke(i: Int = 0): IRPGGear
}

enum class ArmorType(
        val armorMaterial: ItemArmor.ArmorMaterial
) : GearType {
    CLOTH(ItemArmor.ArmorMaterial.LEATHER),
    LEATHER(ItemArmor.ArmorMaterial.LEATHER),
    PLATE(ItemArmor.ArmorMaterial.IRON);

    override fun invoke(i: Int) = when (i) {
        0 -> ItemArmor[this, EntityEquipmentSlot.FEET]
        1 -> ItemArmor[this, EntityEquipmentSlot.LEGS]
        2 -> ItemArmor[this, EntityEquipmentSlot.CHEST]
        3 -> ItemArmor[this, EntityEquipmentSlot.HEAD]
        else -> throw IllegalArgumentException("Invalid index: $i")
    }

    override val weight = 4

    init {
        @Suppress("LeakingThis")
        GearTypeGenerator += this
    }
}

enum class MeleeWeaponType : GearType {
    MACE,
    SWORD,
    AXE;

    override fun invoke(i: Int) = ItemMeleeWeapon[this]

    init {
        @Suppress("LeakingThis")
        GearTypeGenerator += this
    }
}

enum class RangedWeaponType : GearType {
    BOW;

    override fun invoke(i: Int) = ItemRangedWeapon[this]

    init {
        @Suppress("LeakingThis")
        GearTypeGenerator += this
    }
}

enum class OffHandType : GearType {
    SHIELD,
    PARRY_DAGGER,
    FOCUS;

    override fun invoke(i: Int) = ItemOffHand[this]

    init {
        @Suppress("LeakingThis")
        GearTypeGenerator += this
    }
}

enum class Binding : Localizable {
    BOE,
    BOP,
    BOU,
    BOT;
}
