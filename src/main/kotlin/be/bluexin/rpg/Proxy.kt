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

@file:Suppress("unused")

package be.bluexin.rpg

import be.bluexin.rpg.blocks.BlockCaster
import be.bluexin.rpg.blocks.BlockEditor
import be.bluexin.rpg.blocks.BlockGatheringNode
import be.bluexin.rpg.containers.ContainerEditor
import be.bluexin.rpg.containers.RPGEnderChestContainer
import be.bluexin.rpg.entities.*
import be.bluexin.rpg.gear.*
import be.bluexin.rpg.items.DebugExpItem
import be.bluexin.rpg.items.DebugStatsItem
import be.bluexin.rpg.jobs.GatheringCapability
import be.bluexin.rpg.jobs.GatheringRegistry
import be.bluexin.rpg.pets.*
import be.bluexin.rpg.skills.SkillItem
import be.bluexin.rpg.skills.glitter.TrailSystem
import be.bluexin.rpg.stats.*
import be.bluexin.rpg.util.registerDataSerializer
import be.bluexin.saomclib.capabilities.CapabilitiesHandler
import com.teamwizardry.librarianlib.features.base.ModCreativeTab
import com.teamwizardry.librarianlib.features.saving.AbstractSaveHandler
import com.teamwizardry.librarianlib.features.saving.Savable
import com.teamwizardry.librarianlib.features.saving.Save
import kotlinx.coroutines.*
import net.minecraft.entity.SharedMonsterAttributes
import net.minecraft.entity.ai.attributes.BaseAttribute
import net.minecraft.entity.ai.attributes.RangedAttribute
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation
import net.minecraftforge.common.capabilities.CapabilityManager
import net.minecraftforge.common.util.FakePlayer
import net.minecraftforge.fml.client.registry.RenderingRegistry
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import net.minecraftforge.fml.common.registry.EntityRegistry
import net.minecraftforge.fml.relauncher.ReflectionHelper
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import java.io.File
import java.lang.ref.WeakReference

open class CommonProxy : CoroutineScope {

    private val job = Job()
    override val coroutineContext = Dispatchers.IO + job

    open fun preInit(event: FMLPreInitializationEvent) {
        launch { NameGenerator.preInit(event) }
        launch { FormulaeConfiguration.preInit(event) }
        launch {
            GatheringRegistry.setupDataDir(
                File(
                    event.suggestedConfigurationFile.parentFile.parentFile,
                    BlueRPG.MODID
                )
            )
        }

        classLoadItems()
        vanillaHax()
        registerEntities()

        CapabilitiesHandler.registerEntityCapability(
            PlayerStats::class.java,
            PlayerStats.Storage
        ) { it is EntityPlayer && it !is FakePlayer }
        // Not using SAOMCLib for this one because we don't want it autoregistered
        CapabilityManager.INSTANCE.register(GearStats::class.java, GearStats.Storage) { GearStats(ItemStack.EMPTY) }
        CapabilityManager.INSTANCE.register(TokenStats::class.java, TokenStats.Storage) { TokenStats(ItemStack.EMPTY) }
        CapabilityManager.INSTANCE.register(
            GatheringCapability::class.java,
            GatheringCapability.Storage
        ) { GatheringCapability() }
        CapabilitiesHandler.registerEntityCapability(
            PetStorage::class.java,
            PetStorage.Storage
        ) { it is EntityPlayer && it !is FakePlayer }
    }

    private fun vanillaHax() {
        (SharedMonsterAttributes.ATTACK_DAMAGE as BaseAttribute).shouldWatch = true
        try {
            val f = ReflectionHelper.findField(RangedAttribute::class.java, "field_111118_b", "maximumValue")
            f.set(SharedMonsterAttributes.MAX_HEALTH, Double.MAX_VALUE)
        } catch (e: Exception) {
            BlueRPG.LOGGER.warn("Unable to break max health limit", e)
        }
    }

