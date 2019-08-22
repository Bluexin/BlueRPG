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

import be.bluexin.rpg.BlueRPG
import be.bluexin.saomclib.capabilities.AbstractEntityCapability
import be.bluexin.saomclib.capabilities.Key
import com.teamwizardry.librarianlib.features.saving.Save
import com.teamwizardry.librarianlib.features.saving.SaveInPlace
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.ResourceLocation
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.CapabilityInject
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.PlayerEvent
import java.lang.ref.WeakReference

@SaveInPlace
class PetStorage : AbstractEntityCapability() {

    @Save
    internal var petID: Int = -1

    private var petEntityRef: WeakReference<PetEntity>? = null
        get() {
            if (field == null && petID > 0) {
                val p = reference.get()?.world?.getEntityByID(petID) as? PetEntity
                if (p == null) petID = -1
                else field = WeakReference(p)
            }

            return field
        }
        set(value) {
            field = value
            petID = value?.get()?.entityId ?: -1
        }

    var petEntity: PetEntity?
        get() = petEntityRef?.get()
        set(value) {
            petEntityRef = if (value != null) WeakReference(value) else null
        }

    fun killPet() {
        petEntity?.setDead()
        petEntity = null
    }

    companion object {
        @Key
        val KEY = ResourceLocation(BlueRPG.MODID, "pet_storage")

        @CapabilityInject(PetStorage::class)
        lateinit var capability: Capability<PetStorage>
            internal set

        init {
            MinecraftForge.EVENT_BUS.register(this)
        }

        @SubscribeEvent
        fun onPlayerLogout(event: PlayerEvent.PlayerLoggedOutEvent) = event.player.petStorage.killPet()
    }
}

val EntityPlayer.petStorage get() = this.getCapability(PetStorage.capability, null)!!