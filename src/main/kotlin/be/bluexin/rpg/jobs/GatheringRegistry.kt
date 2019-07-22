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

package be.bluexin.rpg.jobs

import be.bluexin.rpg.BlueRPG
import be.bluexin.rpg.devutil.RNG
import be.bluexin.rpg.devutil.buildRegistry
import com.teamwizardry.librarianlib.features.kotlin.fromNBT
import com.teamwizardry.librarianlib.features.kotlin.get
import com.teamwizardry.librarianlib.features.kotlin.tagCompound
import com.teamwizardry.librarianlib.features.kotlin.toNBT
import com.teamwizardry.librarianlib.features.saving.Savable
import com.teamwizardry.librarianlib.features.saving.Save
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import net.minecraft.nbt.CompressedStreamTools
import net.minecraft.util.ResourceLocation
import net.minecraftforge.registries.IForgeRegistryEntry
import net.minecraftforge.registries.IForgeRegistryModifiable
import java.io.File

@Savable
/*data*/ class GatheringData(
    /**
     * Unique key to identify the data
     */
    @Save
    var key: ResourceLocation,
    /**
     * Armourer's Workshop skin
     */
    @Save
    var awSkin: Unit /*TODO: handle AW Skin (de)ser*/,
    /**
     * Minimum respawn time, in ticks
     */
    @Save
    var respawnMin: Long,
    /**
     * Maximum respawn time, in ticks
     */
    @Save
    var respawnMax: Long,
    /**
     * Possible drops when successfully gathering this, as pairs of chance to drop
     */
    @Save
    val drops: MutableList<Pair<Float, Unit>> /*TODO: <chance, SomeDropHandler/LootTable?>*/,
    /**
     * Tool to be used to gather this
     */
    @Save
    var tool: Unit? /*TODO: tool*/,
    /**
     * Requirements to be able to gather this, as a pair of job to required level
     */
    @Save
    var requirement: Pair<Unit, Int> /*TODO: <Job, req level>*/,
    /**
     * Time it takes to gather this, in ticks
     */
    @Save
    var time: Long,
    /**
     * Job experience gain when successfully gathering this
     */
    @Save
    var exp: Long,
    /**
     * Difficulty for gathering this ?_?
     */
    @Save
    var difficulty: Unit /*TODO: ?*/
) : IForgeRegistryEntry.Impl<GatheringData>() {

    init {
        this.registryName = key
    }

    val nextGatherTime: Long
        get() = if (respawnMin < respawnMax) RNG.nextLong(respawnMin, respawnMax) else respawnMin
}

object GatheringRegistry : IForgeRegistryModifiable<GatheringData> by buildRegistry("gathering") { // TODO: sync clients
    // TODO: set missing callback to return dummy value to be replaced by network loading

    private lateinit var saver: SendChannel<Unit>

    private lateinit var datadir: File
    private lateinit var savefile: File
    private lateinit var savefileBackup: File

    fun createServerDataDir(server: String) = this.loadDirectoryLayout(File(datadir, server))

    fun setupDataDir(dir: File) {
        this.datadir = dir
        this.loadDirectoryLayout(dir)
    }

    private fun loadDirectoryLayout(dir: File) {
        if ((!dir.exists() && !dir.mkdirs()) || !dir.isDirectory) throw IllegalStateException("$dir exists but is not a directory")
        savefile = File(dir, "gatheringregistry.dat")
        savefileBackup = File(dir, "gatheringregistryBackup.dat")
    }

    suspend fun load() { // TODO: this should be changed to registry event
        try {
            if (!savefile.exists() && savefileBackup.exists()) savefileBackup.copyTo(savefile)
            if (savefile.exists()) savefile.inputStream().use {
                CompressedStreamTools.readCompressed(it)
            }["root"]?.fromNBT<List<GatheringData>>()?.forEach(::register)
        } catch (e: Exception) {
            BlueRPG.LOGGER.error("Couldn't read Gathering registry", Exception(e))
        }

        saver = GlobalScope.actor(
            context = Dispatchers.IO,
            capacity = Channel.CONFLATED,
            start = CoroutineStart.LAZY
        ) {
            for (it in this) save()
        }
    }

    private fun save() {
        if (savefile.exists()) savefile.copyTo(savefileBackup, true)
        val nbt = tagCompound {
            "root" to try {
                valuesCollection.toList().toNBT()
            } catch (e: Exception) {
                val wrappedException = Exception(e)
                BlueRPG.LOGGER.error("Couldn't save Gathering registry", wrappedException)
                throw wrappedException
            }
        }
        savefile.outputStream().use {
            CompressedStreamTools.writeCompressed(nbt, it)
        }
    }
}