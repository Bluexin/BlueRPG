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

package be.bluexin.rpg.gear

import be.bluexin.rpg.entities.EntityRpgArrow
import be.bluexin.rpg.entities.EntityWandProjectile
import be.bluexin.rpg.stats.FixedStat
import be.bluexin.rpg.stats.GearStats
import be.bluexin.rpg.stats.SecondaryStat
import be.bluexin.rpg.stats.get
import be.bluexin.rpg.util.RNG
import be.bluexin.saomclib.onServer
import com.teamwizardry.librarianlib.features.base.item.ItemModBow
import com.teamwizardry.librarianlib.features.kotlin.localize
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.SharedMonsterAttributes
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.SoundEvents
import net.minecraft.inventory.EntityEquipmentSlot
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.stats.StatList
import net.minecraft.util.ActionResult
import net.minecraft.util.EnumActionResult
import net.minecraft.util.EnumHand
import net.minecraft.util.SoundCategory
import net.minecraft.world.World
import kotlin.math.max

class ItemRangedWeapon private constructor(override val type: RangedWeaponType) : ItemModBow(type.key), IRPGGear {

    companion object {
        private val pieces = Array(RangedWeaponType.values().size) { typeIdx ->
            val type = RangedWeaponType.values()[typeIdx]
            ItemRangedWeapon(type)
        }

        operator fun get(type: RangedWeaponType) = pieces[type.ordinal]
    }

    override fun getAttributeModifiers(slot: EntityEquipmentSlot, stack: ItemStack) =
            super<IRPGGear>.getAttributeModifiers(slot, stack)

    override fun initCapabilities(stack: ItemStack, nbt: NBTTagCompound?) =
            super<IRPGGear>.initCapabilities(stack, nbt)

    override fun addInformation(stack: ItemStack, worldIn: World?, tooltip: MutableList<String>, flagIn: ITooltipFlag) =
            super<IRPGGear>.addInformation(stack, worldIn, tooltip, flagIn)

    override fun getNBTShareTag(stack: ItemStack): NBTTagCompound? =
            super.addNBTShare(stack, super.getNBTShareTag(stack) ?: NBTTagCompound())

    override fun readNBTShareTag(stack: ItemStack, nbt: NBTTagCompound?) {
        super.readNBTShareTag(stack, nbt)
        if (nbt != null) super.readNBTShare(stack, nbt)
    }

    override fun onItemRightClick(worldIn: World, playerIn: EntityPlayer, handIn: EnumHand): ActionResult<ItemStack> {
        val r = super<IRPGGear>.onItemRightClick(worldIn, playerIn, handIn)
        return if (r.type == EnumActionResult.PASS) {
            val itemstack = playerIn.getHeldItem(handIn)
            val ret = net.minecraftforge.event.ForgeEventFactory.onArrowNock(itemstack, worldIn, playerIn, handIn, true)
            if (ret != null) return ret
            playerIn.activeHand = handIn
            ActionResult(EnumActionResult.SUCCESS, itemstack)
        } else r
    }

    override fun tooltipizeFixedStats(stats: GearStats) =
            sequenceOf("rpg.tooltip.fstat".localize(
                    FixedStat.values().asSequence().filter { stats[it] != 0 }.joinToString(separator = "-") {
                        it.localize(stats[it])
                    },
                    FixedStat.BASE_DAMAGE.longName()
            ))

    override fun getItemStackDisplayName(stack: ItemStack) = super<IRPGGear>.getItemStackDisplayName(stack)

    override fun getUnlocalizedNameInefficientlyTrick(stack: ItemStack): String =
            super.getUnlocalizedNameInefficiently(stack)

    override fun getShareTag(): Boolean {
        return true
    }

    override val item: Item
        get() = this

    override val gearSlot: EntityEquipmentSlot
        get() = EntityEquipmentSlot.MAINHAND

    override fun onPlayerStoppedUsing(stack: ItemStack, worldIn: World, entityLiving: EntityLivingBase, timeLeft: Int) {
        if (entityLiving is EntityPlayer) {
            var i = this.getMaxItemUseDuration(stack) - timeLeft
            i = net.minecraftforge.event.ForgeEventFactory.onArrowLoose(stack, worldIn, entityLiving, i, true)
            if (i < 0) return

            val f = getArrowVelocity(i)

            if (f.toDouble() >= 0.1) {
                if (type == RangedWeaponType.BOW) {
                    worldIn onServer {
                        val entity = EntityRpgArrow(worldIn, entityLiving)
                        entity.shoot(entityLiving, entityLiving.rotationPitch, entityLiving.rotationYaw, 0.0f, f * 3.0f, 1.0f)

                        if (f == 1.0f) entity.isCritical = true

                        var damage = entityLiving.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).attributeValue
                        val minD = entityLiving[FixedStat.BASE_DAMAGE]
                        val maxD = entityLiving[FixedStat.MAX_DAMAGE]
                        val r = max(1.0, maxD - minD)
                        damage += (if (minD == maxD) minD else RNG.nextDouble() * r + minD)
                        if (RNG.nextDouble() <= entityLiving[SecondaryStat.CRIT_CHANCE]) {
                            damage *= 1.0 + entityLiving[SecondaryStat.CRIT_DAMAGE]
                        }
                        damage *= 1.0 + entityLiving[SecondaryStat.INCREASED_DAMAGE]
                        entity.damage = damage * f
                        val kb = entityLiving[WeaponAttribute.KNOCKBACK]
                        if (kb > 0) entity.setKnockbackStrength(kb.toInt() * 5)
                        stack.damageItem(1, entityLiving)

                        worldIn.spawnEntity(entity)
                    }

                    worldIn.playSound(null as EntityPlayer?, entityLiving.posX, entityLiving.posY, entityLiving.posZ, SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 1.0f, 1.0f / (Item.itemRand.nextFloat() * 0.4f + 1.2f) + f * 0.5f)
                    entityLiving.addStat(StatList.getObjectUseStats(this)!!)
                } else {
                    worldIn onServer {
                        val entity = EntityWandProjectile(worldIn, entityLiving)
                        entity.shoot(entityLiving, entityLiving.rotationPitch, entityLiving.rotationYaw, 0.0f, f * 1.5f, 1.0f)

                        var damage = entityLiving.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).attributeValue
                        val minD = entityLiving[FixedStat.BASE_DAMAGE]
                        val maxD = entityLiving[FixedStat.MAX_DAMAGE]
                        val r = max(1.0, maxD - minD)
                        damage += (if (minD == maxD) minD else RNG.nextDouble() * r + minD)
                        if (RNG.nextDouble() <= entityLiving[SecondaryStat.CRIT_CHANCE]) {
                            damage *= 1.0 + entityLiving[SecondaryStat.CRIT_DAMAGE]
                        }
                        damage *= 1.0 + entityLiving[SecondaryStat.INCREASED_DAMAGE]
                        entity.damage = damage * f
                        val kb = entityLiving[WeaponAttribute.KNOCKBACK]
//                    if (kb > 0) entity.setKnockbackStrength(kb.toInt() * 5)
                        stack.damageItem(1, entityLiving)

                        worldIn.spawnEntity(entity)
                    }
                }
            }
        }
    }
}