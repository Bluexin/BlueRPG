package be.bluexin.rpg

import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.Event

fun fire(event: Event) = !MinecraftForge.EVENT_BUS.post(event)