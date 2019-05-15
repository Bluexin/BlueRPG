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

package be.bluexin.rpg.skills

import be.bluexin.rpg.BlueRPG
import be.bluexin.rpg.PacketCooldown
import be.bluexin.rpg.util.get
import be.bluexin.saomclib.capabilities.AbstractEntityCapability
import be.bluexin.saomclib.capabilities.Key
import be.bluexin.saomclib.onServer
import com.teamwizardry.librarianlib.features.network.PacketHandler
import com.teamwizardry.librarianlib.features.saving.*
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.MathHelper
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.CapabilityInject

@SaveInPlace
class CooldownCapability : AbstractEntityCapability() {
    private val cooldowns = mutableMapOf<SkillData, Cooldown>()

    @Suppress("unused")
    private var _cooldowns: Map<ResourceLocation, Cooldown>
        @SaveMethodGetter("cooldowns") get() = cooldowns.mapKeys { it.key.key }
        @SaveMethodSetter("cooldowns") set(value) {
            cooldowns.clear()
            cooldowns += value.mapKeys { SkillRegistry[it.key]!! }
        }

    @Save
    private var ticks: Int = 0

    operator fun contains(skillIn: SkillData) = this[skillIn] > 0f

    operator fun get(skillIn: SkillData, partialTicks: Float = 0f): Float {
        val cd = this.cooldowns[skillIn] ?: return 0f

        val f = cd.expireTicks - cd.createTicks
        val f1 = cd.expireTicks - this.ticks + partialTicks
        return MathHelper.clamp(f1 / f, 0f, 1f)
    }

    fun tick() {
        ++this.ticks
        if (!(player ?: return).world.isRemote && this.cooldowns.isNotEmpty()) this.cooldowns.entries.removeIf {
            if (it.value.expireTicks <= this.ticks) {
                notifyOnRemove(it.key)
                true
            } else false
        }
    }

    operator fun set(skillIn: SkillData, ticksIn: Int) {
        this.cooldowns[skillIn] = Cooldown(this.ticks, this.ticks + ticksIn)
        this.notifyOnSet(skillIn, ticksIn)
    }

    operator fun minusAssign(skillIn: SkillData) {
        this.cooldowns -= skillIn
        this.notifyOnRemove(skillIn)
    }

    private fun notifyOnSet(skillIn: SkillData, ticksIn: Int) {
        val p = player ?: return
        p.world onServer {
            PacketHandler.NETWORK.sendTo(PacketCooldown(skillIn, ticksIn), p as EntityPlayerMP)
        }
    }

    private fun notifyOnRemove(skillIn: SkillData) {
        val p = player ?: return
        p.world onServer {
            PacketHandler.NETWORK.sendTo(PacketCooldown(skillIn, 0), p as EntityPlayerMP)
        }
    }

    private val player get() = reference.get() as? EntityPlayer

    companion object {
        @Key
        val KEY = ResourceLocation(BlueRPG.MODID, "cooldowns")

        @CapabilityInject(CooldownCapability::class)
        lateinit var Capability: Capability<CooldownCapability>
            internal set
    }

    @Savable
    data class Cooldown(val createTicks: Int, val expireTicks: Int)
}

val EntityPlayer.cooldowns get() = this.getCapability(CooldownCapability.Capability, null)!!
