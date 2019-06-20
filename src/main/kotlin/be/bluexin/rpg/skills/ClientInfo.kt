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
import be.bluexin.rpg.skills.glitter.GlitterPacket
import net.minecraft.util.ResourceLocation

/**
 * Callbacks for on-cast events.
 * If `total == 0`, it means this skill is instantaneous.
 * If `current > 0`, it means the user stopped casting.
 */
data class CastInfo<T : Trigger>(
    val glitterPacket: ((T, SkillContext, current: Int, total: Int) -> GlitterPacket?)?
)

/**
 * Callbacks for targeting events
 */
data class TargetingInfo<T : Targeting>(
    val glitterPacket: ((T, SkillContext, from: Target) -> GlitterPacket?)?
)

/**
 * Client info for projectiles
 */
data class ProjectileInfo(
    val color1: Int = 0,
    val color2: Int = 0,
    val trailSystem: ResourceLocation = ResourceLocation(BlueRPG.MODID, "none")
)

/**
 * Callbacks for on hit events
 */
data class OnHitInfo(
    val glitterPacket: ((SkillContext, from: Target, to: Target) -> GlitterPacket?)?
)

operator fun <T : Trigger> CastInfo<T>?.invoke(trigger: T, context: SkillContext, current: Int, total: Int) {
    this?.glitterPacket?.invoke(trigger, context, current, total)?.send(context.caster)
}

operator fun <T : Targeting> TargetingInfo<T>?.invoke(trigger: T, context: SkillContext, from: Target) {
    this?.glitterPacket?.invoke(trigger, context, from)?.send(context.caster)
}

operator fun OnHitInfo?.invoke(context: SkillContext, from: Target, to: Target) {
    this?.glitterPacket?.invoke(context, from, to)?.send(context.caster)
}