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

import be.bluexin.rpg.devutil.createEnumKey
import be.bluexin.rpg.skills.glitter.PacketGlitter
import be.bluexin.rpg.stats.SecondaryStat
import be.bluexin.rpg.stats.get
import be.bluexin.saomclib.onServer
import be.bluexin.saomclib.profile
import com.teamwizardry.librarianlib.features.base.entity.LivingEntityMod
import com.teamwizardry.librarianlib.features.config.ConfigDoubleRange
import com.teamwizardry.librarianlib.features.config.ConfigProperty
import com.teamwizardry.librarianlib.features.kotlin.Client
import com.teamwizardry.librarianlib.features.kotlin.createCompoundKey
import com.teamwizardry.librarianlib.features.kotlin.managedValue
import com.teamwizardry.librarianlib.features.network.PacketHandler
import com.teamwizardry.librarianlib.features.network.TargetWatchingEntity
import com.teamwizardry.librarianlib.features.saving.Save
import moe.plushie.armourers_workshop.client.config.ConfigHandlerClient
import moe.plushie.armourers_workshop.client.render.SkinPartRenderData
import moe.plushie.armourers_workshop.client.render.SkinPartRenderer
import moe.plushie.armourers_workshop.client.render.SkinRenderData
import moe.plushie.armourers_workshop.client.skin.cache.ClientSkinCache
import moe.plushie.armourers_workshop.common.skin.data.SkinDescriptor
import net.minecraft.client.Minecraft
import net.minecraft.client.model.ModelBase
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.entity.RenderLiving
import net.minecraft.client.renderer.entity.RenderManager
import net.minecraft.entity.*
import net.minecraft.entity.ai.EntityAILookIdle
import net.minecraft.entity.ai.EntityAIWatchClosest
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.MobEffects
import net.minecraft.init.SoundEvents
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumHand
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.MathHelper
import net.minecraft.world.World
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import org.lwjgl.opengl.GL11
import java.util.*
import kotlin.math.pow

@ConfigDoubleRange(.0, java.lang.Double.MAX_VALUE)
@ConfigProperty("general", "Pet mount speed general multiplier")
var petMountSpeed = 1.0
    internal set

class PetEntity(worldIn: World) : LivingEntityMod(worldIn), IEntityOwnable, IJumpingMount {
    private companion object {
        private val SKIN_DATA = PetEntity::class.createCompoundKey()
        private val MOVEMENT_TYPE_DATA = PetEntity::class.createEnumKey<PetMovementType>()
    }

    @Save
    private var playerUUID: UUID? = null

    @Save
    internal var skinData by managedValue(SKIN_DATA)
    @Save
    internal var movementType by managedValue(MOVEMENT_TYPE_DATA)

