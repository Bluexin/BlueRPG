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

package be.bluexin.rpg.utilities

import be.bluexin.rpg.classes.playerClass
import be.bluexin.rpg.devutil.get
import be.bluexin.rpg.gear.RarityConfiguration
import be.bluexin.rpg.skills.SkillContext
import be.bluexin.rpg.skills.SkillRegistry
import be.bluexin.rpg.stats.FormulaeConfiguration
import com.teamwizardry.librarianlib.features.kotlin.localize
import net.minecraft.command.*
import net.minecraft.command.CommandBase.getEntity
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.server.MinecraftServer
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.BlockPos
import net.minecraft.util.text.TextComponentTranslation

fun <T : Entity> getEntityList(
    server: MinecraftServer,
    sender: ICommandSender,
    target: String,
    clazz: Class<T>
): List<T> {
    return (if (EntitySelector.isSelector(target)) EntitySelector.matchEntities(
        sender, target, clazz
    ) else listOf(getEntity(server, sender, target, clazz)))
}

object Command : CommandBase() {
    enum class Commands(private val handle: (server: MinecraftServer, sender: ICommandSender, args: List<String>) -> Unit) {
        RELOAD({ _, sender, _ ->
            FormulaeConfiguration.reload()
            RarityConfiguration.reload()
            sender.sendMessage(TextComponentTranslation("bluerpg.command.reload.success"))
        });

        operator fun invoke(server: MinecraftServer, sender: ICommandSender, args: List<String>) =
            handle(server, sender, args)
    }

    override fun getName() = "bluerpg"

    override fun execute(server: MinecraftServer, sender: ICommandSender, args: Array<String>) {
        if (args.isEmpty()) throw WrongUsageException(getUsage(sender))
        try {
            Command.Commands.valueOf(args[0].toUpperCase())(server, sender, args.drop(1))
        } catch (_: Exception) {
            throw WrongUsageException(getUsage(sender))
        }
    }

    override fun getUsage(sender: ICommandSender) = "bluerpg.command.usage"

    override fun getTabCompletions(
        server: MinecraftServer,
        sender: ICommandSender,
        args: Array<out String>,
        pos: BlockPos?
    ): MutableList<String> {
        if (sender !is EntityPlayer) return mutableListOf()
        if (args.size != 1) return super.getTabCompletions(server, sender, args, pos)
        val l = Command.Commands.values().map { it.name.toLowerCase() }
        return getListOfStringsMatchingLastWord(args, l)
    }
}

object CastCommand : CommandBase() {
    override fun getName() = "bluerpgcast"

    override fun execute(server: MinecraftServer, sender: ICommandSender, args: Array<String>) {
        if (args.size < 2) throw WrongUsageException(getUsage(sender))
        val target = getEntityList(server, sender, args[0], EntityLivingBase::class.java)
        val skill = SkillRegistry[ResourceLocation(args[1])]
            ?: throw CommandException("bluerpg.command.cast.skillnotfound", args[1])
        val lvl = args.getOrNull(2)?.toIntOrNull() ?: 1
        sender.sendMessage(
            TextComponentTranslation(
                "bluerpg.command.cast.success",
                skill.key,
                lvl,
                target.singleOrNull()?.name ?: "bluerpg.command.cast.targets".localize(target.size)
            )
        )
        for (t in target) skill.processor.cast(SkillContext(t, lvl))
    }

    override fun getUsage(sender: ICommandSender) = "bluerpg.command.cast.usage"

    override fun getRequiredPermissionLevel() = 2

    override fun getTabCompletions(
        server: MinecraftServer,
        sender: ICommandSender,
        args: Array<String>,
        targetPos: BlockPos?
    ): MutableList<String> {
        return when (args.size) {
            1 -> getListOfStringsMatchingLastWord(args, *server.onlinePlayerNames)
            2 -> getListOfStringsMatchingLastWord(args, SkillRegistry.keys)
            else -> super.getTabCompletions(server, sender, args, targetPos)
        }

    }

    override fun getAliases() = listOf("cast")
}

object ResetCommand : CommandBase() {
    override fun getName() = "bluerpgreset"

    override fun execute(server: MinecraftServer, sender: ICommandSender, args: Array<String>) {
        if (args.isEmpty()) throw WrongUsageException(getUsage(sender))
        val target = getPlayer(server, sender, args[0])
        sender.sendMessage(TextComponentTranslation("bluerpg.command.reset.success", target.displayName))
        target.playerClass.reset()
    }

    override fun getUsage(sender: ICommandSender) = "bluerpg.command.reset.usage"

    override fun getRequiredPermissionLevel() = 2

    override fun getTabCompletions(
        server: MinecraftServer,
        sender: ICommandSender,
        args: Array<String>,
        targetPos: BlockPos?
    ): MutableList<String> {
        return when (args.size) {
            1 -> getListOfStringsMatchingLastWord(args, *server.onlinePlayerNames)
            2 -> getListOfStringsMatchingLastWord(args, SkillRegistry.keys)
            else -> super.getTabCompletions(server, sender, args, targetPos)
        }

    }
}