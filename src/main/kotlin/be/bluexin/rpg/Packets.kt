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

import be.bluexin.rpg.containers.ContainerEditor
import be.bluexin.rpg.gear.WeaponAttribute
import be.bluexin.rpg.stats.*
import com.teamwizardry.librarianlib.features.autoregister.PacketRegister
import com.teamwizardry.librarianlib.features.container.internal.ContainerImpl
import com.teamwizardry.librarianlib.features.network.PacketBase
import com.teamwizardry.librarianlib.features.saving.Save
import net.minecraft.util.math.BlockPos
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext
import net.minecraftforge.fml.relauncher.Side

@PacketRegister(Side.SERVER)
class PacketRaiseStat(stat: PrimaryStat, amount: Int) : PacketBase() {
    @Save
    var stat = stat
        internal set
    @Save
    var amount = amount
        internal set

    @Suppress("unused")
    internal constructor() : this(PrimaryStat.STRENGTH, 0)

    override fun handle(ctx: MessageContext) {
        val stats = ctx.serverHandler.player.stats
        if (stats.attributePoints >= amount) {
            if (stats.baseStats.set(stat, stats.baseStats[stat] + amount)) {
                stats.attributePoints -= amount
            }
        }
    }
}

@PacketRegister(Side.SERVER)
class PacketSetEditorStats(pos: BlockPos, stats: StatCapability?) : PacketBase() {
    @Save
    var stats = stats
        internal set

    @Save
    var pos = pos
        internal set

    @Suppress("unused")
    internal constructor() : this(BlockPos.ORIGIN, null)

    override fun handle(ctx: MessageContext) {
        val player = ctx.serverHandler.player
        val te = if (player.world.isBlockLoaded(pos)) player.world.getTileEntity(pos) else return
        if (te != null && ((player.openContainer as? ContainerImpl)?.container as? ContainerEditor)?.te == te) {
            when (stats) {
                is TokenStats -> te.tokenStats = stats as TokenStats
                is GearStats -> te.gearStats = stats as GearStats
                is EggData -> te.eggStats = stats as EggData
            }
        }
    }
}

@PacketRegister(Side.SERVER)
class PacketSaveLoadEditorItem(pos: BlockPos, saving: Boolean) : PacketBase() {
    @Save
    var saving = saving
        internal set

    @Save
    var pos = pos
        internal set

    @Suppress("unused")
    internal constructor() : this(BlockPos.ORIGIN, false)

    override fun handle(ctx: MessageContext) {
        val player = ctx.serverHandler.player
        val te = if (player.world.isBlockLoaded(pos)) player.world.getTileEntity(pos) else return
        if (te != null && ((player.openContainer as? ContainerImpl)?.container as? ContainerEditor)?.te == te) {
            if (saving) te.saveStats()
            else te.loadStats()
        }
    }

}

@PacketRegister(Side.SERVER)
class PacketAttack(entityID: Int) : PacketBase() {
    @Save
    var entityID = entityID
        internal set

    @Suppress("unused")
    internal constructor() : this(0)

    override fun handle(ctx: MessageContext) {
        val player = ctx.serverHandler.player
        val target = player.world.getEntityByID(entityID) ?: return
        val reach = player[WeaponAttribute.RANGE]
        if (player.positionVector.squareDistanceTo(target.positionVector) > reach * reach) return
        player.attackTargetEntityWithCurrentItem(target)
    }
}