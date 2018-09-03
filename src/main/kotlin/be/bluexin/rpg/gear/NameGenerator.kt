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

package be.bluexin.rpg.gear

import be.bluexin.rpg.BlueRPG
import be.bluexin.rpg.stats.stats
import be.bluexin.rpg.util.random
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import com.teamwizardry.librarianlib.features.kotlin.plus
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.EntityEquipmentSlot
import net.minecraft.item.ItemStack
import net.minecraft.util.text.TextFormatting
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.lang.reflect.Type

object NameGenerator {

    private enum class NamePart {
        PREFIX,
        SUFFIX,
        PREFIX_LEGENDARY,
        SUFFIX_LEGENDARY,
        CORE
    }

    private val fix = mutableMapOf(
            NamePart.PREFIX to arrayOf("Mangled", "Heartburning", "Ichibod's", "Apostle's", "Bloodied", "Tainted", "Purified", "Frostbitten", "Scorched", "Electrostatic", "Solar", "Tidal", "Mutilated", "Grotesque", "Cultured", "Windswept", "Roaring", "Phrozen's", "Super's", "Reaping", "Earthbound", "Buried", "Gypsy's", "Searing", "Flowing", "Stoneskin", "Steelskin", "Glorious", "Rusty", "Traditional", "Ancient", "Shimmering", "Ancestral", "Sacrificial", "Wanadi's", "Lionheart", "Dragonheart", "Swampy", "Obliterating", "War-Torn", "Shiny", "Battle-Hard", "Faded", "Cracked", "Reinforced", "Diligent", "Deadwind", "Cat's", "Yin's", "Yang's", "Venomous", "Sparking", "Glimmering", "Angered", "Weightless", "Featherlight", "Vanishing", "Silent", "Deafening", "Icarus's", "Mighty", "Vorporal", "Enchanting", "Corse", "Platinum", "Savage", "Brutal", "Cruel", "Astral", "Foul", "Toxic", "Blessed", "Faithful", "Dirty", "Evil", "Holy", "Screaming", "Howling", "Aged", "Nimble", "Keen", "Flat's", "Fine", "King's", "Master", "Grandmaster", "Deadly", "Merciless", "Celestial", "Sacred", "Divine", "Arcing", "Sturdy", "Faithful", "Bluexin's", "Sky's", "Lilly's", "Tireless", "Trickster's", "Sensei's", "Feral", "Hexing", "Fungal", "Branded"),
            NamePart.SUFFIX to arrayOf("Corrosion", "the Moon", "the Massacred", "the Inkborn", "the Sun", "the Immortal", "the Dead", "the Exiled", "Decay", "the Bear", "the Lion", "the Crow", "the Eagle", "the Wolf", "the Viper", "the Cobra", "the Strider", "the Dragon", "the Hydra", "the Chimera", "the Champion", "the Cyclops", "the Muse", "the Titan", "the Muse", "the Siren", "the Gryphon", "Ruin", "Annihilation", "the Afterlife", "Vitality", "the Moral", "Preservation", "Decimation", "Souls", "Quiet Mind", "the Lucky", "Conquest", "Assassins", "the Bane-Born", "Creation", "Restoration", "Shattering", "Extinction", "the Drake", "the Leviathan", "the Landwurm", "Betrayal", "the Trusted", "the Fool", "the Wise", "the Minotaur", "the Yeti", "the Black Widow", "the Headless", "the Golem", "the Banshee", "Treachery", "Spite", "Promise", "Solemn Vows", "Bad Habits", "Deception", "The Elements", "Shadowtones", "Legends", "Legacy", "Vile Wispers", "Spectacles", "Insanity", "Storms", "Destiny", "Focus", "Deflection", "Gestures", "Worship", "Prayer", "White Lights", "Judgement", "Blood Art", "Ultimate Pain", "Pain Rage", "the Kraken", "Worth", "Gore", "Butchery", "Carnage", "Winter", "Summer", "Thunder", "Blight", "Thorns", "Spikes"),
            NamePart.PREFIX_LEGENDARY to arrayOf("Wolf", "Wraggle", "Candy", "Uruk", "Fury", "Myth", "Hunger", "Cold", "Fate", "Vein", "Venom", "Smirk", "Vile", "Ember", "Stale", "Fool"),
            NamePart.SUFFIX_LEGENDARY to arrayOf("sbane", " Toes", "core", "tome", "spine", "totem", "wurm", "skin", "crutch", "touch", "gaurd", "branch", "glow", "'s Caress", "shadow", "tongue")
    )

    private val armorCores = mutableMapOf(
            EntityEquipmentSlot.HEAD to arrayOf("Studs", "Loops", "Plugs", "Earings", "Jewels", "Hoops"),
            EntityEquipmentSlot.CHEST to arrayOf("Garb", "Wrap", "Tunic", "Chest", "Guard", "Grasp"),
            EntityEquipmentSlot.LEGS to arrayOf("Pants", "Legs", "Leggings", "Pleats", "Tasset", "Waistguard"),
            EntityEquipmentSlot.FEET to arrayOf("Boots", "Shoes", "Walks", "Greaves", "Tabi", "Footguards")
    )

