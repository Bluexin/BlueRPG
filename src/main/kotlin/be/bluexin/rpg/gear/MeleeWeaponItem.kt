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

package be.bluexin.rpg.gear

import be.bluexin.rpg.DamageHandler
import be.bluexin.rpg.devutil.set
import be.bluexin.rpg.stats.FixedStat
import be.bluexin.rpg.stats.GearStats
import be.bluexin.rpg.stats.stats
import be.bluexin.saomclib.onServer
import com.google.common.collect.Multimap
import com.teamwizardry.librarianlib.features.base.item.ItemModSword
import com.teamwizardry.librarianlib.features.helpers.getBoolean
import com.teamwizardry.librarianlib.features.kotlin.localize
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.entity.Entity
import net.minecraft.entity.ai.attributes.AttributeModifier
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.EntityEquipmentSlot
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.ActionResult
import net.minecraft.util.EnumActionResult
import net.minecraft.util.EnumHand
import net.minecraft.world.World

class MeleeWeaponItem private constructor(override val type: MeleeWeaponType) :
    ItemModSword(type.key, ToolMaterial.IRON), IRPGGear {
    // Could use EntityPlayer.REACH_DISTANCE and SharedMonsterAttributes.ATTACK_SPEED attributes to implement mechanics
    companion object {
        private val pieces = Array(MeleeWeaponType.values().size) { typeIdx ->
            val type = MeleeWeaponType.values()[typeIdx]
            MeleeWeaponItem(type)
        }

        operator fun get(type: MeleeWeaponType) = pieces[type.ordinal]
    }

    override fun getAttributeModifiers(
        slot: EntityEquipmentSlot,
        stack: ItemStack
    ): Multimap<String, AttributeModifier> {
        val m = super<IRPGGear>.getAttributeModifiers(slot, stack)

        if (stack.stats?.generated == true) for ((stat, value) in type.attributes) {
            m[stat.attribute.name] = AttributeModifier(stat.uuid[0], stat.attribute.name, value, stat.operation.op)
        }

        return m
    }

    override fun initCapabilities(stack: ItemStack, nbt: NBTTagCompound?) =
        super<IRPGGear>.initCapabilities(stack, nbt)

    override fun addInformation(stack: ItemStack, worldIn: World?, tooltip: MutableList<String>, flagIn: ITooltipFlag) =
        super<IRPGGear>.addInformation(stack, worldIn, tooltip, flagIn)

    override fun getNBTShareTag(stack: ItemStack): NBTTagCompound? =
        super.addNBTShare(stack, super.getNBTShareTag(stack) ?: NBTTagCompound())

    override fun readNBTShareTag(stack: ItemStack, nbt: NBTTagCompound?) =
        super.readNBTShareTag(stack, super.readNBTShare(stack, nbt))

    override fun getShareTag() = super<IRPGGear>.getShareTag()

    override fun onItemRightClick(worldIn: World, playerIn: EntityPlayer, handIn: EnumHand): ActionResult<ItemStack> {
        val r = super<IRPGGear>.onItemRightClick(worldIn, playerIn, handIn)
        return if (r.type == EnumActionResult.PASS) super<ItemModSword>.onItemRightClick(
            worldIn,
            playerIn,
            handIn
        ) else r
    }

    override fun getItemStackDisplayName(stack: ItemStack) = super<IRPGGear>.getItemStackDisplayName(stack)

    override fun getUnlocalizedNameInefficientlyTrick(stack: ItemStack): String =
        super.getUnlocalizedNameInefficiently(stack)

    override fun tooltipizeFixedStats(stats: GearStats) =
        sequenceOf("rpg.tooltip.fstat"
            .localize(
                FixedStat.values().asSequence().filter { stats[it] != 0 }.joinToString(separator = "-") {
                    it.localize(stats[it])
                },
                FixedStat.BASE_DAMAGE.longName()
            )
        )

    override fun getMaxDamage(stack: ItemStack) = super<IRPGGear>.getMaxDamage(stack)

    override fun setDamage(stack: ItemStack, damage: Int) {
        if (stack.stats?.generated == true) super.setDamage(stack, damage)
    }

    override fun onLeftClickEntity(stack: ItemStack, player: EntityPlayer, entity: Entity): Boolean {
        player.world.onServer {
            val stats = stack.stats!!
            if (stats.generated) {
                if (stats.bound == null && stats.binding == Binding.BOE) stats.bindTo(player)
                val t = player.entityData
                if (t.getBoolean("bluerpg:customAttackProcessing", false)) {
                    val lastTime = t.getLong("bluerpg:weapontime")
                    if (lastTime != player.world.totalWorldTime) {
                        t.setLong("bluerpg:weapontime", player.world.totalWorldTime)
                        t.setFloat("bluerpg:lastweaponcd", player.getCooledAttackStrength(0f))
                    }
                    return super.onLeftClickEntity(stack, player, entity)
                } else DamageHandler.handleCustomAttack(player)
            }
        }
        return true
    }

    override val item: Item
        get() = this

    override val gearSlot: EntityEquipmentSlot
        get() = EntityEquipmentSlot.MAINHAND

    override fun createEntity(world: World, location: Entity, itemstack: ItemStack) =
        super<IRPGGear>.createEntity(world, location, itemstack)

    override fun hasCustomEntity(stack: ItemStack) = super<IRPGGear>.hasCustomEntity(stack)
}