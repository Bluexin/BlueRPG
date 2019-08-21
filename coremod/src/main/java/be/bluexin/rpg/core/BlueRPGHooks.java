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

package be.bluexin.rpg.core;

import be.bluexin.rpg.events.*;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.inventory.IInventory;
import net.minecraft.util.CombatTracker;
import net.minecraftforge.common.MinecraftForge;

@SuppressWarnings("unused")
public class BlueRPGHooks {

    private BlueRPGHooks() {
    }

    public static InventoryPlayer inventoryPlayerHook(InventoryPlayer inventory, EntityPlayer player) {
        CreatePlayerInventoryEvent evt = new CreatePlayerInventoryEvent(player, inventory);
        MinecraftForge.EVENT_BUS.post(evt);
        return evt.inventoryPlayer;
    }

    public static Container containerPlayerHook(ContainerPlayer container, EntityPlayer player) {
        CreatePlayerContainerEvent evt = new CreatePlayerContainerEvent(player, container);
        MinecraftForge.EVENT_BUS.post(evt);
        return evt.container;
    }

    public static void equipmentPostChangeHook(EntityLivingBase entity, EntityEquipmentSlot slot) {
        MinecraftForge.EVENT_BUS.post(new LivingEquipmentPostChangeEvent(entity, slot));
    }

    public static void openEnderChestGui(EntityPlayer player, IInventory chest) {
        OpenEnderChestEvent evt = new OpenEnderChestEvent(player, chest);
        MinecraftForge.EVENT_BUS.post(evt);
        if (!evt.isCanceled()) player.displayGUIChest(chest);
    }

    public static void combatStart(EntityLivingBase entity) {
        MinecraftForge.EVENT_BUS.post(new CombatEvent(entity, CombatEvent.Action.START));
    }

    public static void combatEnd(EntityLivingBase entity) {
        MinecraftForge.EVENT_BUS.post(new CombatEvent(entity, CombatEvent.Action.END));
    }

    public static CombatTracker combatTrackerHook(CombatTracker tracker, EntityLivingBase entity) {
        CreateCombatTrackerEvent evt = new CreateCombatTrackerEvent(entity, tracker);
        MinecraftForge.EVENT_BUS.post(evt);
        return evt.tracker;
    }
}
