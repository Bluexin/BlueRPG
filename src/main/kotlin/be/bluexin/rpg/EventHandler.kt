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

import be.bluexin.rpg.containers.RPGContainer
import be.bluexin.rpg.containers.RPGEnderChestContainer
import be.bluexin.rpg.events.CreatePlayerContainerEvent
import be.bluexin.rpg.events.CreatePlayerInventoryEvent
import be.bluexin.rpg.events.LivingEquipmentPostChangeEvent
import be.bluexin.rpg.gear.IRPGGear
import be.bluexin.rpg.gear.WeaponAttribute
import be.bluexin.rpg.gear.WeaponType
import be.bluexin.rpg.gui.GuiRpgInventory
import be.bluexin.rpg.inventory.RPGInventory
import be.bluexin.rpg.pets.EggItem
import be.bluexin.rpg.pets.RenderEggItem
import be.bluexin.rpg.pets.eggData
import be.bluexin.rpg.skills.*
import be.bluexin.rpg.stats.*
import be.bluexin.rpg.util.RNG
import be.bluexin.rpg.util.Resources
import be.bluexin.saomclib.onServer
import com.teamwizardry.librarianlib.features.config.ConfigProperty
import com.teamwizardry.librarianlib.features.container.GuiHandler
import com.teamwizardry.librarianlib.features.container.internal.ContainerImpl
import com.teamwizardry.librarianlib.features.kotlin.Minecraft
import net.minecraft.block.Block
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.color.IItemColor
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityPainting
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.ContainerChest
import net.minecraft.inventory.ContainerPlayer
import net.minecraft.inventory.EntityEquipmentSlot
import net.minecraft.inventory.InventoryEnderChest
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagByte
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.util.text.TextComponentTranslation
import net.minecraftforge.client.event.*
import net.minecraftforge.common.util.FakePlayer
import net.minecraftforge.event.RegistryEvent
import net.minecraftforge.event.ServerChatEvent
import net.minecraftforge.event.entity.EntityEvent
import net.minecraftforge.event.entity.EntityJoinWorldEvent
import net.minecraftforge.event.entity.living.*
import net.minecraftforge.event.entity.player.CriticalHitEvent
import net.minecraftforge.event.entity.player.PlayerContainerEvent
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.event.entity.player.PlayerPickupXpEvent
import net.minecraftforge.event.world.BlockEvent
import net.minecraftforge.fml.client.config.GuiUtils.drawTexturedModalRect
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.eventhandler.Event
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import org.lwjgl.opengl.GL11
import kotlin.math.min

@Mod.EventBusSubscriber(modid = BlueRPG.MODID)
object CommonEventHandler {

    @SubscribeEvent
    @JvmStatic
    fun playerTick(event: TickEvent.PlayerTickEvent) {
        if (event.phase != TickEvent.Phase.END) return
        event.player.world onServer {
            val stats = event.player.stats
            if (stats.dirty) stats.sync()
            if (event.player.health > event.player.maxHealth) event.player.health = event.player.maxHealth
            var regenTick = event.player.entityData.getInteger("bluerpg:regen")
            if (--regenTick <= 0) {
                regenTick = 100
                val combat = event.player.combatTracker.inCombat
                event.player.heal((event.player[SecondaryStat.REGEN] * if (combat) 0.2 else 1.0).toFloat())
            }
            event.player.entityData.setInteger("bluerpg:regen", regenTick)
            // TODO: same for mana
        }
    }

    @SubscribeEvent
    @JvmStatic
    fun changeGear(event: LivingEquipmentChangeEvent) {
        val p = event.entityLiving as? EntityPlayer ?: return

        @Suppress("NON_EXHAUSTIVE_WHEN")
        when (event.slot) {
            EntityEquipmentSlot.MAINHAND -> {
                val offHand = p.heldItemOffhand
                if (!offHand.isEmpty) {
                    val from = (event.from.item as? IRPGGear)?.type as? WeaponType
                    val to = (event.to.item as? IRPGGear)?.type as? WeaponType
                    if (to?.twoHander == true && from?.twoHander != true) {
                        offHand.setTagInfo("bluerpg:twohandflag", NBTTagByte(1))
                    } else if (from?.twoHander == true && to?.twoHander != true) {
                        offHand.setTagInfo("bluerpg:twohandflag", NBTTagByte(0))
                    }
                }
            }
            EntityEquipmentSlot.OFFHAND -> {
                val mainhand = (p.heldItemMainhand.item as? IRPGGear)?.type as? WeaponType
                val to = event.to
                if (to.item is IRPGGear) to.setTagInfo(
                    "bluerpg:twohandflag",
                    NBTTagByte(if (mainhand?.twoHander == true) 1 else 0)
                )
            }
        }
    }

