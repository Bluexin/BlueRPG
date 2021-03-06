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

package be.bluexin.rpg.gear

import be.bluexin.rpg.BlueRPG
import be.bluexin.rpg.devutil.Localizable
import be.bluexin.rpg.devutil.RNG
import be.bluexin.rpg.devutil.RpgProjectile
import be.bluexin.rpg.gear.Rarity.*
import be.bluexin.rpg.stats.PrimaryStat
import be.bluexin.rpg.stats.SecondaryStat
import be.bluexin.rpg.stats.Stat
import com.teamwizardry.librarianlib.features.kotlin.plus
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.SharedMonsterAttributes
import net.minecraft.entity.ai.attributes.IAttribute
import net.minecraft.entity.ai.attributes.RangedAttribute
import net.minecraft.inventory.EntityEquipmentSlot
import net.minecraft.item.ItemArmor
import net.minecraft.util.SoundEvent
import net.minecraft.util.text.TextFormatting
import net.minecraft.util.text.TextFormatting.*
import net.minecraft.world.World
import java.util.*

enum class Rarity(
    private val color: TextFormatting,
    private val primaryRolls: Int,
    val colorRGB: Int
) : Localizable {
    COMMON(WHITE, 1, 0xFFFFFF),
    UNCOMMON(GREEN, 1, 0x55FF55),
    RARE(AQUA, 2, 0x55FFFF),
    EPIC(LIGHT_PURPLE, 2, 0xFF55FF),
    LEGENDARY(YELLOW, 3, 0xFFFF55),
    MYTHIC(RED, 3, 0xFF5555),
    GODLIKE(WHITE, 3, 0xFFFFFF);

    val shouldNotify by lazy { ordinal >= LEGENDARY.ordinal }

    val secondaryRolls by lazy { ordinal + 1 }

    fun format(text: String): String {
        return color + text + RESET
    }

    fun rollStats(): Array<Stat> {
        val primaries = LinkedList(PrimaryStat.values().toList())
        val secondaries = LinkedList(SecondaryStat.values().toList())
        val p = Array<Stat>(primaryRolls) {
            primaries.removeAt(RNG.nextInt(primaries.size))
        }
        val s = Array<Stat>(secondaryRolls) {
            secondaries.removeAt(RNG.nextInt(secondaries.size))
        }

        return p + s
    }
}

enum class TokenType(
    rarityWeights: Map<Rarity, Int>
) : Localizable {
    TOKEN(
        mapOf(
            COMMON to 3300,
            UNCOMMON to 4100,
            RARE to 2450,
            EPIC to 150
        )
    ),
    CRAFTED(
        mapOf(
            COMMON to 1000,
            UNCOMMON to 5700,
            RARE to 3700,
            EPIC to 945,
            LEGENDARY to 47,
            MYTHIC to 7,
            GODLIKE to 1
        )
    ),
    PLUS(
        mapOf(
            UNCOMMON to 4000,
            RARE to 4000,
            EPIC to 1920,
            LEGENDARY to 70,
            MYTHIC to 8,
            GODLIKE to 2
        )
    ),
    PLUS2(
        mapOf(
            RARE to 5000,
            EPIC to 3920,
            LEGENDARY to 1067,
            MYTHIC to 10,
            GODLIKE to 3
        )
    );

    var rarityWeights = rarityWeights.withDefault { 0 }
        set(value) {
            field = value
            totalWeight = value.values.sum()
        }

    private var totalWeight = rarityWeights.values.sum()

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

    val allowBlock get() = false

    val allowParry get() = false

    operator fun invoke(i: Int = 0): IRPGGear
}

enum class ArmorType(
    val armorMaterial: ItemArmor.ArmorMaterial
) : GearType {
    CLOTH(ItemArmor.ArmorMaterial.LEATHER),
    LEATHER(ItemArmor.ArmorMaterial.LEATHER),
    PLATE(ItemArmor.ArmorMaterial.IRON);

    override fun invoke(i: Int) = when (i) {
        0 -> ArmorItem[this, EntityEquipmentSlot.FEET]
        1 -> ArmorItem[this, EntityEquipmentSlot.LEGS]
        2 -> ArmorItem[this, EntityEquipmentSlot.CHEST]
        3 -> ArmorItem[this, EntityEquipmentSlot.HEAD]
        else -> throw IllegalArgumentException("Invalid index: $i")
    }

    override val weight = 4

    init {
        @Suppress("LeakingThis")
        GearTypeGenerator += this
    }
}

