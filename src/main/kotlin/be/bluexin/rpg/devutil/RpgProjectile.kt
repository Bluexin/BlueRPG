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

package be.bluexin.rpg.devutil

import be.bluexin.rpg.skills.TargetWithLookVec
import net.minecraft.client.renderer.entity.Render
import net.minecraft.client.renderer.entity.RenderManager
import net.minecraft.entity.Entity
import net.minecraft.entity.IProjectile
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

interface RpgProjectile : IProjectile {
    var computedDamage: Double
    var knockback: Int
    fun realShoot(shooter: TargetWithLookVec, pitchOffset: Float, velocity: Float, inaccuracy: Float)
}

@SideOnly(Side.CLIENT)
abstract class RpgProjectileRender<T : Entity>(renderManager: RenderManager) : Render<T>(renderManager) {
    override fun doRender(entity: T, x: Double, y: Double, z: Double, entityYaw: Float, partialTicks: Float) = Unit

    override fun getEntityTexture(entity: T): ResourceLocation? = null
}