    @SubscribeEvent
    @JvmStatic
    fun postChangeGear(event: LivingEquipmentPostChangeEvent) {
        (event.entityLiving as? EntityPlayer)?.apply {
            equipmentAndArmor.forEach {
                if (it.item is IRPGGear) it.enabled = it.requirementMet(this)
            }
        }
    }

    @SubscribeEvent
    @JvmStatic
    fun entityConstructing(event: EntityEvent.EntityConstructing) {
        val e = event.entity
        if (e is EntityPlayer) {
            val m = e.attributeMap
            PrimaryStat.values().forEach {
                if (it.shouldRegister) m.registerAttribute(it.attribute)
            }
            SecondaryStat.values().forEach {
                if (it.shouldRegister) m.registerAttribute(it.attribute)
            }
            FixedStat.values().forEach {
                if (it.shouldRegister) m.registerAttribute(it.attribute)
            }
            WeaponAttribute.values().forEach {
                if (it.shouldRegister) m.registerAttribute(it.attribute)
            }
        }
    }

    @SubscribeEvent
    @JvmStatic
    fun hitEntity(event: LivingAttackEvent) = DamageHandler(event)

    @SubscribeEvent
    @JvmStatic
    fun livingHurt(event: LivingHurtEvent) = DamageHandler(event)

    @SubscribeEvent
    @JvmStatic
    fun entityHit(event: LivingDamageEvent) = DamageHandler(event)

    @SubscribeEvent
    @JvmStatic
    fun knockBack(event: LivingKnockBackEvent) {
        val a = event.attacker as? EntityPlayer ?: return
        event.strength += a[WeaponAttribute.KNOCKBACK].toFloat() * a.entityData.getFloat("bluerpg:lastweaponcd")
    }

    @SubscribeEvent
    @JvmStatic
    fun vanillaCrit(event: CriticalHitEvent) {
        event.result = Event.Result.DENY
    }

    @SubscribeEvent
    @JvmStatic
    fun pickupXp(event: PlayerPickupXpEvent) {
        event.entityPlayer.stats.level += event.orb.xpValue.toLong()
    }

    @SubscribeEvent
    @JvmStatic
    fun newRegistry(event: RegistryEvent.NewRegistry) {
        SkillRegistry
    }

