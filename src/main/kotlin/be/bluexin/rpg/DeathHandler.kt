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

package be.bluexin.rpg

import be.bluexin.rpg.devutil.RNG
import be.bluexin.rpg.devutil.get
import be.bluexin.rpg.devutil.set
import be.bluexin.rpg.inventory.RPGInventory
import com.teamwizardry.librarianlib.features.config.ConfigDoubleRange
import com.teamwizardry.librarianlib.features.config.ConfigProperty
import com.teamwizardry.librarianlib.features.kotlin.isNotEmpty
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.text.TextComponentTranslation
import net.minecraftforge.event.entity.living.LivingDeathEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.random.Random

@Mod.EventBusSubscriber(modid = BlueRPG.MODID)
object DeathHandler {

    @ConfigDoubleRange(0.0, 1.0)
    @ConfigProperty("death rules", "Drop chance per slot, during day time")
    var dropChanceDay = 0.2
        internal set

    @ConfigDoubleRange(0.0, 1.0)
    @ConfigProperty("death rules", "Drop chance per slot, during night time")
    var dropChanceNight = 0.7
        internal set

    @ConfigProperty("death rules", "Whether to destroy a piece of equipment on death")
    var destroyArmor = true
        internal set

    fun loadDropRestriction() {
        var s = dropRestriction
        limitIsWhitelist = !s.startsWith('!')
        if (!limitIsWhitelist) s = s.substring(1)
        dropRestrictionItems = s.split(',')
            .mapNotNull {
                if (it.isNotBlank()) Item.getByNameOrId(it.trim())
                    ?: null.apply { BlueRPG.LOGGER.warn("Invalid ID: `$it`") } else null
            }
    }

    @ConfigProperty(
        "death rules",
        "Comma-separated list with the registry names of all permitted items. Prefix with ! to turn into a blacklist instead."
    )
    var dropRestriction: String = "!"
    private lateinit var dropRestrictionItems: List<Item>
    private var limitIsWhitelist = false

    private val rng = Random(RNG.nextLong())

    @SubscribeEvent
    @JvmStatic
    fun handleDrops(event: LivingDeathEvent) { // do not change this to PlayerDropEvent !! see diff between EP & EPMP implementations of #onDeath
        val e = event.entity as? EntityPlayer ?: return
        this.handleDropping(e, if (e.world.isDaytime) dropChanceDay else dropChanceNight)
        if (this.destroyArmor) this.handleDestroying(e)
    }

    private fun handleDropping(player: EntityPlayer, chance: Double) {
        if (chance > 0) {
            val i = player.inventory as RPGInventory
            var drops = 0
            i.allIndices.forEach {
                if (it !in i.armorIndices && it != i.offHandIndex && it != i.eggIndex && rng.nextDouble() < chance) {
                    val iss = i[it]
                    if (iss.isNotEmpty && (iss.item in dropRestrictionItems == limitIsWhitelist)) {
                        player.dropItem(iss, true, false)
                        i[it] = ItemStack.EMPTY
                        ++drops
                    }
                }
            }
            if (drops > 0) player.sendMessage(
                if (drops > 1) TextComponentTranslation("rpg.death.many", drops)
                else TextComponentTranslation("rpg.drop.single")
            )
        }
    }

    private fun handleDestroying(player: EntityPlayer) {
        val inv = player.inventory as RPGInventory
        val i = inv.destroyableIndices.random(rng)
        val it = player.inventory[i]
        if (it.isEmpty) player.server!!.playerList.sendMessage(
            TextComponentTranslation(
                "rpg.death.free",
                player.displayName
            )
        )
        else {
            player.inventory[i] = ItemStack.EMPTY
            player.server!!.playerList.sendMessage(
                TextComponentTranslation(
                    "rpg.death.notfree",
                    player.displayName,
                    it.textComponent
                )
            )
        }
    }
}