    private val weaponCores: MutableMap<GearType, Array<String>> = mutableMapOf(
            MeleeWeaponType.MACE to arrayOf("Mace", "Club", "Cudgel", "Mallot", "Truncheon", "Flail"),
            MeleeWeaponType.SWORD to arrayOf("Sword", "Blade", "Edge", "Brand", "Saber", "Razor"),
            MeleeWeaponType.AXE to arrayOf("Axe", "Hatchet", "Cleaver", "Chopper", "Francesca", "Carver"),
            MeleeWeaponType.SWORD_2H to arrayOf("Greatsword", "Claymore", "Greatedge", "Longsword", "Zweihänder", "Buster"),
            MeleeWeaponType.SPEAR to arrayOf("Spear", "Pike", "Halbard", "Lance", "Trident", "Spetum"),
            MeleeWeaponType.STAFF to arrayOf("Staff", "Cane", "Pole", "Rod", "Stave", "Wand"),
            RangedWeaponType.BOW to arrayOf("Bow", "Recurve", "Composite", "Long Bow", "Reflex", "Repeater"),
            OffHandType.SHIELD to arrayOf("Shield", "Kite", "Buckler", "Tower Shield", "Aegis", "Heater"),
            OffHandType.PARRY_DAGGER to arrayOf("Dagger", "Spike", "Knife", "Shiv", "Shank", "Dirk"),
            OffHandType.FOCUS to arrayOf("Focus", "Orb", "Globe", "Sphere", "Marble", "Jewel")
    )

    internal fun preInit(event: FMLPreInitializationEvent) {
        try {
            val dir = File(event.modConfigurationDirectory, BlueRPG.MODID)
            if (!dir.exists()) dir.mkdir()
            if (!dir.isDirectory) throw IllegalStateException("$dir exists and is not a directory")

            val gson = GsonBuilder()
                    .setPrettyPrinting()
                    .registerTypeAdapter(GearType::class.java, GearTypeDeserializer)
                    .create()

            var f = File(dir, "name_fixes.json")
            if (f.exists()) {
                val read = FileReader(f).use { gson.fromJson<Map<NamePart, List<String>>>(it, object : TypeToken<Map<NamePart, List<String>>>() {}.type) }
                read.forEach { k, it -> fix[k] = it.toTypedArray() }
            }
            FileWriter(f).use { gson.toJson(fix, it) }

            f = File(dir, "name_armors.json")
            if (f.exists()) {
                val read = FileReader(f).use { gson.fromJson<Map<EntityEquipmentSlot, List<String>>>(it, object : TypeToken<Map<EntityEquipmentSlot, List<String>>>() {}.type) }
                read.forEach { k, it -> armorCores[k] = it.toTypedArray() }
            }
            FileWriter(f).use { gson.toJson(armorCores, it) }

            f = File(dir, "name_weapons.json")
            if (f.exists()) {
                val read = FileReader(f).use { gson.fromJson<Map<GearType, List<String>>>(it, object : TypeToken<Map<GearType, List<String>>>() {}.type) }
                read.forEach { k, it -> weaponCores[k] = it.toTypedArray() }
            }
            for (it in MeleeWeaponType.values()) weaponCores.putIfAbsent(it, arrayOf("Unknown"))
            for (it in RangedWeaponType.values()) weaponCores.putIfAbsent(it, arrayOf("Unknown"))
            for (it in OffHandType.values()) weaponCores.putIfAbsent(it, arrayOf("Unknown"))
            FileWriter(f).use { gson.toJson(weaponCores, it) }
        } catch (e: Exception) {
            BlueRPG.LOGGER.warn("Unable to load translation files", e)
        }
    }

    operator fun invoke(iss: ItemStack, player: EntityPlayer): String {
        val stats = iss.stats ?: return "Error: Missing Stats"
        if (stats.rarity?.ordinal ?: 0 >= Rarity.LEGENDARY.ordinal) return generateLegendary(player).wrapped(stats.rarity!!)
        val item = iss.item
        if (item is ItemArmor) return generateArmor(item).wrapped(stats.rarity?: Rarity.COMMON)
        if (item is IRPGGear) return generateWeapon(item.type).wrapped(stats.rarity?: Rarity.COMMON)

        return "Error: Unknown Item ${iss.item}"
    }

    private fun String.wrapped(rarity: Rarity): String {
        return rarity.format(TextFormatting.BOLD + this)
    }

    private fun generateLegendary(player: EntityPlayer): String {
        return "${player.displayNameString}'s ${fix[NamePart.PREFIX_LEGENDARY]?.random()}${fix[NamePart.SUFFIX_LEGENDARY]?.random()}"
    }

    private fun generateArmor(gear: ItemArmor): String {
        return "${fix[NamePart.PREFIX]?.random()} ${armorCores[gear.gearSlot]?.random()} of ${fix[NamePart.SUFFIX]?.random()}"
    }

    private fun generateWeapon(gearType: GearType): String {
        return "${fix[NamePart.PREFIX]?.random()} ${weaponCores[gearType]?.random()} of ${fix[NamePart.SUFFIX]?.random()}"
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
            throw IllegalArgumentException("Invalid GearType: $s")
        }
    }
}
