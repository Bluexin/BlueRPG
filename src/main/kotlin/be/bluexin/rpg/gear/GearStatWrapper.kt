package be.bluexin.rpg.gear

import be.bluexin.rpg.stats.GearStats
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTBase
import net.minecraft.util.EnumFacing
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.ICapabilitySerializable

class GearStatWrapper(itemStack: ItemStack) : ICapabilitySerializable<NBTBase> {

    val stats = GearStats(itemStack)//.also(GearStats::generate)

    override fun <T : Any?> getCapability(capability: Capability<T>, facing: EnumFacing?): T? =
            if (capability == GearStats.Capability) GearStats.Capability.cast(stats) else null

    override fun hasCapability(capability: Capability<*>, facing: EnumFacing?) = capability == GearStats.Capability

    override fun deserializeNBT(nbt: NBTBase?) {
        if (nbt != null) GearStats.Storage.readNBT(GearStats.Capability, stats, null, nbt)
    }

    override fun serializeNBT(): NBTBase {
        return GearStats.Storage.writeNBT(GearStats.Capability, stats, null)
    }
}