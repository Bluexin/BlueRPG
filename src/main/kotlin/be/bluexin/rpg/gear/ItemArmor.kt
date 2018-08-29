package be.bluexin.rpg.gear

import com.teamwizardry.librarianlib.features.base.item.ItemModArmor
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.inventory.EntityEquipmentSlot
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.world.World

class ItemArmor private constructor(override val type: ArmorType, material: ArmorMaterial, slot: EntityEquipmentSlot)
    : ItemModArmor("${slot.getName()}_${type.key}", material, slot), IRPGGear {

    companion object {
        private val pieces = Array(ArmorType.values().size) { typeIdx ->
            val type = ArmorType.values()[typeIdx]
            val armorSlots = EntityEquipmentSlot.values().filter {
                it.slotType == EntityEquipmentSlot.Type.ARMOR
            }.sortedBy { it.index }
            Array(armorSlots.size) { slotIdx ->
                val armorSlot = armorSlots[slotIdx]
                ItemArmor(
                        type,
                        type.armorMaterial,
                        armorSlot
                )
            }
        }

        operator fun get(type: ArmorType, slot: EntityEquipmentSlot) = pieces[type.ordinal][slot.index]
        operator fun get(slot: EntityEquipmentSlot, type: ArmorType) = pieces[type.ordinal][slot.index]
    }

    override fun getAttributeModifiers(slot: EntityEquipmentSlot, stack: ItemStack) =
            super<IRPGGear>.getAttributeModifiers(slot, stack)

    override fun initCapabilities(stack: ItemStack, nbt: NBTTagCompound?) =
            super<IRPGGear>.initCapabilities(stack, nbt)

    override fun addInformation(stack: ItemStack, worldIn: World?, tooltip: MutableList<String>, flagIn: ITooltipFlag) =
            super<IRPGGear>.addInformation(stack, worldIn, tooltip, flagIn)

    override val key = "${slot.getName()}_${type.key}"

    override val gearSlot: EntityEquipmentSlot
        get() = equipmentSlot

    override val item: Item
        get() = this
}