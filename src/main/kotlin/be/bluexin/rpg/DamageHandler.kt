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

package be.bluexin.rpg

import be.bluexin.rpg.gear.IRPGGear
import be.bluexin.rpg.gear.ItemMeleeWeapon
import be.bluexin.rpg.stats.FixedStat
import be.bluexin.rpg.stats.SecondaryStat
import be.bluexin.rpg.stats.get
import be.bluexin.rpg.util.RNG
import be.bluexin.saomclib.capabilities.getPartyCapability
import com.teamwizardry.librarianlib.features.config.ConfigIntRange
import com.teamwizardry.librarianlib.features.config.ConfigProperty
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.potion.Potion
import net.minecraft.potion.PotionEffect
import net.minecraft.util.DamageSource
import net.minecraft.util.EntityDamageSource
import net.minecraft.util.EntityDamageSourceIndirect
import net.minecraft.util.text.ITextComponent
import net.minecraftforge.event.entity.living.LivingAttackEvent
import net.minecraftforge.event.entity.living.LivingDamageEvent
import net.minecraftforge.event.entity.living.LivingHurtEvent
import kotlin.math.max

object DamageHandler {

    @ConfigIntRange(0, Int.MAX_VALUE)
    @ConfigProperty("cooldowns", "(in ticks) defaults and range are skewed ;-;")
    var reflectCD = 100
        internal set

    @ConfigIntRange(0, Int.MAX_VALUE)
    @ConfigProperty("cooldowns", "(in ticks) defaults and range are skewed ;-;")
    var blockCD = 100
        internal set

    @ConfigIntRange(0, Int.MAX_VALUE)
    @ConfigProperty("cooldowns", "(in ticks) defaults and range are skewed ;-;")
    var dodgeCD = 100
        internal set

    @ConfigIntRange(0, Int.MAX_VALUE)
    @ConfigProperty("cooldowns", "(in ticks) defaults and range are skewed ;-;")
    var parryCD = 100
        internal set

    @ConfigIntRange(0, Int.MAX_VALUE)
    @ConfigProperty("cooldowns", "(in ticks) defaults and range are skewed ;-;")
    var lifeStealCD = 100
        internal set

    @ConfigIntRange(0, Int.MAX_VALUE)
    @ConfigProperty("cooldowns", "(in ticks) defaults and range are skewed ;-;")
    var manaStealCD = 100
        internal set

    @ConfigIntRange(0, Int.MAX_VALUE)
    @ConfigProperty("cooldowns", "(in ticks) defaults and range are skewed ;-;")
    var rootCD = 100
        internal set

    @ConfigIntRange(0, Int.MAX_VALUE)
    @ConfigProperty("cooldowns", "(in ticks) defaults and range are skewed ;-;")
    var slowCD = 100
        internal set

    operator fun invoke(event: LivingAttackEvent) {
        /*
        We hit entity.
        Cancel vanilla damage, apply our own with damage from weapon, crit and whatever.
         */
        val s = event.source
        if (s.trueSource is EntityPlayer && s !is RpgDamageSource) when (s) {
            is EntityDamageSourceIndirect -> {
                // TODO
            }
            is EntityDamageSource -> {
                event.isCanceled = true

                val source = RpgDamageSource(s)
                val player = source.immediateSource as EntityPlayer
                var damage = event.amount.toDouble()

                if (player.heldItemMainhand.item is ItemMeleeWeapon) {
                    val minD = player[FixedStat.BASE_DAMAGE]
                    val maxD = player[FixedStat.MAX_DAMAGE]
                    val r = max(1.0, maxD - minD)
                    damage += (if (minD == maxD) minD else RNG.nextDouble() * r + minD) * player.entityData.getFloat("bluerpg:lastweaponcd")
                }

                if (RNG.nextDouble() <= player[SecondaryStat.CRIT_CHANCE]) {
                    damage *= 1.0 + player[SecondaryStat.CRIT_DAMAGE]
                }

                damage *= 1.0 + player[SecondaryStat.INCREASED_DAMAGE]

                event.entity.attackEntityFrom(source, damage.toFloat())
            }
        }
    }

    operator fun invoke(event: LivingHurtEvent) {
        val attacker = event.source.trueSource as? EntityPlayer ?: return
        val target = event.entityLiving as? EntityPlayer ?: return

        if (attacker.getPartyCapability().party?.isMember(target) == true) event.isCanceled = true
    }

