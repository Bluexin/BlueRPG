package be.bluexin.rpg.gear

import com.teamwizardry.librarianlib.features.base.IModelGenerator
import com.teamwizardry.librarianlib.features.base.item.ItemMod
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.entity.EntityLivingBase
import net.minecraft.inventory.EntityEquipmentSlot
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.world.World

class ItemOffHand private constructor(override val type: OffHandType) : ItemMod(type.key), IModelGenerator, IRPGGear {

    companion object {
        private val pieces = Array(OffHandType.values().size) { typeIdx ->
            val type = OffHandType.values()[typeIdx]
            ItemOffHand(type)
        }

        operator fun get(type: OffHandType) = pieces[type.ordinal]
    }

    override fun getAttributeModifiers(slot: EntityEquipmentSlot, stack: ItemStack) =
            super<IRPGGear>.getAttributeModifiers(slot, stack)

    override fun initCapabilities(stack: ItemStack, nbt: NBTTagCompound?) =
            super<IRPGGear>.initCapabilities(stack, nbt)

    override fun addInformation(stack: ItemStack, worldIn: World?, tooltip: MutableList<String>, flagIn: ITooltipFlag) =
            super<IRPGGear>.addInformation(stack, worldIn, tooltip, flagIn)

    override val gearSlot: EntityEquipmentSlot
        get() = EntityEquipmentSlot.OFFHAND

    override val item: Item
        get() = this

    override fun isShield(stack: ItemStack, entity: EntityLivingBase?) = entity == null
}