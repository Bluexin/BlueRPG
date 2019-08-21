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
import be.bluexin.rpg.devutil.RNG
import be.bluexin.rpg.skills.glitter.PacketGlitter
import be.bluexin.rpg.skills.glitter.PacketLightning
import be.bluexin.rpg.stats.*
import net.minecraft.init.MobEffects
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import net.minecraftforge.event.RegistryEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@Mod.EventBusSubscriber(modid = BlueRPG.MODID)
object DummySkills {

    private val LEAP_BUFF = BuffType(
        "leap_speed", BuffInfo(
            SecondaryStat.SPEED,
            "13b8af93-9ade-4eed-91bc-7ccdb5583280",
            .15,
            Operation.MULTIPLY_EXPONENTIAL
        )
    )

    @SubscribeEvent
    @JvmStatic
    fun registerSkills(event: RegistryEvent.Register<SkillData>) {
        event.registry.registerAll(
            SkillData(
                ResourceLocation("faerunskills", "skill_0"),
                mana = 10,
                cooldown = 60,
                magic = true,
                levelTransformer = LevelModifier(.1f, .1f),
                processor = Processor(
                    Use(10),
                    Projectile(
                        args = Projectile.Args(velocity = 1.5f, condition = RequireStatus(Status.AGGRESSIVE)),
                        projectileInfo = ProjectileInfo(
                            color1 = 0x0055BB, color2 = 0x0085BB,
                            trailSystem = ResourceLocation(BlueRPG.MODID, "ice")
                        )
                    ),
                    RequireStatus(Status.AGGRESSIVE),
                    MultiEffect(arrayOf(
                        Damage { (caster, _), _ ->
                            /*caster[PrimaryStat.INTELLIGENCE] * 2f *
                                    RNG.nextDouble(.95, 1.05) + RNG.nextInt(3)*/
                            1.0
                        }, Threat { _, _ -> Double.MAX_VALUE }
                    ))
                ),
                uuid = arrayOf()
            ),
            SkillData(
                ResourceLocation(BlueRPG.MODID, "skill_1"),
                mana = 10,
                cooldown = 60,
                magic = true,
                levelTransformer = LevelModifier(0f, 0f),
                processor = Processor(
                    trigger = Use({ (_, level) -> 20 - MathHelper.clamp(level - 1, 0, 2) * 5 }),
                    targeting = Projectile(
                        args = Projectile.Args(
                            velocity = .8f,
                            inaccuracy = 2.5f, precise = true
                        ),
                        projectileInfo = ProjectileInfo(
                            color1 = 0xFFDD0B, color2 = 0xFF0000,
                            trailSystem = ResourceLocation(BlueRPG.MODID, "embers")
                        )
                    ),
                    condition = RequireStatus(Status.AGGRESSIVE),
                    effect = Skill(
                        AoE(clientInfo = TargetingInfo { aoe, ctx, from ->
                            if (from is TargetWithPosition) PacketGlitter(
                                PacketGlitter.Type.AOE,
                                from.pos,
                                0xFFDD0B,
                                0xFF0000,
                                aoe.range(ctx) / 5
                            )
                            else null
                        }),
                        RequireStatus(Status.AGGRESSIVE),
                        Damage { (caster, _), _ ->
                            caster[PrimaryStat.STRENGTH] * 2f *
                                    RNG.nextDouble(1.05, 1.15) + RNG.nextInt(6)
                        }
                    )
                ),
                uuid = arrayOf()
            ),
            SkillData(
                ResourceLocation(BlueRPG.MODID, "skill_2"),
                mana = 10,
                cooldown = 60,
                magic = true,
                levelTransformer = LevelModifier(0f, 0f),
                processor = Processor(
                    Use(10),
                    Self(TargetingInfo { _, _, from ->
                        if (from is TargetWithPosition) PacketGlitter(
                            PacketGlitter.Type.HEAL,
                            from.feet,
                            0x2CB000,
                            0x8EE807,
                            .4
                        ) else null
                    }),
                    RequireStatus(Status.FRIENDLY),
                    Damage { (caster, _), _ ->
                        -(caster[PrimaryStat.WISDOM] * 2f *
                                RNG.nextDouble(.95, 1.05) + RNG.nextInt(3))
                    }
                ),
                uuid = arrayOf()
            ),
            SkillData(
                ResourceLocation(BlueRPG.MODID, "skill_3"),
                mana = 10,
                cooldown = 60,
                magic = true,
                levelTransformer = LevelModifier(0f, 0f),
                processor = Processor(
                    Use(15),
                    Self(TargetingInfo { _, _, from ->
                        if (from is TargetWithPosition) PacketGlitter(
                            PacketGlitter.Type.AOE,
                            from.feet,
                            0xA4D9F7,
                            0xCADDE8,
                            .4
                        ) else null
                    }),
                    null,
                    MultiEffect(
                        arrayOf(
                            Velocity { _, target ->
                                if (target is TargetWithLookVec) Vec3d.fromPitchYaw(-15f, target.yaw)
                                else Vec3d.ZERO
                            },
                            ApplyBuff { (_, skillLevel), _ ->
                                Buff(LEAP_BUFF, 20 + 10 * skillLevel, skillLevel - 1)
                            })
                    )
                ),
                uuid = arrayOf()
            ),
            SkillData(
                ResourceLocation(BlueRPG.MODID, "skill_4"),
                mana = 10,
                cooldown = 60,
                magic = true,
                levelTransformer = LevelModifier(0f, 0f),
                processor = Processor(
                    Use(10),
                    Raycast(range = 15.0),
                    RequireStatus(Status.AGGRESSIVE),
                    MultiEffect(
                        arrayOf(
                            Damage(OnHitInfo { _, from, to ->
                                if (from is TargetWithPosition && to is TargetWithPosition) PacketLightning(
                                    from.pos,
                                    to.pos
                                ) else null
                            }) { (caster, _), _ ->
                                caster[PrimaryStat.DEXTERITY] * 2f * RNG.nextDouble(.95, 1.05) + RNG.nextInt(3)
                            },
                            Skill(
                                Chain(args = Chain.Args(delayMillis = 100)),
                                RequireStatus(Status.AGGRESSIVE),
                                Damage(OnHitInfo { _, from, to ->
                                    if (from is TargetWithPosition && to is TargetWithPosition) PacketLightning(
                                        from.pos,
                                        to.pos
                                    ) else null
                                }) { (caster, _), _ ->
                                    caster[PrimaryStat.DEXTERITY] * RNG.nextDouble(.95, 1.05) + RNG.nextInt(3)
                                }
                            )
                        )
                    )
                ),
                uuid = arrayOf()
            ),
            SkillData.passive(
                ResourceLocation(BlueRPG.MODID, "skill_5"),
                targeting = AoE(),
                condition = RequireStatus(Status.AGGRESSIVE),
                effect = ApplyBuff { (_, level), _ ->
                    Buff(MobEffects.POISON, 25 shr (level - 1), level - 1)
                },
                uuid = arrayOf()
            ),
            SkillData.passive(
                ResourceLocation(BlueRPG.MODID, "skill_6"),
                targeting = AoE(),
                condition = RequireStatus(Status.AGGRESSIVE),
                effect = ApplyBuff { (_, level), _ ->
                    Buff(MobEffects.POISON, 25 shr (level - 1), level - 1)
                },
                uuid = arrayOf()
            )
        )
    }

}