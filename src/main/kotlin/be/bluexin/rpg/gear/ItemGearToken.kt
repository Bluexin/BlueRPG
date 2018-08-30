package be.bluexin.rpg.gear

import be.bluexin.rpg.stats.GearStats
import be.bluexin.rpg.stats.Level
import be.bluexin.saomclib.onServer
import com.teamwizardry.librarianlib.features.base.item.ItemMod
import com.teamwizardry.librarianlib.features.kotlin.localize
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.ActionResult
import net.minecraft.util.EnumActionResult
import net.minecraft.util.EnumHand
import net.minecraft.util.NonNullList
import net.minecraft.world.World
import net.minecraftforge.items.ItemHandlerHelper

class ItemGearToken private constructor(val type: TokenType) : ItemMod("gear_token_${type.key}") {

    companion object {
        private val pieces = Array(TokenType.values().size) { typeIdx ->
            val type = TokenType.values()[typeIdx]
            ItemGearToken(type)
        }

        operator fun get(type: TokenType) = pieces[type.ordinal]
    }

    init {
        hasSubtypes = false
    }

    override fun onItemRightClick(worldIn: World, playerIn: EntityPlayer, handIn: EnumHand): ActionResult<ItemStack> {
        var stack = playerIn.getHeldItem(handIn)

        worldIn onServer {
            val rarity = type.generateRarity()
            val gear = GearTypeGenerator()
            val iss = ItemStack(gear.item)
            val stats = iss.getCapability(GearStats.Capability, null)
                    ?: throw IllegalStateException("Missing capability!")
            stats.rarity = rarity
            stats.ilvl = stack.itemDamage + 1
            stats.generate()

            if (!playerIn.isCreative) stack.shrink(1)
            if (stack.isEmpty) stack = iss
            else ItemHandlerHelper.giveItemToPlayer(playerIn, iss)
        }

        return ActionResult.newResult(EnumActionResult.SUCCESS, stack)
    }

    override fun addInformation(stack: ItemStack, worldIn: World?, tooltip: MutableList<String>, flagIn: ITooltipFlag) {
        tooltip.add("rpg.display.level".localize(stack.itemDamage + 1))
    }

    override fun getSubItems(tab: CreativeTabs, subItems: NonNullList<ItemStack>) {
        if (this.isInCreativeTab(tab)) for (i in 0..Level.LEVEL_CAP step 5) subItems.add(ItemStack(this, 1, i - 1))
    }

    override fun getDamage(stack: ItemStack): Int {
        return super.getDamage(stack)
    }
}