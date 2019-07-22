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

package be.bluexin.rpg.gear

import be.bluexin.rpg.devutil.RpgProjectile
import be.bluexin.rpg.skills.TargetWithCollision
import be.bluexin.rpg.skills.TargetWithLookVec
import be.bluexin.rpg.skills.TargetWithMovement
import be.bluexin.saomclib.onClient
import com.teamwizardry.librarianlib.features.base.entity.ArrowEntityMod
import com.teamwizardry.librarianlib.features.saving.Savable
import com.teamwizardry.librarianlib.features.saving.Save
import net.minecraft.client.renderer.entity.RenderArrow
import net.minecraft.client.renderer.entity.RenderManager
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation
import net.minecraft.world.World
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

@Savable
class RpgArrowEntity : ArrowEntityMod,
    RpgProjectile {

    @Save
    var initialX = 0.0
    @Save
    var initialY = 0.0
    @Save
    var initialZ = 0.0

    @Suppress("unused")
    constructor(world: World) : super(world)

    constructor(world: World, shooter: EntityLivingBase) : super(world, shooter)

    init {
        pickupStatus = PickupStatus.DISALLOWED
    }

    override fun realShoot(shooter: TargetWithLookVec, pitchOffset: Float, velocity: Float, inaccuracy: Float) {
        isCritical = velocity >= 1f
        initialX = super.posX
        initialY = super.posY
        initialZ = super.posZ
        val lookVec = shooter.lookVec
        this.shoot(lookVec.x, lookVec.y, lookVec.z, velocity * 3f, inaccuracy)

        if (shooter is TargetWithMovement) {
            this.motionX += shooter.motionX
            this.motionZ += shooter.motionZ

            if (shooter is TargetWithCollision && !shooter.onGround) {
                this.motionY += shooter.motionY
            }
        }
    }

    override fun getIsCritical(): Boolean {
        return world.isRemote && super.getIsCritical()
    }

    override fun getArrowStack(): ItemStack = ItemStack.EMPTY

    override fun onUpdate() {
        super.onUpdate()

        world onClient {
            if (initialX == 0.0) {
                initialX = super.posX
                initialY = super.posY
                initialZ = super.posZ
            }
        }

        if (this.getDistanceSq(initialX, initialY, initialZ) > 200) {
            this.setDead()
        }
    }

    override var computedDamage: Double
        get() = damage
        set(value) {
            damage = value / 3.0
        }

    override var knockback: Int = 0
        set(value) {
            field = value
            setKnockbackStrength(knockback)
        }
}

@SideOnly(Side.CLIENT)
class RpgArrowRender(renderManagerIn: RenderManager) : RenderArrow<RpgArrowEntity>(renderManagerIn) {
    private companion object {
        private val RES_ARROW = ResourceLocation("textures/entity/projectiles/arrow.png")
    }

    override fun getEntityTexture(entity: RpgArrowEntity) = RES_ARROW
}