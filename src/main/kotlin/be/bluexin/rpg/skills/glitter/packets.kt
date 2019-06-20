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

package be.bluexin.rpg.skills.glitter

import com.teamwizardry.librarianlib.features.autoregister.PacketRegister
import com.teamwizardry.librarianlib.features.network.PacketBase
import com.teamwizardry.librarianlib.features.network.PacketHandler
import com.teamwizardry.librarianlib.features.network.sendToAllAround
import com.teamwizardry.librarianlib.features.saving.Save
import net.minecraft.entity.EntityLivingBase
import net.minecraft.util.math.Vec3d
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext
import net.minecraftforge.fml.relauncher.Side
import java.awt.Color

abstract class GlitterPacket : PacketBase() {
    fun send(around: EntityLivingBase) =
        PacketHandler.NETWORK.sendToAllAround(this, around.world, around.positionVector, 64.0)

    override fun handle(ctx: MessageContext) = this.shine()

    abstract fun shine()
}

@PacketRegister(Side.CLIENT)
class PacketLightning(
    @Save var from: Vec3d,
    @Save var to: Vec3d
) : GlitterPacket() {
    override fun shine() = BeamLightningSystem.lightItUp(from, to)
}

@PacketRegister(Side.CLIENT)
class PacketGlitter(
    @Save var type: Type,
    @Save var pos: Vec3d,
    @Save var color1: Int,
    @Save var color2: Int,
    @Save var spread: Double
) : GlitterPacket() {

    constructor(type: Type, pos: Vec3d, color1: Color, color2: Color, spread: Double) : this(
        type,
        pos,
        color1.rgb,
        color2.rgb,
        spread
    )

    enum class Type {
        AOE,
        HEAL
    }

    override fun shine() {
        when (type) {
            Type.AOE -> AoE.burst(pos, Color(color1, true), Color(color2, true), spread)
            Type.HEAL -> Heal.burst(pos, Color(color1, true), Color(color2, true), spread)
        }
    }
}