    private var jumpPower = 0f
    private var mountJumping = false

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
        aiMoveSpeed = .8f
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
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).baseValue = 0.62
        this.getEntityAttribute(SharedMonsterAttributes.ARMOR).baseValue = 2.0
    }

    override fun initEntityAI() {
        super.initEntityAI()
        this.tasks.addTask(6, EntityAIFollowOwner(this, 1.0, 5f, 3f))
        this.tasks.addTask(10, EntityAIWatchClosest(this, EntityPlayer::class.java, 8.0f))
        this.tasks.addTask(10, EntityAILookIdle(this))
    }

    override fun onLivingUpdate() {
        this.world onServer {
            val owner = this.owner
            if (owner == null || owner.petStorage.petEntity != this) this.setDead()
        }

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

    override fun processInteract(player: EntityPlayer, hand: EnumHand): Boolean {
        if (this.isBeingRidden || player.petStorage.petEntity != this) return false
        player.rotationYaw = this.rotationYaw
        player.rotationPitch = this.rotationPitch

        return player.startRiding(this)
    }

    override fun addPassenger(passenger: Entity) {
        super.addPassenger(passenger)
        passenger.world onServer {
            PacketHandler.CHANNEL.send(
                TargetWatchingEntity(this), PacketGlitter(
                    PacketGlitter.Type.AOE, this.positionVector, 0x82add2, 0x6d8396, .7
                )
            )
        }
    }

    override fun removePassenger(passenger: Entity) {
        super.removePassenger(passenger)
        passenger.world onServer {
            PacketHandler.CHANNEL.send(
                TargetWatchingEntity(this), PacketGlitter(
                    PacketGlitter.Type.AOE, this.positionVector, 0x82add2, 0x6d8396, .35
                )
            )
        }
    }

    override fun canBeSteered() = this.controllingPassenger is EntityLivingBase

    override fun getControllingPassenger(): Entity? = this.passengers.firstOrNull()

    override fun handleStartJump(p_184775_1_: Int) = Unit

    @SideOnly(Side.CLIENT)
    override fun setJumpPower(jumpPowerIn: Int) =
        if (jumpPowerIn >= 90) this.jumpPower = 1.0f
        else this.jumpPower = 0.4f + 0.4f * jumpPowerIn.toFloat() / 90.0f

    override fun canJump() = this.controllingPassenger != null

    override fun handleStopJump() = Unit

    override fun travel(_strafe: Float, vertical: Float, _forward: Float) {
        var strafe = _strafe
        var forward = _forward
        if (this.isBeingRidden && this.canBeSteered()) {
            val driver = this.controllingPassenger as EntityLivingBase
            this.rotationYaw = driver.rotationYaw
            this.prevRotationYaw = this.rotationYaw
            this.rotationPitch = driver.rotationPitch * 0.5f
            this.setRotation(this.rotationYaw, this.rotationPitch)
            this.renderYawOffset = this.rotationYaw
            this.rotationYawHead = this.renderYawOffset
            strafe = driver.moveStrafing * 0.5f
            forward = driver.moveForward

            if (forward <= 0.0f) forward *= 0.25f

            if (this.jumpPower > 0.0f && !this.mountJumping && this.onGround) {
                this.motionY = .7 * this.jumpPower

                if (this.isPotionActive(MobEffects.JUMP_BOOST)) {
                    this.motionY += ((this.getActivePotionEffect(MobEffects.JUMP_BOOST)!!.amplifier + 1).toFloat() * 0.1f).toDouble()
                }

                this.mountJumping = true
                this.isAirBorne = true

                if (forward > 0.0f) {
                    val f = MathHelper.sin(this.rotationYaw * 0.017453292f)
                    val f1 = MathHelper.cos(this.rotationYaw * 0.017453292f)
                    this.motionX += (-0.4f * f * this.jumpPower).toDouble()
                    this.motionZ += (0.4f * f1 * this.jumpPower).toDouble()
                    this.playSound(SoundEvents.ENTITY_HORSE_JUMP, 0.4f, 1.0f)
                }

                this.jumpPower = 0.0f
            }

            this.jumpMovementFactor = this.aiMoveSpeed * 0.1f

            if (this.canPassengerSteer()) {
                this.aiMoveSpeed =
                    (this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).attributeValue * driver[SecondaryStat.SPEED] * 1000 * petMountSpeed).toFloat()
                super.travel(strafe, vertical, forward)
            } else if (driver is EntityPlayer) {
                this.motionX = 0.0
                this.motionY = 0.0
                this.motionZ = 0.0
            }

            if (this.onGround) {
                this.jumpPower = 0.0f
                this.mountJumping = false
            }

            this.prevLimbSwingAmount = this.limbSwingAmount
            val d1 = this.posX - this.prevPosX
            val d0 = this.posZ - this.prevPosZ
            var f2 = MathHelper.sqrt(d1 * d1 + d0 * d0) * 4.0f

            if (f2 > 1.0f) {
                f2 = 1.0f
            }

            this.limbSwingAmount += (f2 - this.limbSwingAmount) * 0.4f
            this.limbSwing += this.limbSwingAmount
        } else {
            this.jumpMovementFactor = 0.02f
            super.travel(strafe, vertical, forward)
        }
    }
}

@SideOnly(Side.CLIENT)
class RenderPet(renderManager: RenderManager) : RenderLiving<PetEntity>(renderManager, ModelPet(), .4f) {
    override fun getEntityTexture(entity: PetEntity): ResourceLocation? = null

    override fun bindEntityTexture(entity: PetEntity) = true

    override fun handleRotationFloat(pet: PetEntity, partialTicks: Float) =
        pet.movementHandler.handleRotationFloat(partialTicks)

    override fun preRenderCallback(pet: PetEntity, partialTicks: Float) =
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

        Client.minecraft.profile("Render BlueRPG AW Pet") {
            val skinPointer = (entityIn as? PetEntity)?.skinPointer ?: return
            val distanceSquared = Minecraft.getMinecraft().player.getDistanceSq(
                entityIn.posX,
                entityIn.posY,
                entityIn.posZ
            )
            if (distanceSquared > ConfigHandlerClient.renderDistanceSkin.toDouble().pow(2.0)) return

            val data = ClientSkinCache.INSTANCE.getSkin(skinPointer) ?: return
            val skinDye = skinPointer.skinDye

            GL11.glEnable(GL11.GL_CULL_FACE)
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
            GL11.glEnable(GL11.GL_BLEND)
            for (partData in data.parts) {
                val offset = partData.partType.offset
                GL11.glPushMatrix()
                GL11.glTranslated(
                    offset.x.toDouble(),
                    -offset.y.toDouble() + 1.45,
                    offset.z.toDouble()
                ) // y + 1.45 for current head skins to be floor-aligned
                if (entityIn.isBeingRidden) GL11.glScalef(2f, 2f, 2f)
                SkinPartRenderer.INSTANCE.renderPart(
                    SkinPartRenderData(
                        partData, SkinRenderData(
                            scale,
                            skinDye,
                            null,
                            MathHelper.sqrt(distanceSquared).toDouble(),
                            true,
                            false,
                            true,
                            null
                        )
                    )
                )
                GL11.glPopMatrix()
            }
            GlStateManager.resetColor()
            GL11.glDisable(GL11.GL_CULL_FACE)
        }
    }
}