@file:Suppress("unused")

package be.bluexin.rpg

import be.bluexin.rpg.gear.*
import be.bluexin.rpg.items.DebugExpItem
import be.bluexin.rpg.items.DebugStatsItem
import be.bluexin.rpg.stats.GearStats
import be.bluexin.rpg.stats.PlayerStats
import be.bluexin.saomclib.capabilities.CapabilitiesHandler
import com.teamwizardry.librarianlib.features.base.ModCreativeTab
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.common.capabilities.CapabilityManager
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

open class CommonProxy {
    open fun preInit() {
        classLoadItems()

        CapabilitiesHandler.registerEntityCapability(PlayerStats::class.java, PlayerStats.Storage) { it is EntityPlayer }
        // Not using SAOMCLib for this one because we don't want it autoregistered
        CapabilityManager.INSTANCE.register(GearStats::class.java, GearStats.Storage) { GearStats(ItemStack.EMPTY) }

        MinecraftForge.EVENT_BUS.register(CommonEventHandler)
    }

    private fun classLoadItems() {
        object: ModCreativeTab() {
            override val iconStack: ItemStack
                get() = ItemStack(DebugStatsItem)

            init {
                registerDefaultTab()
            }
        }
        DebugStatsItem
        DebugExpItem
        ItemArmor
        ItemMeleeWeapon
        ItemRangedWeapon
        ItemOffHand
        ItemGearToken
    }
}

@SideOnly(Side.CLIENT)
class ClientProxy: CommonProxy() {
    override fun preInit() {
        super.preInit()

        MinecraftForge.EVENT_BUS.register(ClientEventHandler)
    }
}

@SideOnly(Side.SERVER)
class ServerProxy: CommonProxy() {
    override fun preInit() {
        super.preInit()

        MinecraftForge.EVENT_BUS.register(ServerEventHandler)
    }
}