    operator fun invoke(event: LivingDamageEvent) {
        /*
        An entity was hit.
        Change damage based on armor, dodge and whatnot.
        As well as life/mana steal.
         */
        // TODO: life/mana steal on non-players
        val target = event.entityLiving
        if (!event.source.isUnblockable && target is EntityPlayer) {
            val attacker = event.source.trueSource as? EntityLivingBase
            val tags = target.entityData
            val time = target.world.totalWorldTime

            if (tags.getLong("bluerpg:reflectcd") <= time - reflectCD && RNG.nextDouble() <= target[SecondaryStat.REFLECT]) {
                event.isCanceled = true
                tags.setLong("bluerpg:reflectcd", time)
                attacker?.attackEntityFrom(
                        DamageSource.causeThornsDamage(target), event.amount
                )
                return
            }

            val mainhand = target.heldItemMainhand.item as? IRPGGear
            val offhand = target.heldItemOffhand.item as? IRPGGear

            if ((mainhand?.type?.allowBlock == true || offhand?.type?.allowBlock == true) && tags.getLong("bluerpg:blockcd") <= time - blockCD && RNG.nextDouble() <= target[SecondaryStat.BLOCK]) {
                event.isCanceled = true
                tags.setLong("bluerpg:blockcd", time)
                val pct = event.amount / target.maxHealth
                if (pct >= 0.25f) {
                    target.addPotionEffect(PotionEffect(slow, 60, 1))
                } else if (pct >= 0.05f) {
                    target.addPotionEffect(PotionEffect(slow, 40, 0))
                }
                return
            }

            if (tags.getLong("bluerpg:dodgecd") <= time - dodgeCD && RNG.nextDouble() <= target[SecondaryStat.DODGE]) {
                event.isCanceled = true
                tags.setLong("bluerpg:dodgecd", time)
                return
            }

            var damage = event.amount.toDouble()

            if ((mainhand?.type?.allowBlock == true || offhand?.type?.allowBlock == true) && tags.getLong("bluerpg:parrycd") <= time - parryCD && RNG.nextDouble() <= target[SecondaryStat.PARRY]) {
                event.isCanceled = true
                tags.setLong("bluerpg:parrycd", time)
                damage *= 0.75
            }

            damage *= 1.0 - target[FixedStat.ARMOR]

            if (attacker is EntityPlayer) {
                damage *= 1.0 - target[SecondaryStat.RESISTANCE]
                val atags = attacker.entityData

                if (atags.getLong("bluerpg:lifestealcd") <= time - lifeStealCD && RNG.nextDouble() <= attacker[SecondaryStat.LIFE_STEAL_CHANCE]) {
                    attacker.heal((damage * attacker[SecondaryStat.LIFE_STEAL]).toFloat())
                    atags.setLong("bluerpg:lifestealcd", time)
                }

                if (atags.getLong("bluerpg:manastealcd") <= time - manaStealCD && RNG.nextDouble() <= attacker[SecondaryStat.MANA_LEECH_CHANCE]) {
                    // TODO: steal mana
                    atags.setLong("bluerpg:manastealcd", time)
                }

                if (atags.getLong("bluerpg:rootcd") <= time - rootCD && RNG.nextDouble() <= attacker[SecondaryStat.ROOT]) {
                    target.addPotionEffect(PotionEffect(slow, 60, 98))
                    atags.setLong("bluerpg:rootcd", time)
                }

                if (atags.getLong("bluerpg:slowcd") <= time - slowCD && RNG.nextDouble() <= attacker[SecondaryStat.SLOW]) {
                    target.addPotionEffect(PotionEffect(slow, 100, 1))
                    atags.setLong("bluerpg:slowcd", time)
                }
            }

            event.amount = damage.toFloat()
        }
    }

    private val slow by lazy { Potion.getPotionFromResourceLocation("slowness")!! }

    class RpgDamageSource(private val original: EntityDamageSource) : EntityDamageSource(original.damageType, original.immediateSource) {
        override fun setDamageAllowedInCreativeMode(): DamageSource = original.setDamageAllowedInCreativeMode()
        override fun getIsThornsDamage() = original.isThornsDamage
        override fun getImmediateSource() = original.immediateSource
        override fun isDifficultyScaled() = original.isDifficultyScaled
        override fun getDamageLocation() = original.damageLocation
        override fun setExplosion(): DamageSource = original.setExplosion()
        override fun isUnblockable() = original.isUnblockable
        override fun isFireDamage() = original.isFireDamage
        override fun setMagicDamage(): DamageSource = original.setMagicDamage()
        override fun isCreativePlayer() = original.isCreativePlayer
        override fun isProjectile() = original.isProjectile
        override fun isExplosion() = original.isExplosion
        override fun getDeathMessage(entityLivingBaseIn: EntityLivingBase): ITextComponent = original.getDeathMessage(entityLivingBaseIn)
        override fun setDamageIsAbsolute(): DamageSource = original.setDamageIsAbsolute()
        override fun canHarmInCreative() = original.canHarmInCreative()
        override fun isDamageAbsolute() = original.isDamageAbsolute
        override fun getTrueSource() = original.trueSource
        override fun setIsThornsDamage(): EntityDamageSource = original.setIsThornsDamage()
        override fun setDamageBypassesArmor(): DamageSource = original.setDamageBypassesArmor()
        override fun getDamageType(): String = original.getDamageType()
        override fun setDifficultyScaled(): DamageSource = original.setDifficultyScaled()
        override fun setProjectile(): DamageSource = original.setProjectile()
        override fun getHungerDamage() = original.hungerDamage
        override fun isMagicDamage() = original.isMagicDamage
        override fun setFireDamage(): DamageSource = original.setFireDamage()
    }
}