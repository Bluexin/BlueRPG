/*
 * Copyright (C) 2019.  Arnaud 'Bluexin' Solé
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

package be.bluexin.rpg.inventory

import be.bluexin.rpg.gear.ItemOffHand
import be.bluexin.rpg.pets.EggItem
import be.bluexin.rpg.pets.petStorage
import be.bluexin.rpg.util.offset
import com.teamwizardry.librarianlib.features.kotlin.asNonnullListWithDefault
import com.teamwizardry.librarianlib.features.kotlin.clamp
import com.teamwizardry.librarianlib.features.kotlin.isNotEmpty
import net.minecraft.block.state.IBlockState
import net.minecraft.client.util.RecipeItemHelper
import net.minecraft.crash.CrashReport
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.item.Item
import net.minecraft.item.ItemArmor
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import net.minecraft.util.NonNullList
import net.minecraft.util.ReportedException
import net.minecraft.util.math.MathHelper
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

class RPGInventory(playerIn: EntityPlayer) : InventoryPlayer(playerIn) {

    val rpgHotbar = MutableList(4) { ItemStack.EMPTY }
    val rpgHotbarIndices = rpgHotbar.indices
    val realMainInventory = MutableList(65) { ItemStack.EMPTY }
    val realMainIndices = realMainInventory.indices offset rpgHotbarIndices.last + 1
    val armorIndices = armorInventory.indices offset realMainIndices.last + 1
    val offHandIndex = armorIndices.last + 1
    val eggSlot = MutableList(1) { ItemStack.EMPTY }
    val eggIndex = offHandIndex + 1
    val bagSlots = MutableList(3) { ItemStack.EMPTY }
    val bagIndices = bagSlots.indices offset eggIndex + 1
    val allIndices = 0..bagIndices.last
    val allAsSequence = sequenceOf(
        rpgHotbar,
        realMainInventory,
        armorInventory,
        offHandInventory,
        eggSlot,
        bagSlots
    ).flatten()
    val destroyableIndices = armorIndices + offHandIndex

    val skills = Array(5) { ItemStack.EMPTY }

    init {
        this.allInventories = listOf(
            rpgHotbar.asNonnullListWithDefault(ItemStack.EMPTY),
            realMainInventory.asNonnullListWithDefault(ItemStack.EMPTY),
            armorInventory,
            offHandInventory,
            eggSlot.asNonnullListWithDefault(ItemStack.EMPTY),
            bagSlots.asNonnullListWithDefault(ItemStack.EMPTY)
        )

        this.mainInventory = FakeMain()
    }

    override fun setInventorySlotContents(index: Int, stack: ItemStack) {
        super.setInventorySlotContents(index, stack)
        if (index == eggIndex) player.petStorage.killPet()
    }

    override fun getSizeInventory(): Int {
        return allIndices.last + 1
    }

    override fun getCurrentItem() = this.mainInventory[currentItem]

    override fun pickItem(index: Int) {
        if (player.isCreative) return super.pickItem(index)
        this.currentItem = this.bestHotbarSlot
        val itemstack = this.getCurrentItem()
        this.rpgHotbar[this.currentItem] = this.rpgHotbar[index]
        this.rpgHotbar[index] = itemstack
    }

    @SideOnly(Side.CLIENT)
    override fun setPickedItemStack(stack: ItemStack) {
        if (player.isCreative) return super.setPickedItemStack(stack)
        val i = this.getSlotFor(stack)

        if (i in rpgHotbarIndices) this.currentItem = i
        else {
            if (i == -1) {
                this.currentItem = this.bestHotbarSlot

                if (!this.rpgHotbar[this.currentItem].isEmpty) {
                    val j = this.firstEmptyStack
                    if (j != -1) this.rpgHotbar[j] = this.rpgHotbar[this.currentItem]
                }

                this.rpgHotbar[this.currentItem] = stack
            } else this.pickItem(i)
        }
    }

    override fun writeToNBT(nbtTagListIn: NBTTagList): NBTTagList {
        allAsSequence.forEachIndexed { index, itemStack ->
            if (itemStack.isNotEmpty) {
                val nbttagcompound = NBTTagCompound()
                nbttagcompound.setByte("Slot", index.toByte())
                itemStack.writeToNBT(nbttagcompound)
                nbtTagListIn.appendTag(nbttagcompound)
            }
        }

        return nbtTagListIn
    }

    override fun storeItemStack(itemStackIn: ItemStack): Int {
        when {
            this.canMergeStacks(this.getStackInSlot(currentItem), itemStackIn) -> return currentItem
            this.canMergeStacks(this.getStackInSlot(offHandIndex), itemStackIn) -> return offHandIndex
            else -> {
                for (i in rpgHotbarIndices) if (canMergeStacks(getStackInSlot(i), itemStackIn)) return i
                for (i in realMainIndices) if (canMergeStacks(getStackInSlot(i), itemStackIn)) return i
                return -1
            }
        }
    }

    override fun isEmpty() = allAsSequence.all(ItemStack::isEmpty)

    override fun getBestHotbarSlot(): Int {
        if (player.isCreative) return super.getBestHotbarSlot()
        for (i in rpgHotbarIndices) {
            val j = (this.currentItem + i) % rpgHotbar.size
            if (this.rpgHotbar[j].isEmpty) return j
        }

        for (k in rpgHotbarIndices) {
            val l = (this.currentItem + k) % rpgHotbar.size
            if (!this.rpgHotbar[l].isItemEnchanted) return l
        }

        return this.currentItem.clamp(rpgHotbarIndices.first, rpgHotbarIndices.last)
    }

    @SideOnly(Side.CLIENT)
    override fun changeCurrentItem(direction: Int) {
        if (player.isCreative) return super.changeCurrentItem(direction)
        this.currentItem = (this.currentItem - MathHelper.clamp(direction, -1, 1) + rpgHotbar.size) % rpgHotbar.size
    }

    override fun getFirstEmptyStack() =
        this.realMainInventory.indexOfFirst(ItemStack::isEmpty).let { if (it >= 0) it + this.realMainIndices.first else it }

    override fun fillStackedContents(helper: RecipeItemHelper, includeOffHand: Boolean) {
        for (itemstack in this.rpgHotbar) helper.accountStack(itemstack)
        for (itemstack in this.realMainInventory) helper.accountStack(itemstack)
        if (includeOffHand) helper.accountStack(this.offHandInventory[0])
    }

    override fun readFromNBT(nbtTagListIn: NBTTagList) {
        this.clear()

        nbtTagListIn.forEach {
            it as NBTTagCompound

            val j = it.getByte("Slot").toInt() and 255
            val iss = ItemStack(it)
            if (iss.isNotEmpty) this.setInventorySlotContents(j, iss)
        }
    }

    override fun add(_index: Int, stack: ItemStack): Boolean {
        var index = _index
        return if (stack.isEmpty) false
        else {
            try {
                if (stack.isItemDamaged) {
                    if (index == -1) index = this.firstEmptyStack

                    when {
                        index >= 0 -> {
                            this.realMainInventory[index - this.realMainIndices.first] = stack.copy()
                            this.realMainInventory[index - this.realMainIndices.first].animationsToGo = 5
                            stack.count = 0
                            true
                        }
                        this.player.capabilities.isCreativeMode -> {
                            stack.count = 0
                            true
                        }
                        else -> false
                    }
                } else {
                    var i: Int

                    while (true) {
                        i = stack.count

                        if (index == -1) stack.count = this.storePartialItemStack(stack)
                        else stack.count = this.addResource(index, stack)
                        if (stack.isEmpty || stack.count >= i) break
                    }

                    if (stack.count == i && this.player.capabilities.isCreativeMode) {
                        stack.count = 0
                        true
                    } else stack.count < i
                }
            } catch (throwable: Throwable) {
                val crashreport = CrashReport.makeCrashReport(throwable, "Adding item to inventory")
                val crashreportcategory = crashreport.makeCategory("Item being added")
                crashreportcategory.addCrashSection("Item ID", Integer.valueOf(Item.getIdFromItem(stack.item)))
                crashreportcategory.addCrashSection("Item data", Integer.valueOf(stack.metadata))
                crashreportcategory.addDetail("Registry Name") { stack.item.registryName.toString() }
                crashreportcategory.addDetail("Item Class") { stack.item.javaClass.name }
                crashreportcategory.addDetail(
                    "Item name"
                ) { stack.displayName }
                throw ReportedException(crashreport)
            }
        }
    }

    override fun findSlotMatchingUnusedItem(stack: ItemStack): Int {
        return realMainInventory.indexOfFirst {
            it.isNotEmpty &&
                    !it.isItemDamaged &&
                    !it.isItemEnchanted &&
                    !it.hasDisplayName() &&
                    this.stackEqualExact(stack, it)
        }.let { if (it >= 0) it + this.realMainIndices.first else it }
    }

    override fun decrementAnimations() {
        super.decrementAnimations()

        eggSlot.forEach {
            if (it.isNotEmpty) (it.item as? EggItem)?.onUpdateInPetSlot(player, it, player.world, player.petStorage)
        }
    }

    override fun damageArmor(damage: Float) {
        // NOP
    }

    override fun getDestroySpeed(state: IBlockState) =
        if (this.currentItem in this.rpgHotbarIndices && !this.rpgHotbar[this.currentItem].isEmpty)
            this.rpgHotbar[this.currentItem].getDestroySpeed(state)
        else 1f

    override fun hasItemStack(itemStackIn: ItemStack) =
        allAsSequence.any { it.isNotEmpty && it.isItemEqual(itemStackIn) }

    @SideOnly(Side.CLIENT)
    override fun getSlotFor(stack: ItemStack) = this.realMainInventory.indexOfFirst {
        it.isNotEmpty && this.stackEqualExact(stack, it)
    }.let { if (it >= 0) it + this.realMainIndices.first else it }

    override fun isItemValidForSlot(index: Int, stack: ItemStack): Boolean {
        return when (index) {
            in rpgHotbarIndices -> true
            in realMainIndices -> true
            in armorIndices -> stack.item is ItemArmor
            offHandIndex -> stack.item is ItemOffHand
            eggIndex -> stack.item === EggItem
            in bagIndices -> false
            else -> false
        }
    }

    private inner class FakeMain : NonNullList<ItemStack>() {
        override fun clear() = TODO("Not implemented")
        override fun add(index: Int, element: ItemStack) = TODO("Not implemented")
        override fun removeAt(p_remove_1_: Int): ItemStack = TODO("Not implemented")

        override fun get(index: Int): ItemStack = when {
            index in rpgHotbarIndices -> rpgHotbar[index]
            player.isCreative -> realMainInventory[index - rpgHotbar.size]
            else -> skills[index - rpgHotbarIndices.last - 1]
        }

        override fun set(index: Int, element: ItemStack): ItemStack = when {
            index in rpgHotbarIndices -> rpgHotbar.set(index, element)
            player.isCreative -> realMainInventory.set(index - rpgHotbar.size, element)
            else -> element
        }

        override val size: Int
            get() = rpgHotbar.size + realMainInventory.size
    }
}