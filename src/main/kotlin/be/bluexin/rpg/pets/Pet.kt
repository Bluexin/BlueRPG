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

package be.bluexin.rpg.pets

import be.bluexin.rpg.util.createEnumKey
import be.bluexin.saomclib.profile
import com.teamwizardry.librarianlib.features.base.entity.LivingEntityMod
import com.teamwizardry.librarianlib.features.kotlin.Minecraft
import com.teamwizardry.librarianlib.features.kotlin.createCompoundKey
import com.teamwizardry.librarianlib.features.kotlin.managedValue
import com.teamwizardry.librarianlib.features.saving.Save
import moe.plushie.armourers_workshop.client.config.ConfigHandlerClient
import moe.plushie.armourers_workshop.client.render.SkinPartRenderer
import moe.plushie.armourers_workshop.client.render.SkinRenderData
import moe.plushie.armourers_workshop.client.skin.cache.ClientSkinCache
import moe.plushie.armourers_workshop.common.skin.data.SkinDescriptor
import net.minecraft.client.Minecraft
import net.minecraft.client.model.ModelBase
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.entity.RenderLiving
import net.minecraft.client.renderer.entity.RenderManager
import net.minecraft.entity.Entity
import net.minecraft.entity.IEntityOwnable
import net.minecraft.entity.SharedMonsterAttributes
import net.minecraft.entity.ai.EntityAILookIdle
import net.minecraft.entity.ai.EntityAIWatchClosest
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.MathHelper
import net.minecraft.world.World
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import org.lwjgl.opengl.GL11
import java.util.*

class EntityPet(worldIn: World) : LivingEntityMod(worldIn), IEntityOwnable {
    private companion object {
        private val SKIN_DATA = EntityPet::class.createCompoundKey()
        private val MOVEMENT_TYPE_DATA = EntityPet::class.createEnumKey<PetMovementType>()
    }

    @Save
    private var playerUUID: UUID? = null

    @Save
    internal var skinData by managedValue(SKIN_DATA)
    @Save
    internal var movementType by managedValue(MOVEMENT_TYPE_DATA)

    val isJumping get() = super.isJumping

    var skinPointer: SkinDescriptor? = null
        set(value) {
            field = value
            val tag = NBTTagCompound()
            value?.writeToCompound(tag)
            skinData = tag
        }
        get() {
            if (field == null && !skinData.isEmpty) {
                field = SkinDescriptor().apply { this.readFromCompound(skinData) }
            }
            return field
        }

    val movementHandler: PetMovementHandler by lazy { movementType(this) }

    init {
        setSize(.8f, .8f)
    }

    override fun onAddedToWorld() {
        super.onAddedToWorld()
        moveHelper = movementHandler.createMoveHelper()
        jumpHelper = movementHandler.createJumpHelper()
        this.movementHandler.registerAI() // Don't use #initEntityAI because our fields aren't initialized yet u_u
    }

    override fun entityInit() {
        super.entityInit()
        this.getDataManager().register(SKIN_DATA, NBTTagCompound())
        this.getDataManager().register(MOVEMENT_TYPE_DATA, PetMovementType.BOUNCE)
    }

    override fun applyEntityAttributes() {
        super.applyEntityAttributes()
        this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).baseValue = 35.0
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).baseValue = 0.45
        this.getEntityAttribute(SharedMonsterAttributes.ARMOR).baseValue = 2.0
    }

    override fun initEntityAI() {
        this.tasks.addTask(6, EntityAIFollowOwner(this, 1.0, 5f, 3f))
        this.tasks.addTask(10, EntityAIWatchClosest(this, EntityPlayer::class.java, 8.0f))
        this.tasks.addTask(10, EntityAILookIdle(this))
    }

    override fun onLivingUpdate() {
        super.onLivingUpdate()

        this.movementHandler.onLivingUpdate()
    }

    fun setOwner(player: EntityPlayer) {
        playerUUID = player.gameProfile.id
    }

    override fun updateAITasks() = this.movementHandler.updateAITasks()

    override fun getOwner() = if (this.playerUUID != null) world.getPlayerEntityByUUID(this.playerUUID!!) else null

    override fun getOwnerId() = this.playerUUID

    override fun getJumpUpwardsMotion() = this.movementHandler.getJumpUpwardsMotion()

    override fun jump() {
        super.jump()
        this.movementHandler.jump()
    }
}

@SideOnly(Side.CLIENT)
class RenderPet(renderManager: RenderManager) : RenderLiving<EntityPet>(renderManager, ModelPet(), .8f) {
    override fun getEntityTexture(entity: EntityPet): ResourceLocation? = null

    override fun bindEntityTexture(entity: EntityPet) = true

    override fun handleRotationFloat(pet: EntityPet, partialTicks: Float) =
        pet.movementHandler.handleRotationFloat(partialTicks)

    override fun preRenderCallback(pet: EntityPet, partialTicks: Float) =
        pet.movementHandler.preRenderCallback(partialTicks)
}

@SideOnly(Side.CLIENT)
class ModelPet : ModelBase() {

    override fun render(
        entityIn: Entity,
        limbSwing: Float,
        limbSwingAmount: Float,
        ageInTicks: Float,
        netHeadYaw: Float,
        headPitch: Float,
        scale: Float
    ) {
        this.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entityIn)

        Minecraft().profile("Render BlueRPG AW Pet") {
            val skinPointer = (entityIn as? EntityPet)?.skinPointer ?: return
            val distanceSquared = Minecraft.getMinecraft().player.getDistanceSq(
                entityIn.posX,
                entityIn.posY,
                entityIn.posZ
            )
            if (distanceSquared > Math.pow(ConfigHandlerClient.renderDistanceSkin.toDouble(), 2.0)) return

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
                GL11.glTranslated(
                    offset.x.toDouble(),
                    -offset.y.toDouble() + 1.45,
                    offset.z.toDouble()
                ) // y + 1.45 for current head skins to be floor-aligned
                SkinPartRenderer.INSTANCE.renderPart(
                    SkinRenderData(
                        partData,
                        scale,
                        skinDye,
                        null,
                        MathHelper.sqrt(distanceSquared).toInt(),
                        true,
                        null
                    )
                )
                GlStateManager.resetColor()
                GlStateManager.color(1f, 1f, 1f, 1f)
                GL11.glDisable(GL11.GL_CULL_FACE)
                GL11.glPopMatrix()

                break
            }
        }
    }
}