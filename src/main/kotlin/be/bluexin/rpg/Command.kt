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

import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import net.minecraft.command.WrongUsageException
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.server.MinecraftServer
import net.minecraft.util.math.BlockPos

object Command : CommandBase() {
    enum class Commands(private val handle: (server: MinecraftServer, sender: ICommandSender, args: List<String>) -> Unit) {
        RELOAD({ _, sender, _ ->
            be.bluexin.rpg.stats.FormulaeConfiguration.reload()
            sender.sendMessage(net.minecraft.util.text.TextComponentTranslation("bluerpg.command.reload.success"))
        });

        operator fun invoke(server: MinecraftServer, sender: ICommandSender, args: List<String>) = handle(server, sender, args)
    }

    override fun getName() = "bluerpg"

    override fun execute(server: MinecraftServer, sender: ICommandSender, args: Array<String>) {
        if (args.isEmpty()) throw WrongUsageException(getUsage(sender))
        try {
            Commands.valueOf(args[0].toUpperCase())(server, sender, args.drop(1))
        } catch (_: Exception) {
            throw WrongUsageException(getUsage(sender))
        }
    }

    override fun getUsage(sender: ICommandSender) = "bluerpg.command.usage"

    override fun getTabCompletions(server: MinecraftServer, sender: ICommandSender, args: Array<out String>, pos: BlockPos?): MutableList<String> {
        if (sender !is EntityPlayer) return mutableListOf()
        if (args.size != 1) return super.getTabCompletions(server, sender, args, pos)
        val l = Commands.values().map { it.name.toLowerCase() }
        return CommandBase.getListOfStringsMatchingLastWord(args, l)
    }
}