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
import be.bluexin.rpg.gear.WeaponAttribute
import be.bluexin.rpg.gear.WeaponType
import be.bluexin.rpg.stats.*
import be.bluexin.rpg.util.Resources
import be.bluexin.saomclib.onServer
import com.teamwizardry.librarianlib.features.kotlin.localize
import net.minecraft.client.Minecraft
import net.minecraft.entity.ai.attributes.IAttributeInstance
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.EntityEquipmentSlot
import net.minecraft.nbt.NBTTagByte
import net.minecraft.util.text.TextComponentTranslation
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.client.event.TextureStitchEvent
import net.minecraftforge.event.ServerChatEvent
import net.minecraftforge.event.entity.EntityEvent
import net.minecraftforge.event.entity.living.*
import net.minecraftforge.event.entity.player.CriticalHitEvent
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.fml.common.eventhandler.Event
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

object CommonEventHandler {

    @SubscribeEvent
    fun playerTick(event: TickEvent.PlayerTickEvent) {
        event.player.world onServer {
            val stats = event.player.stats
            if (stats.dirty) stats.sync()
            if (event.player.health > event.player.maxHealth) event.player.health = event.player.maxHealth
            else event.player.heal(event.player[SecondaryStat.REGEN].toFloat() / 20f)
            // TODO: same for mana
        }
    }

    @SubscribeEvent
    fun changeGear(event: LivingEquipmentChangeEvent) {
        val p = event.entityLiving as? EntityPlayer?: return

        @Suppress("NON_EXHAUSTIVE_WHEN")
        when (event.slot) {
            EntityEquipmentSlot.MAINHAND -> {
                val offHand = p.heldItemOffhand
                if (!offHand.isEmpty) {
                    val from = (event.from.item as? IRPGGear)?.type as? WeaponType
                    val to = (event.to.item as? IRPGGear)?.type as? WeaponType
                    if (to?.twoHander == true && from?.twoHander != true) {
                        offHand.setTagInfo("bluerpg:twohandflag", NBTTagByte(1))
                    } else if (from?.twoHander == true && to?.twoHander != true) {
                        offHand.setTagInfo("bluerpg:twohandflag", NBTTagByte(0))
                    }
                }
            }
            EntityEquipmentSlot.OFFHAND -> {
                val mainhand = (p.heldItemMainhand.item as? IRPGGear)?.type as? WeaponType
                val to = event.to
                if (to.item is IRPGGear) to.setTagInfo("bluerpg:twohandflag", NBTTagByte(if (mainhand?.twoHander == true) 1 else 0))
            }
        }

        p.equipmentAndArmor.forEach {
            it.setTagInfo("bluerpg:disabled", NBTTagByte(if (it.requirementMet(p)) 0 else 1))
        }
    }

    @SubscribeEvent
    fun entityConstructing(event: EntityEvent.EntityConstructing) {
        val e = event.entity
        if (e is EntityPlayer) {
            val m = e.attributeMap
            PrimaryStat.values().forEach {
                if (it.shouldRegister) m.registerAttribute(it.attribute)
            }
            SecondaryStat.values().forEach {
                if (it.shouldRegister) m.registerAttribute(it.attribute)
            }
            FixedStat.values().forEach {
                if (it.shouldRegister) m.registerAttribute(it.attribute)
            }
            WeaponAttribute.values().forEach {
                if (it.shouldRegister) m.registerAttribute(it.attribute)
            }
        }
    }

    @SubscribeEvent
    fun hitEntity(event: LivingAttackEvent) = DamageHandler(event)

    @SubscribeEvent
    fun livingHurt(event: LivingHurtEvent) = DamageHandler(event)

    @SubscribeEvent
    fun entityHit(event: LivingDamageEvent) = DamageHandler(event)

    @SubscribeEvent
    fun knockBack(event: LivingKnockBackEvent) {
        val a = event.attacker as? EntityPlayer ?: return
        event.strength += a[WeaponAttribute.KNOCKBACK].toFloat() * a.entityData.getFloat("bluerpg:lastweaponcd")
    }

    @SubscribeEvent
    fun vanillaCrit(event: CriticalHitEvent) {
        event.result = Event.Result.DENY
    }
}

@SideOnly(Side.CLIENT)
object ClientEventHandler {
    @SubscribeEvent
    fun debugOverlay(event: RenderGameOverlayEvent.Text) {
        val player = Minecraft.getMinecraft().player
        event.left.add("(temporary)")
        event.left.addAll(PrimaryStat.values().map {
            val att: IAttributeInstance? = player.getEntityAttribute(it.attribute)
            val base = att?.baseValue?.toInt() ?: 0
            "rpg.display.stat".localize(it.longName(), "$base +${(att?.attributeValue?.toInt() ?: 0) - base}")
        })
    }

    @SubscribeEvent
    fun hitEmpty(event: PlayerInteractEvent.LeftClickEmpty) = DamageHandler.handleRange(event.entityPlayer)

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    fun onTextureStitchEvent(event: TextureStitchEvent) {
        event.map.registerSprite(Resources.PARTICLE)
    }
}

@SideOnly(Side.SERVER)
object ServerEventHandler {
    @SubscribeEvent
    fun messageSent(event: ServerChatEvent) {
        // [i],(i),[item] and (item)
        val regex = "[\\[(](i|item)[])]".toRegex()
        if (event.message.contains(regex)) {
            val s = event.component.formattedText.split(regex, 2)
            val component = TextComponentTranslation(s[0])
            component.appendSibling(event.player.heldItemMainhand.textComponent)
            s.asSequence().drop(1).forEach { component.appendText(it) }
            event.component = component
        }
    }
}