package be.bluexin.rpg.stats

import be.bluexin.rpg.BlueRPG
import be.bluexin.rpg.gear.Binding
import be.bluexin.rpg.gear.IRPGGear
import be.bluexin.rpg.gear.Rarity
import be.bluexin.saomclib.capabilities.Key
import com.teamwizardry.librarianlib.features.saving.AbstractSaveHandler
import com.teamwizardry.librarianlib.features.saving.Save
import com.teamwizardry.librarianlib.features.saving.SaveInPlace
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTBase
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraft.util.ResourceLocation
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.CapabilityInject
import java.lang.ref.WeakReference

@SaveInPlace
class GearStats(val itemStackIn: ItemStack) {

    @Save
    var rarity = Rarity.COMMON // TODO

    @Save
    var binding = Binding.BOE // TODO

    @Save
    var bound = false

    @Save
    var ilvl = 1

    @Save
    var stats: StatsCollection = StatsCollection(WeakReference(itemStackIn))
        internal set

    fun generate() {
        this.stats.clear()
        val gear = itemStackIn.item as? IRPGGear?: return
        val stats = rarity.rollStats()
        stats.forEach {
            this.stats[it] = it.getRoll(ilvl, rarity, gear.type, gear.gearSlot)
        }
    }

    operator fun get(stat: Stat) = stats[stat]

    internal object Storage : Capability.IStorage<GearStats> {
        override fun readNBT(capability: Capability<GearStats>, instance: GearStats, side: EnumFacing?, nbt: NBTBase) {
            val nbtTagCompound = nbt as? NBTTagCompound ?: return
            instance.stats.clear()
            try {
                AbstractSaveHandler.readAutoNBT(instance, nbtTagCompound.getTag(KEY.toString()), false)
            } catch (_: Exception) {
                // Resetting bad data is fine
            }
        }

        override fun writeNBT(capability: Capability<GearStats>, instance: GearStats, side: EnumFacing?): NBTBase {
            return NBTTagCompound().also { it.setTag(KEY.toString(), AbstractSaveHandler.writeAutoNBT(instance, false)) }
        }
    }

    companion object {
        @Key
        val KEY = ResourceLocation(BlueRPG.MODID, "gear_stats")

        @CapabilityInject(GearStats::class)
        lateinit var Capability: Capability<GearStats>
            internal set
    }
}