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

import be.bluexin.rpg.gear.WeaponAttribute
import be.bluexin.rpg.stats.*
import be.bluexin.saomclib.onServer
import com.teamwizardry.librarianlib.features.kotlin.localize
import com.teamwizardry.librarianlib.features.network.PacketHandler
import com.teamwizardry.librarianlib.features.utilities.RaycastUtils
import net.minecraft.client.Minecraft
import net.minecraft.entity.ai.attributes.IAttributeInstance
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.text.TextComponentTranslation
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.event.ServerChatEvent
import net.minecraftforge.event.entity.EntityEvent
import net.minecraftforge.event.entity.living.LivingAttackEvent
import net.minecraftforge.event.entity.living.LivingDamageEvent
import net.minecraftforge.event.entity.living.LivingHurtEvent
import net.minecraftforge.event.entity.living.LivingKnockBackEvent
import net.minecraftforge.event.entity.player.PlayerInteractEvent
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
    fun hitEmpty(event: PlayerInteractEvent.LeftClickEmpty) {
        val player = event.entityPlayer
        val reach = player[WeaponAttribute.RANGE]
        val target = RaycastUtils.getEntityLookedAt(event.entityPlayer, reach)
        if (target != null) PacketHandler.NETWORK.sendToServer(PacketAttack(target.entityId))
        else {
            val aoeRadius = player[WeaponAttribute.ANGLE].toInt()
            if (aoeRadius > 0) {
                val yaw = player.rotationYaw
                for (i in (yaw.toInt() - aoeRadius / 2)..(yaw.toInt() + aoeRadius / 2) step 15) {
                    player.rotationYaw = i.toFloat()
                    val t = RaycastUtils.getEntityLookedAt(player, reach)
                    if (t != null) {
                        PacketHandler.NETWORK.sendToServer(PacketAttack(t.entityId))
                    }
                }
                player.rotationYaw = yaw
            }
        }
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