    private fun classLoadItems() {
        object : ModCreativeTab() {
            override val iconStack: ItemStack
                get() = ItemStack(ItemGearToken[TokenType.TOKEN])

            init {
                registerDefaultTab()
            }
        }
        ItemGearToken
        DebugStatsItem
        DebugExpItem
        ItemArmor
        ItemMeleeWeapon
        ItemRangedWeapon
        ItemOffHand
        SkillItem

        BlockEditor
        ContainerEditor
        BlockCaster
        BlockGatheringNode

        EggItem

        EntitySkillProjectile.Companion // This is needed for it's custom serializer
        RPGEnderChestContainer.Companion // Classloading FTW
    }

    private fun registerEntities() {
        var id = 0
        EntityRegistry.registerModEntity(
            ResourceLocation(BlueRPG.MODID, "entity_rpg_arrow"),
            EntityRpgArrow::class.java,
            "entity_rpg_arrow",
            ++id,
            BlueRPG,
            128,
            1,
            true
        )
        EntityRegistry.registerModEntity(
            ResourceLocation(BlueRPG.MODID, "entity_wand_projectile"),
            EntityWandProjectile::class.java,
            "entity_wand_projectile",
            ++id,
            BlueRPG,
            128,
            1,
            true
        )
        EntityRegistry.registerModEntity(
            ResourceLocation(BlueRPG.MODID, "entity_skill_projectile"),
            EntitySkillProjectile::class.java,
            "entity_skill_projectile",
            ++id,
            BlueRPG,
            128,
            1,
            true
        )
        EntityRegistry.registerModEntity(
            ResourceLocation(BlueRPG.MODID, "entity_pet"),
            EntityPet::class.java,
            "entity_pet",
            ++id,
            BlueRPG,
            128,
            1,
            true
        )
    }

    open fun init(event: FMLInitializationEvent) {
        runBlocking { job.children.forEach { it.join() } }
        launch { GatheringRegistry.load() }
        launch { FormulaeConfiguration.init() }
        trickLiblib()
        registerDataSerializer<PetMovementType>()
        CommonEventHandler.loadInteractionLimit()
    }

    private fun trickLiblib() { // FIXME: remove once TeamWizardry/LibrarianLib#57 is fixed
        @Savable
        class T(@Save val s: StatCapability)

        @Savable
        class T2(@Save val s: Stat)

        AbstractSaveHandler.writeAutoNBT(T(GearStats(ItemStack.EMPTY)), false)
        AbstractSaveHandler.writeAutoNBT(T(TokenStats(ItemStack.EMPTY)), false)
        AbstractSaveHandler.writeAutoNBT(T(EggData()), false)
        AbstractSaveHandler.writeAutoNBT(T(PlayerStats().apply {
            baseStats = StatsCollection(WeakReference(Unit))
            level = Level(WeakReference<EntityPlayer>(null))
        }), false)
        AbstractSaveHandler.writeAutoNBT(T2(PrimaryStat.DEXTERITY), false)
        AbstractSaveHandler.writeAutoNBT(T2(SecondaryStat.REGEN), false)
        AbstractSaveHandler.writeAutoNBT(T2(FixedStat.HEALTH), false)
    }

    fun postInit(event: FMLPostInitializationEvent) {
        runBlocking { job.children.forEach { it.join() } }
    }
}

@SideOnly(Side.CLIENT)
class ClientProxy : CommonProxy() {
    override fun preInit(event: FMLPreInitializationEvent) {
        super.preInit(event)

        TrailSystem.load()

        registerEntityRenderers()
    }

    private fun registerEntityRenderers() {
        RenderingRegistry.registerEntityRenderingHandler(EntityRpgArrow::class.java, ::RenderRpgArrow)
        RenderingRegistry.registerEntityRenderingHandler(EntityWandProjectile::class.java, ::RenderWandProjectile)
        RenderingRegistry.registerEntityRenderingHandler(EntitySkillProjectile::class.java, ::RenderSkillProjectile)
        RenderingRegistry.registerEntityRenderingHandler(EntityPet::class.java, ::RenderPet)
    }
}

@SideOnly(Side.SERVER)
class ServerProxy : CommonProxy()