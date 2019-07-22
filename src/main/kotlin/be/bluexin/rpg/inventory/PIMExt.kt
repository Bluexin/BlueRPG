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

import be.bluexin.rpg.devutil.IUsable
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.ItemStack
import net.minecraft.server.management.PlayerInteractionManager
import net.minecraft.util.ActionResult
import net.minecraft.util.EnumActionResult
import net.minecraft.util.EnumHand
import net.minecraft.world.GameType
import net.minecraft.world.World
import net.minecraftforge.items.ItemHandlerHelper

fun PlayerInteractionManager.processItemUse(
    player: EntityPlayerMP,
    worldIn: World,
    stack: ItemStack,
    hand: EnumHand
): EnumActionResult {
    when {
        this.gameType == GameType.SPECTATOR -> return EnumActionResult.PASS
        player.cooldownTracker.hasCooldown(stack.item) -> return EnumActionResult.PASS
        else -> {
            val i = stack.count
            val j = stack.metadata
            val copyBeforeUse = stack.copy()
            val actionresult = stack.use(worldIn, player)
            val itemstack = actionresult.result

            return if (itemstack === stack && itemstack.count == i && itemstack.maxItemUseDuration <= 0 && itemstack.metadata == j) {
                actionresult.type
            } else if (actionresult.type == EnumActionResult.FAIL && itemstack.maxItemUseDuration > 0 && !player.isHandActive) {
                actionresult.type
            } else {
                if (this.isCreative) {
                    itemstack.count = i
                    if (itemstack.isItemStackDamageable) itemstack.itemDamage = j
                }

                if (itemstack.isEmpty) net.minecraftforge.event.ForgeEventFactory.onPlayerDestroyItem(
                    player,
                    copyBeforeUse,
                    hand
                )
                if (!stack.isItemEqual(itemstack)) ItemHandlerHelper.giveItemToPlayer(player, itemstack)

                actionresult.type
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
fun ItemStack.use(worldIn: World, playerIn: EntityPlayer): ActionResult<ItemStack> {
    return (item as? IUsable<ItemStack>)?.invoke(this, worldIn, playerIn)
        ?: ActionResult.newResult(EnumActionResult.FAIL, this)
}