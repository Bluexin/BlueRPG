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

import be.bluexin.rpg.classes.PlayerClassCollection
import be.bluexin.rpg.classes.PlayerClassRegistry
import be.bluexin.rpg.devutil.AutoCapabilityStorage
import be.bluexin.rpg.devutil.BlueRPGDataFixer
import be.bluexin.rpg.devutil.registerDataSerializer
import be.bluexin.rpg.gear.*
import be.bluexin.rpg.inventory.RPGEnderChestContainer
import be.bluexin.rpg.jobs.GatheringNodeBlock
import be.bluexin.rpg.pets.*
import be.bluexin.rpg.skills.*
import be.bluexin.rpg.skills.glitter.TrailSystem
import be.bluexin.rpg.stats.*
import be.bluexin.rpg.utilities.*
import be.bluexin.saomclib.capabilities.CapabilitiesHandler
import com.teamwizardry.librarianlib.features.base.ModCreativeTab
import com.teamwizardry.librarianlib.features.kotlin.Minecraft
import com.teamwizardry.librarianlib.features.saving.AbstractSaveHandler
import com.teamwizardry.librarianlib.features.saving.Savable
import com.teamwizardry.librarianlib.features.saving.Save
import kotlinx.coroutines.*
import net.minecraft.client.renderer.entity.RenderEntityItem
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

// TODO: Split by feature
open class CommonProxy : CoroutineScope {

    private val job = Job()
    override val coroutineContext = Dispatchers.IO + job

    open fun preInit(event: FMLPreInitializationEvent) {
        customConfDir = File(event.suggestedConfigurationFile.parentFile, BlueRPG.MODID)
        if (!customConfDir.exists()) customConfDir.mkdir()
        if (!customConfDir.isDirectory) throw IllegalStateException("$customConfDir exists and is not a directory")
        launch { NameGenerator.preInit() }
        launch { FormulaeConfiguration.preInit() }
        launch { RarityConfiguration.preInit() }
        /*val localDir = File(
            event.suggestedConfigurationFile.parentFile.parentFile,
            BlueRPG.MODID
        )
        launch { GatheringRegistry.setupDataDir(localDir) }*/
        launch { PlayerClassRegistry.setupDataDir(customConfDir /*TODO: use localDir when server autosync is in place*/) }
//        launch { SkillRegistry.setupDataDir(customConfDir /*TODO: use localDir when server autosync is in place*/) } // Disabled

        classLoadItems()
        vanillaHax()
        registerEntities()

        CapabilitiesHandler.registerEntityCapability(
            PlayerStats::class.java,
            AutoCapabilityStorage()
        ) { it is EntityPlayer && it !is FakePlayer }
        CapabilitiesHandler.registerEntityCapability(
            PlayerClassCollection::class.java,
            AutoCapabilityStorage()
        ) { it is EntityPlayer && it !is FakePlayer }
        CapabilitiesHandler.registerEntityCapability(
            PetStorage::class.java,
            AutoCapabilityStorage()
        ) { it is EntityPlayer && it !is FakePlayer }
        CapabilitiesHandler.registerEntityCapability(
            CooldownCapability::class.java,
            AutoCapabilityStorage()
        ) { it is EntityPlayer && it !is FakePlayer }

        // Not using SAOMCLib for these because we don't want them autoregistered
        CapabilityManager.INSTANCE.register(GearStats::class.java, GearStats.Storage) { GearStats(ItemStack.EMPTY) }
        CapabilityManager.INSTANCE.register(TokenStats::class.java, TokenStats.Storage) { TokenStats(ItemStack.EMPTY) }
        /*CapabilityManager.INSTANCE.register(
            GatheringCapability::class.java,
            GatheringCapability.Storage
        ) { GatheringCapability() }*/
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
                get() = ItemStack(GearTokenItem[TokenType.TOKEN])

            init {
                registerDefaultTab()
            }
        }
        GearTokenItem
        StatsDebugItem
        ExpDebugItem
        ArmorItem
        MeleeWeaponItem
        RangedWeaponItem
        OffHandItem
        SkillItem
        DynItem.Companion