interface WeaponType : GearType {
    val twoHander: Boolean
}

enum class MeleeWeaponType(
    override val twoHander: Boolean = false,
    override val allowBlock: Boolean = false,
    override val allowParry: Boolean = false,
    val attributes: Array<WAValue> = arrayOf()
) : WeaponType {
    MACE(
        attributes = arrayOf(
            WeaponAttribute.KNOCKBACK to 0.8,
            WeaponAttribute.ATTACK_SPEED to -2.4
        )
    ),
    SWORD(
        allowParry = true, attributes = arrayOf(
            WeaponAttribute.ATTACK_SPEED to -2.4
        )
    ),
    AXE(
        attributes = arrayOf(
            WeaponAttribute.ANGLE to 120.0,
            WeaponAttribute.ATTACK_SPEED to -3.2
        )
    ),
    SWORD_2H(
        twoHander = true, allowBlock = true, attributes = arrayOf(
            WeaponAttribute.ANGLE to 90.0,
            WeaponAttribute.ATTACK_SPEED to -3.2,
            WeaponAttribute.RANGE to 1.5
        )
    ),
    SPEAR(
        twoHander = true, attributes = arrayOf(
            WeaponAttribute.ATTACK_SPEED to -2.4,
            WeaponAttribute.RANGE to 3.0
        )
    ),
    BO(
        twoHander = true, allowParry = true, attributes = arrayOf(
            WeaponAttribute.ATTACK_SPEED to -1.6,
            WeaponAttribute.RANGE to 2.5
        )
    );

    override fun invoke(i: Int) = MeleeWeaponItem[this]

    init {
        @Suppress("LeakingThis")
        GearTypeGenerator += this
    }
}

enum class RangedWeaponType(
    val entity: (World, EntityLivingBase) -> RpgProjectile,
    override val twoHander: Boolean = false,
    val sound: SoundEvent? = null
) : WeaponType {
    BOW(::RpgArrowEntity, twoHander = true),
    WAND(::WandProjectileEntity);

    override fun invoke(i: Int) = RangedWeaponItem[this]

    init {
        @Suppress("LeakingThis")
        GearTypeGenerator += this
    }
}

enum class OffHandType(override val allowBlock: Boolean = false, override val allowParry: Boolean = false) : GearType {
    SHIELD(allowBlock = true),
    PARRY_DAGGER(allowParry = true),
    FOCUS;

    override fun invoke(i: Int) = OffHandItem[this]

    init {
        @Suppress("LeakingThis")
        GearTypeGenerator += this
    }
}

data class WAValue(val attribute: WeaponAttribute, val value: Double)

infix fun WeaponAttribute.to(value: Double) = WAValue(this, value)

enum class WeaponAttribute(uuid: String, attribute: IAttribute? = null) : Stat {
    RANGE("d11ea045-59f1-4318-9388-d50355a544e2") {
        override val baseValue: Double get() = 4.0
    },
    ANGLE("18be9aac-8a1f-4e31-a386-806843161d8d"),
    KNOCKBACK("f2dac0e3-60df-4c8e-934c-25bf2e4b1dac"),
    ATTACK_SPEED("196414ec-f1c4-421f-ae1d-0a1b9742ddfe", SharedMonsterAttributes.ATTACK_SPEED);

    override val uuid = arrayOf(UUID.fromString(uuid))

    override val shouldRegister = attribute == null

    override val attribute: IAttribute by lazy {
        attribute
            ?: RangedAttribute(
                null,
                "${BlueRPG.MODID}.${this.name.toLowerCase()}",
                baseValue,
                0.0,
                Double.MAX_VALUE
            ).setShouldWatch(true)
    }

    override fun getRoll(ilvl: Int, rarity: Rarity, gearType: GearType, slot: EntityEquipmentSlot) = 0
}

enum class Binding : Localizable {
    BOE,
    BOP,
    BOT;
}