    @SubscribeEvent
    @JvmStatic
    fun registerSkills(event: RegistryEvent.Register<SkillData>) {
        event.registry.registerAll(
            SkillData(
                ResourceLocation(BlueRPG.MODID, "skill_0"),
                ResourceLocation(BlueRPG.MODID, "skill_0"),
                "I don't really have a description yet",
                mana = 10,
                cooldown = 60,
                levelTransformer = Placeholder(2),
                processor = Processor(
                    Use(10),
                    Projectile(
                        condition = RequireStatus(Status.AGGRESSIVE),
                        color1 = 0x0055BB, color2 = 0x0085BB,
                        trailSystem = ResourceLocation(BlueRPG.MODID, "ice"),
                        velocity = 1.5f
                    ),
                    RequireStatus(Status.AGGRESSIVE),
                    Damage { caster, _ ->
                        caster[PrimaryStat.INTELLIGENCE] * 2f *
                                RNG.nextDouble(.95, 1.05) + RNG.nextInt(3)
                    }
                )
            ),
            SkillData(
                ResourceLocation(BlueRPG.MODID, "skill_1"),
                ResourceLocation(BlueRPG.MODID, "skill_1"),
                "I don't really have a description yet",
                mana = 10,
                cooldown = 60,
                levelTransformer = Placeholder(2),
                processor = Processor(
                    Use(20),
                    Projectile(
                        velocity = .8f,
                        inaccuracy = 2.5f,
                        color1 = 0xFFDD0B, color2 = 0xFF0000,
                        trailSystem = ResourceLocation(BlueRPG.MODID, "embers"),
                        precise = true
                    ),
                    RequireStatus(Status.AGGRESSIVE),
                    Skill(
                        AoE(color1 = 0xFFDD0B, color2 = 0xFF0000),
                        RequireStatus(Status.AGGRESSIVE),
                        Damage { caster, _ ->
                            caster[PrimaryStat.STRENGTH] * 2f *
                                    RNG.nextDouble(1.05, 1.15) + RNG.nextInt(6)
                        }
                    )

                )
            ),
            SkillData(
                ResourceLocation(BlueRPG.MODID, "skill_2"),
                ResourceLocation(BlueRPG.MODID, "skill_2"),
                "I don't really have a description yet",
                mana = 10,
                cooldown = 60,
                levelTransformer = Placeholder(2),
                processor = Processor(
                    Use(10),
                    Self(
                        color1 = 0x2CB000,
                        color2 = 0x8EE807,
                        glitter = PacketGlitter.Type.HEAL
                    ),
                    RequireStatus(Status.FRIENDLY),
                    Damage { caster, _ ->
                        -(caster[PrimaryStat.WISDOM] * 2f *
                                RNG.nextDouble(.95, 1.05) + RNG.nextInt(3))
                    }
                )
            ),
            SkillData(
                ResourceLocation(BlueRPG.MODID, "skill_3"),
                ResourceLocation(BlueRPG.MODID, "skill_3"),
                "I don't really have a description yet",
                mana = 10,
                cooldown = 60,
                levelTransformer = Placeholder(2),
                processor = Processor(
                    Use(15),
                    Self(
                        color1 = 0xA4D9F7,
                        color2 = 0xCADDE8,
                        glitter = PacketGlitter.Type.AOE
                    ),
                    null,
                    Velocity { _, target ->
                        if (target is TargetWithLookVec) target.lookVec
                        else Vec3d.ZERO
                    }
                )
            ),
            SkillData(
                ResourceLocation(BlueRPG.MODID, "skill_4"),
                ResourceLocation(BlueRPG.MODID, "skill_4"),
                "I don't really have a description yet",
                mana = 10,
                cooldown = 60,
                levelTransformer = Placeholder(2),
                processor = Processor(
                    Use(10),
                    Raycast(range = 15.0),
                    RequireStatus(Status.AGGRESSIVE),
                    MultiEffect(
                        arrayOf(
                            Damage { caster, _ ->
                                caster[PrimaryStat.DEXTERITY] * 2f * RNG.nextDouble(.95, 1.05) + RNG.nextInt(3)
                            },
                            Skill(
                                Chain(delayMillis = 100),
                                RequireStatus(Status.AGGRESSIVE),
                                Damage { caster, _ ->
                                    caster[PrimaryStat.DEXTERITY] * RNG.nextDouble(.95, 1.05) + RNG.nextInt(3)
                                }
                            ))
                    )
                )
            )
        )
    }

    @SubscribeEvent
    @JvmStatic
    fun playerInventoryCreated(event: CreatePlayerInventoryEvent) {
        if (event.player !is FakePlayer) event.inventoryPlayer = RPGInventory(event.player)
    }

    @SubscribeEvent
    @JvmStatic
    fun playerContainerCreated(event: CreatePlayerContainerEvent) {
        if (event.player !is FakePlayer) event.container =
                RPGContainer(event.player, event.container as ContainerPlayer).impl
    }

    fun loadInteractionLimit() {
        var s = interactionLimit
        limitIsWhitelist = !s.startsWith('!')
        if (!limitIsWhitelist) s = s.substring(1)
        interactionLimitBlocks = s.split(',')
            .mapNotNull {
                if (it.isNotBlank()) Block.getBlockFromName(it)
                    ?: null.apply { BlueRPG.LOGGER.warn("Invalid ID: `$it`") } else null
            }
    }

    @ConfigProperty(
        "security",
        "Comma-separated list with the registry names of all permitted blocks. Prefix with ! to turn into a blacklist instead."
    )
    var interactionLimit: String = "!"
    private lateinit var interactionLimitBlocks: List<Block>
    private var limitIsWhitelist = false

    @SubscribeEvent
    @JvmStatic
    fun playerInteractWithBlock(event: PlayerInteractEvent.RightClickBlock) {
        if (!event.entityPlayer.isCreative && (limitIsWhitelist xor (event.entityPlayer.world.getBlockState(event.pos).block in interactionLimitBlocks))) {
            event.useBlock = Event.Result.DENY
        }
    }

    @ConfigProperty("security", "Whether to protect farmland from trampling")
    var protectFarmland = false

    @SubscribeEvent
    @JvmStatic
    fun farmlandTrample(event: BlockEvent.FarmlandTrampleEvent) {
        if (protectFarmland) event.isCanceled = true
    }

    @SubscribeEvent
    @JvmStatic
    fun openContainer(event: PlayerContainerEvent.Open) {
        val c = event.container
        when (c) {
            is ContainerChest -> {
                if (c.lowerChestInventory is InventoryEnderChest) {
                    GuiHandler.open(RPGEnderChestContainer.NAME, event.entityPlayer, BlockPos.ORIGIN)
                }
            }
        }
    }