        EditorBlock
        EditorContainer
        CasterBlock
        GatheringNodeBlock

        EggItem

        SkillProjectileEntity.Companion // This is needed for it's custom serializer
        RPGEnderChestContainer.Companion // Classloading FTW
    }

    private fun registerEntities() {
        var id = 0
        EntityRegistry.registerModEntity(
            ResourceLocation(BlueRPG.MODID, "entity_rpg_arrow"),
            RpgArrowEntity::class.java,
            "entity_rpg_arrow",
            ++id,
            BlueRPG,
            128,
            1,
            true
        )
        EntityRegistry.registerModEntity(
            ResourceLocation(BlueRPG.MODID, "entity_wand_projectile"),
            WandProjectileEntity::class.java,
            "entity_wand_projectile",
            ++id,
            BlueRPG,
            128,
            1,
            true
        )
        EntityRegistry.registerModEntity(
            ResourceLocation(BlueRPG.MODID, "entity_skill_projectile"),
            SkillProjectileEntity::class.java,
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
        EntityRegistry.registerModEntity(
            ResourceLocation(BlueRPG.MODID, "entity_rpg_item"),
            RPGItemEntity::class.java,
            "entity_rpg_item",
            ++id,
            BlueRPG,
            128,
            1,
            true
        )
    }

    open fun init(event: FMLInitializationEvent) {
        runBlocking { job.children.forEach { it.join() } }
//        launch { GatheringRegistry.load() }
        launch { PlayerClassRegistry.load() }
//        launch { SkillRegistry.load() } // disabled
        launch { FormulaeConfiguration.init() }
        trickLiblib()
        registerDataSerializer<PetMovementType>()
        CommonEventHandler.loadInteractionLimit()
        DeathHandler.loadDropRestriction()
    }

    private fun trickLiblib() { // FIXME: remove once TeamWizardry/LibrarianLib#57 is fixed
        @Savable
        class T(@Save val s: StatCapability)

        @Savable
        class T2(@Save val s: Stat)

        AbstractSaveHandler.writeAutoNBT(T(GearStats(ItemStack.EMPTY)), false)
        AbstractSaveHandler.writeAutoNBT(T(TokenStats(ItemStack.EMPTY)), false)
        AbstractSaveHandler.writeAutoNBT(T(EggData()), false)
        AbstractSaveHandler.writeAutoNBT(T(DynamicData()), false)
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

    companion object {
        internal lateinit var customConfDir: File
    }
}

@SideOnly(Side.CLIENT)
class ClientProxy : CommonProxy() {
    override fun preInit(event: FMLPreInitializationEvent) {
        BlueRPGDataFixer.setup(Minecraft().dataFixer)
        super.preInit(event)

        TrailSystem.load()

        registerEntityRenderers()
    }

    override fun init(event: FMLInitializationEvent) {
        super.init(event)
        Keybinds.register()
    }

    private fun registerEntityRenderers() {
        RenderingRegistry.registerEntityRenderingHandler(RpgArrowEntity::class.java, ::RpgArrowRender)
        RenderingRegistry.registerEntityRenderingHandler(WandProjectileEntity::class.java, ::WandProjectileRender)
        RenderingRegistry.registerEntityRenderingHandler(SkillProjectileEntity::class.java, ::SkillProjectileRender)
        RenderingRegistry.registerEntityRenderingHandler(EntityPet::class.java, ::RenderPet)
        RenderingRegistry.registerEntityRenderingHandler(RPGItemEntity::class.java) {
            RenderEntityItem(
                it,
                Minecraft().renderItem
            )
        }
    }
}

@SideOnly(Side.SERVER)
class ServerProxy : CommonProxy()