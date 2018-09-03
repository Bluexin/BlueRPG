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

@file:Suppress("unused")

package be.bluexin.rpg

import be.bluexin.rpg.blocks.BlockEditor
import be.bluexin.rpg.containers.ContainerEditor
import be.bluexin.rpg.gear.*
import be.bluexin.rpg.items.DebugExpItem
import be.bluexin.rpg.items.DebugStatsItem
import be.bluexin.rpg.stats.*
import be.bluexin.saomclib.capabilities.CapabilitiesHandler
import com.teamwizardry.librarianlib.features.base.ModCreativeTab
import com.teamwizardry.librarianlib.features.saving.AbstractSaveHandler
import com.teamwizardry.librarianlib.features.saving.Savable
import com.teamwizardry.librarianlib.features.saving.Save
import net.minecraft.entity.SharedMonsterAttributes
import net.minecraft.entity.ai.attributes.BaseAttribute
import net.minecraft.entity.ai.attributes.RangedAttribute
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.common.capabilities.CapabilityManager
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import net.minecraftforge.fml.relauncher.ReflectionHelper
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import java.lang.ref.WeakReference

open class CommonProxy {
    open fun preInit(event: FMLPreInitializationEvent) {
        classLoadItems()
        vanillaHax()

        CapabilitiesHandler.registerEntityCapability(PlayerStats::class.java, PlayerStats.Storage) { it is EntityPlayer }
        // Not using SAOMCLib for this one because we don't want it autoregistered
        CapabilityManager.INSTANCE.register(GearStats::class.java, GearStats.Storage) { GearStats(ItemStack.EMPTY) }
        CapabilityManager.INSTANCE.register(TokenStats::class.java, TokenStats.Storage) { TokenStats(ItemStack.EMPTY) }

        MinecraftForge.EVENT_BUS.register(CommonEventHandler)

        NameGenerator.preInit(event)
    }

    private fun vanillaHax() {
        (SharedMonsterAttributes.ATTACK_DAMAGE as BaseAttribute).shouldWatch = true
        try {
            val f = ReflectionHelper.findField(RangedAttribute::class.java, "field_111118_b", "maximumValue")
            f.set(SharedMonsterAttributes.MAX_HEALTH, Double.MAX_VALUE)
        } catch (e: Exception) {
            BlueRPG.LOGGER.warn("Unable to break max health limit", e)
        }
    }

    private fun classLoadItems() {
        object: ModCreativeTab() {
            override val iconStack: ItemStack
                get() = ItemStack(ItemGearToken[TokenType.TOKEN])

            init {
                registerDefaultTab()
            }
        }
        ItemGearToken
        DebugStatsItem
        DebugExpItem
        ItemArmor
        ItemMeleeWeapon
        ItemRangedWeapon
        ItemOffHand

        BlockEditor
        ContainerEditor
    }

    open fun init(event: FMLInitializationEvent) {
        trickLiblib()
    }

    private fun trickLiblib() { // FIXME: remove once TeamWizardry/LibrarianLib#57 is fixed
        @Savable
        class T(@Save val s: StatCapability)
        @Savable
        class T2(@Save val s: Stat)

        AbstractSaveHandler.writeAutoNBT(T(GearStats(ItemStack.EMPTY)), false)
        AbstractSaveHandler.writeAutoNBT(T(TokenStats(ItemStack.EMPTY)), false)
        AbstractSaveHandler.writeAutoNBT(T(PlayerStats().apply {
            baseStats = StatsCollection(WeakReference(Unit))
            level = Level(WeakReference<EntityPlayer>(null))
        }), false)
        AbstractSaveHandler.writeAutoNBT(T2(PrimaryStat.DEXTERITY), false)
        AbstractSaveHandler.writeAutoNBT(T2(SecondaryStat.PSYCHE), false)
        AbstractSaveHandler.writeAutoNBT(T2(FixedStat.HEALTH), false)
    }
}

@SideOnly(Side.CLIENT)
class ClientProxy: CommonProxy() {
    override fun preInit(event: FMLPreInitializationEvent) {
        super.preInit(event)

        MinecraftForge.EVENT_BUS.register(ClientEventHandler)
    }
}

@SideOnly(Side.SERVER)
class ServerProxy: CommonProxy() {
    override fun preInit(event: FMLPreInitializationEvent) {
        super.preInit(event)

        MinecraftForge.EVENT_BUS.register(ServerEventHandler)
    }
}