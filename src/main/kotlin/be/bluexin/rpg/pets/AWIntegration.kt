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

package be.bluexin.rpg.pets

import com.teamwizardry.librarianlib.features.kotlin.isNotEmpty
import moe.plushie.armourers_workshop.common.crafting.ItemSkinningRecipes
import moe.plushie.armourers_workshop.common.crafting.recipe.RecipeItemSkinning
import moe.plushie.armourers_workshop.common.items.ModItems
import moe.plushie.armourers_workshop.utils.SkinNBTHelper
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

object AWIntegration {
    init {
        ItemSkinningRecipes.recipes.add(RecipeSkinPetEgg)
    }
}

object RecipeSkinPetEgg : RecipeItemSkinning(null) {

    override fun onCraft(inventory: IInventory) {
        for (slotId in 0 until inventory.sizeInventory) inventory.decrStackSize(slotId, 1)
    }

    override fun getCraftingResult(inventory: IInventory): ItemStack {
        var skinStack = ItemStack.EMPTY
        var itemStack = ItemStack.EMPTY

        for (slotId in 0 until inventory.sizeInventory) {
            val stack = inventory.getStackInSlot(slotId)
            if (stack.isNotEmpty) {
                if (stack.item === ModItems.skin && SkinNBTHelper.stackHasSkinData(stack)) {
                    if (skinStack.isNotEmpty) return ItemStack.EMPTY
                    skinStack = stack
                } else {
                    if (itemStack.isNotEmpty) return ItemStack.EMPTY
                    itemStack = stack
                }
            }
        }

        return if (skinStack.isNotEmpty && itemStack.isNotEmpty) {
            val returnStack = itemStack.copy()

            val skinData = SkinNBTHelper.getSkinDescriptorFromStack(skinStack)
            if (!returnStack.hasTagCompound()) returnStack.tagCompound = NBTTagCompound()
            skinData.writeToCompound(returnStack.tagCompound, "skin")

            returnStack
        } else ItemStack.EMPTY
    }

    override fun matches(inventory: IInventory) = getCraftingResult(inventory).isNotEmpty
}