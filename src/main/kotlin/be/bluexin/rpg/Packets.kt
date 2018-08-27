package be.bluexin.rpg

import be.bluexin.rpg.stats.Stats
import be.bluexin.rpg.stats.stats
import com.teamwizardry.librarianlib.features.autoregister.PacketRegister
import com.teamwizardry.librarianlib.features.network.PacketBase
import com.teamwizardry.librarianlib.features.saving.Save
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext
import net.minecraftforge.fml.relauncher.Side

@PacketRegister(Side.SERVER)
class PacketRaiseStat(stat: Stats, amount: Int) : PacketBase() {
    @Save
    var stat = stat
        internal set
    @Save
    var amount = amount
        internal set

    @Suppress("unused")
    internal constructor() : this(Stats.STRENGTH, 0)

    override fun handle(ctx: MessageContext) {
        val stats = ctx.serverHandler.player.stats
        if (stats.attributePoints >= amount) {
            if (stats.baseStats.set(stat, stats.baseStats[stat] + amount)) {
                stats.attributePoints -= amount
            }
        }
    }
}