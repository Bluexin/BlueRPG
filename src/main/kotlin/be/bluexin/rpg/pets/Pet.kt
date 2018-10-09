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

package be.bluexin.rpg.pets

import com.teamwizardry.librarianlib.features.base.entity.LivingEntityMod
import com.teamwizardry.librarianlib.features.kotlin.tagCompound
import com.teamwizardry.librarianlib.features.utilities.profile
import moe.plushie.armourers_workshop.client.render.SkinPartRenderer
import moe.plushie.armourers_workshop.client.skin.cache.ClientSkinCache
import moe.plushie.armourers_workshop.common.config.ConfigHandlerClient
import moe.plushie.armourers_workshop.common.skin.data.SkinDescriptor
import net.minecraft.client.Minecraft
import net.minecraft.client.model.ModelBase
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.entity.RenderLiving
import net.minecraft.client.renderer.entity.RenderManager
import net.minecraft.entity.Entity
import net.minecraft.entity.SharedMonsterAttributes
import net.minecraft.util.ResourceLocation
import net.minecraft.world.World
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import org.lwjgl.opengl.GL11

class EntityPet(worldIn: World) : LivingEntityMod(worldIn) {

    init {
        setSize(.8f, .8f)
    }

    override fun applyEntityAttributes() {
        super.applyEntityAttributes()
        this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).baseValue = 35.0
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).baseValue = 0.23000000417232513
        this.getEntityAttribute(SharedMonsterAttributes.ARMOR).baseValue = 2.0
    }

}

@SideOnly(Side.CLIENT)
class RenderPet(renderManager: RenderManager) : RenderLiving<EntityPet>(renderManager, ModelPet(), .8f) {
    override fun getEntityTexture(entity: EntityPet): ResourceLocation? = null

    override fun bindEntityTexture(entity: EntityPet) = true
}

class ModelPet : ModelBase() {
    private val skinPointer = SkinDescriptor().apply {
        this.readFromCompound(tagCompound {
            "armourersWorkshop" to tagCompound {
                "identifier" to tagCompound {
                    "skinType" to "armourers:block"
                    "globalId" to 0
                    "localId" to 276296895
                }
                "dyeData" to tagCompound { }
                "lock" to false
            }
        })
    }

    override fun render(entityIn: Entity, limbSwing: Float, limbSwingAmount: Float, ageInTicks: Float, netHeadYaw: Float, headPitch: Float, scale: Float) {
        this.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entityIn)

        skinPointer.readFromCompound(tagCompound {
            "armourersWorkshop" to tagCompound {
                "identifier" to tagCompound {
                    "skinType" to "armourers:block"
                    "globalId" to 0
                    "localId" to 276296895//-554593869
                }
                "dyeData" to tagCompound { }
                "lock" to false
            }
        })
        profile("Render BlueRPG AW Pet") {
            val distance = Minecraft.getMinecraft().player.getDistance(
                    entityIn.posX,
                    entityIn.posY,
                    entityIn.posZ)
            if (distance > ConfigHandlerClient.maxSkinRenderDistance) {
                return
            }

            val data = ClientSkinCache.INSTANCE.getSkin(skinPointer) ?: return
            val skinDye = skinPointer.skinDye

            val size = data.parts.size
            for (i in 0 until size) {
                val partData = data.parts[i]
                GL11.glPushMatrix()

                GL11.glEnable(GL11.GL_CULL_FACE)
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
                GL11.glEnable(GL11.GL_BLEND)
                val offset = partData.partType.offset
                GL11.glTranslated(offset.x.toDouble(), -offset.y.toDouble(), offset.z.toDouble())
                SkinPartRenderer.INSTANCE.renderPart(partData, scale, skinDye, null, distance, true)
                GlStateManager.resetColor()
                GlStateManager.color(1f, 1f, 1f, 1f)
                GL11.glDisable(GL11.GL_CULL_FACE)
                GL11.glPopMatrix()

                break
            }
        }
    }
}