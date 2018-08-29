package be.bluexin.rpg.gear

import com.teamwizardry.librarianlib.features.base.item.ItemModSword
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.inventory.EntityEquipmentSlot
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.world.World

class ItemMeleeWeapon private constructor(override val type: MeleeWeaponType) : ItemModSword(type.key, ToolMaterial.IRON), IRPGGear {

    companion object {
        private val pieces = Array(MeleeWeaponType.values().size) { typeIdx ->
            val type = MeleeWeaponType.values()[typeIdx]
            ItemMeleeWeapon(type)
        }

        operator fun get(type: MeleeWeaponType) = pieces[type.ordinal]
    }

    override fun getAttributeModifiers(slot: EntityEquipmentSlot, stack: ItemStack) =
            super<IRPGGear>.getAttributeModifiers(slot, stack)

    override fun initCapabilities(stack: ItemStack, nbt: NBTTagCompound?) =
            super<IRPGGear>.initCapabilities(stack, nbt)

    override fun addInformation(stack: ItemStack, worldIn: World?, tooltip: MutableList<String>, flagIn: ITooltipFlag) =
            super<IRPGGear>.addInformation(stack, worldIn, tooltip, flagIn)

    override val item: Item
        get() = this

    override val gearSlot: EntityEquipmentSlot
        get() = EntityEquipmentSlot.MAINHAND
}