    @ConfigProperty("security", "Whether to protect paintings from breaking")
    var protectPaintings = false

    @SubscribeEvent
    @JvmStatic
    fun spawnPainting(event: EntityJoinWorldEvent) {
        val e = event.entity
        if (e is EntityLivingBase) e.maxHurtResistantTime = 0
        if (protectPaintings && e is EntityPainting) e.setEntityInvulnerable(true)
    }
}

@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = BlueRPG.MODID, value = [Side.CLIENT])
object ClientEventHandler {

    @SubscribeEvent
    @JvmStatic
    fun hitEmpty(event: PlayerInteractEvent.LeftClickEmpty) = DamageHandler.handleRange(event.entityPlayer)

    @SubscribeEvent
    @JvmStatic
    fun onTextureStitchEvent(event: TextureStitchEvent) {
        event.map.registerSprite(Resources.PARTICLE)
    }

    @SubscribeEvent
    @JvmStatic
    fun registerItemHandlers(event: ColorHandlerEvent.Item) {
        event.itemColors.registerItemColorHandler(
            IItemColor { stack, tintIndex ->
                val eggData = stack.eggData ?: return@IItemColor -1
                if (tintIndex == 0) eggData.primaryColor else eggData.secondaryColor
            }, EggItem
        )
    }

    @SubscribeEvent
    @JvmStatic
    fun registerModels(event: ModelRegistryEvent) {
        EggItem.tileEntityItemStackRenderer = object : TileEntityItemStackRenderer() {
            private val skinRenderer = RenderEggItem()

            override fun renderByItem(itemStackIn: ItemStack) =
                if (itemStackIn.eggData?.isHatched != true) TileEntityItemStackRenderer.instance.renderByItem(
                    itemStackIn
                )
                else skinRenderer.renderByItem(itemStackIn)
        }
    }

    @SubscribeEvent
    @JvmStatic
    fun openGui(event: GuiOpenEvent) {
        val g = event.gui
        when (g) {
            is GuiInventory -> event.gui =
                    GuiRpgInventory((g.inventorySlots as ContainerImpl).container as RPGContainer)
//            is GuiChest -> event.gui = GuiRpgEnderInventory(RPGEnderChestContainer(Minecraft().player))
        }
    }

    @SubscribeEvent
    @JvmStatic
    fun renderCastBar(event: RenderGameOverlayEvent.Post) { // TODO: Skin with SAOUI
        if (event.type == RenderGameOverlayEvent.ElementType.HOTBAR) {
            val mc = Minecraft()
            val iss = mc.player.activeItemStack
            if (iss.item == SkillItem && iss.maxItemUseDuration > 0) {
                GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f)
                GlStateManager.disableBlend()

                mc.profiler.startSection("castBar")
                val charge = min(1 - mc.player.itemInUseCount / iss.maxItemUseDuration.toFloat(), 1f)
                val barWidth = 182
                val res = ScaledResolution(mc)
                val x = res.scaledWidth / 2 - barWidth / 2
                val filled = (charge * barWidth).toInt()
                val top = res.scaledHeight - 80

                mc.textureManager.bindTexture(Gui.ICONS)

                drawTexturedModalRect(x, top, 0, 84, barWidth, 5, 1f)
                if (filled > 0) drawTexturedModalRect(x, top, 0, 89, filled, 5, 1f)

                GlStateManager.enableBlend()
                mc.profiler.endSection()
                GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f)
            }
        }
    }

    /*@SubscribeEvent
    fun drawInventory(event: GuiContainerEvent.DrawForeground) {
        val ct = event.guiContainer as? GuiInventory ?: return

    }*/
}

@SideOnly(Side.SERVER)
@Mod.EventBusSubscriber(modid = BlueRPG.MODID, value = [Side.SERVER])
object ServerEventHandler {
    private val regex = "[\\[(](i|item)[])]".toRegex()

    @SubscribeEvent
    @JvmStatic
    fun messageSent(event: ServerChatEvent) {
        // [i],(i),[item] and (item)
        if (event.message.contains(regex)) {
            val s = event.component.formattedText.split(regex, 2)
            val component = TextComponentTranslation(s[0])
            component.appendSibling(event.player.heldItemMainhand.textComponent)
            s.asSequence().drop(1).forEach { component.appendText(it) }
            event.component = component
        }
    }
}