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

package be.bluexin.rpg.skills

import be.bluexin.rpg.BlueRPG
import be.bluexin.rpg.PacketUseSkill
import com.teamwizardry.librarianlib.features.kotlin.Minecraft
import com.teamwizardry.librarianlib.features.network.PacketHandler
import net.minecraft.client.settings.KeyBinding
import net.minecraftforge.client.settings.KeyConflictContext
import net.minecraftforge.fml.client.registry.ClientRegistry
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import org.lwjgl.input.Keyboard
import java.util.*


@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = BlueRPG.MODID, value = [Side.CLIENT])
object Keybinds {
    private val allKeyBinds = LinkedList<Keybind>()

    private val skills = Array(5) {
        Keybind(
            KeyBinding("rpg.key.skill_$it.desc", KeyConflictContext.IN_GAME, Keyboard.KEY_5 + it, skillsCategory),
            {
                if (it !in Minecraft().player.cooldowns) {
                    PacketHandler.NETWORK.sendToServer(PacketUseSkill(it, true))
                    true
                } else false
            },
            { PacketHandler.NETWORK.sendToServer(PacketUseSkill(it, false)) }
        )
    }

    fun register() = allKeyBinds.forEach(Keybind::register)

    @SubscribeEvent
    @JvmStatic
    fun keyPress(event: InputEvent.KeyInputEvent) = allKeyBinds.forEach(Keybind::refresh)

    private const val skillsCategory = "rpg.key.skills"

    @SideOnly(Side.CLIENT)
    private data class Keybind(
        private val binding: KeyBinding,
        private val onPress: () -> Boolean = { true },
        private val onRelease: () -> Unit = { Unit }
    ) {
        init {
            allKeyBinds += this
        }

        private var isDown = false
            set(value) {
                if (value != field) {
                    field = value
                    if (value) field = onPress()
                    else onRelease()
                }
            }

        fun refresh() {
            this.isDown = binding.isKeyDown
        }

        fun register() = ClientRegistry.registerKeyBinding(binding)
    }
}