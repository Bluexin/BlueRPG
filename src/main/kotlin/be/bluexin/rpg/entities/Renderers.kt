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

package be.bluexin.rpg.entities

import net.minecraft.client.renderer.entity.Render
import net.minecraft.client.renderer.entity.RenderArrow
import net.minecraft.client.renderer.entity.RenderManager
import net.minecraft.entity.Entity
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

@SideOnly(Side.CLIENT)
class RenderRpgArrow(renderManagerIn: RenderManager) : RenderArrow<EntityRpgArrow>(renderManagerIn) {
    private val RES_ARROW = ResourceLocation("textures/entity/projectiles/arrow.png")

    override fun getEntityTexture(entity: EntityRpgArrow) = RES_ARROW
}

@SideOnly(Side.CLIENT)
abstract class RenderRpgProjectile<T: Entity>(renderManager: RenderManager) : Render<T>(renderManager) {
    override fun doRender(entity: T, x: Double, y: Double, z: Double, entityYaw: Float, partialTicks: Float) {
        // nop
    }

    override fun getEntityTexture(entity: T): ResourceLocation? {
        return null
    }
}

@SideOnly(Side.CLIENT)
class RenderWandProjectile(renderManager: RenderManager) : RenderRpgProjectile<EntityWandProjectile>(renderManager)

@SideOnly(Side.CLIENT)
class RenderSkillProjectile(renderManager: RenderManager) : RenderRpgProjectile<EntitySkillProjectile>(renderManager)
