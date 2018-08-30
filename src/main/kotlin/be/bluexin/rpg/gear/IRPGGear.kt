package be.bluexin.rpg.gear

import be.bluexin.rpg.BlueRPG
import be.bluexin.rpg.stats.GearStats
import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import com.teamwizardry.librarianlib.features.kotlin.localize
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.entity.ai.attributes.AttributeModifier
import net.minecraft.inventory.EntityEquipmentSlot
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.world.World
import net.minecraftforge.common.capabilities.ICapabilityProvider

interface IRPGGear { // TODO: use ISpecialArmor

    val type: GearType

    val gearSlot: EntityEquipmentSlot

    fun addInformation(stack: ItemStack, worldIn: World?, tooltip: MutableList<String>, flagIn: ITooltipFlag) {
        val cap = stack.getCapability(GearStats.Capability, null)
        if (cap != null) {
            tooltip.add(cap.rarity.format("rpg.display.item".localize(cap.rarity.localized, "rpg.$key.name".localize())))
            tooltip.add("rpg.display.level".localize(cap.ilvl))
            val shift = GuiScreen.isShiftKeyDown()
            tooltip.add("rpg.display.stats".localize())
            if (cap.stats.isEmpty()) tooltip.add("rpg.display.notgenerated".localize())
            else tooltip.addAll(cap.stats().map {
                "rpg.display.stat".localize(if (shift) it.key.longName() else it.key.shortName(), it.value)
            })
        }
    }

    fun getAttributeModifiers(slot: EntityEquipmentSlot, stack: ItemStack): Multimap<String, AttributeModifier> {
        return HashMultimap.create() // TODO use this for stats like hp?
    }

    fun initCapabilities(stack: ItemStack, nbt: NBTTagCompound?): ICapabilityProvider? {
        BlueRPG.LOGGER.warn("$stack -> $nbt")
        return GearStatWrapper(stack)
    }

    fun addNBTShare(stack: ItemStack, nbt: NBTTagCompound): NBTTagCompound {
        val cap = stack.getCapability(GearStats.Capability, null)?: return nbt
        val capNbt = GearStats.Storage.writeNBT(GearStats.Capability, cap, null)
        nbt.setTag("capabilities", capNbt)
        BlueRPG.LOGGER.warn("Write share: $nbt")
        return nbt
    }

    fun readNBTShare(stack: ItemStack, nbt: NBTTagCompound?) {
        BlueRPG.LOGGER.warn("Read share: $nbt")
        val capNbt = nbt?.getCompoundTag("capabilities")?: return
        val cap = stack.getCapability(GearStats.Capability, null)?: return
        GearStats.Storage.readNBT(GearStats.Capability, cap, null, capNbt)
    }

    val item: Item

    val key: String
        get() = type.key
}