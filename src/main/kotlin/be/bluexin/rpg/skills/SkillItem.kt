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
import be.bluexin.rpg.classes.playerClass
import be.bluexin.rpg.devutil.get
import com.teamwizardry.librarianlib.features.base.ICustomTexturePath
import com.teamwizardry.librarianlib.features.base.IExtraVariantHolder
import com.teamwizardry.librarianlib.features.base.item.ItemMod
import com.teamwizardry.librarianlib.features.helpers.getNBTString
import com.teamwizardry.librarianlib.features.helpers.setNBTString
import com.teamwizardry.librarianlib.features.kotlin.Minecraft
import com.teamwizardry.librarianlib.features.kotlin.localize
import net.minecraft.client.renderer.block.model.ModelResourceLocation
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation
import net.minecraft.world.World
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

object SkillItem : ItemMod("skill_item"), IExtraVariantHolder, ICustomTexturePath {

    val unknown get() = ItemStack(this)
    operator fun get(skill: SkillData) = ItemStack(this).apply { setNBTString("skill", skill.key.toString()) }
    operator fun get(skill: ResourceLocation) = ItemStack(this).apply { setNBTString("skill", skill.toString()) }

    private val ItemStack.skillName
        get() = ResourceLocation(
            this.getNBTString("skill") ?: "${BlueRPG.MODID}:unknown_skill"
        )
    val ItemStack.skill get() = SkillRegistry[this.skillName]

    override fun getTranslationKey(stack: ItemStack) =
        "rpg.skill.${(stack.skill?.name ?: "${BlueRPG.MODID}:unknown_skill")}"

    override val extraVariants by lazy {
        arrayOf(
            "${BlueRPG.MODID}:skill/unknown_skill",
            *SkillRegistry.keys.map { it.modelPath }.toTypedArray()
        )
    }

    override val meshDefinition: ((stack: ItemStack) -> ModelResourceLocation)? =
        {
            ModelResourceLocation(
                ResourceLocation(it.skill?.name ?: "${BlueRPG.MODID}:unknown_skill").modelPath,
                "inventory"
            )
        }

    override fun addInformation(stack: ItemStack, worldIn: World?, tooltip: MutableList<String>, flagIn: ITooltipFlag) {
        val player = Minecraft().player
        stack.skill?.apply {
            tooltip += "rpg.skill.level".localize(player.playerClass[this])
            tooltip += "rpg.skill.$name.description".localize()
            tooltip += "rpg.skill.manacost".localize(manaCost(player), manaCostReduced(player))
            tooltip += "rpg.skill.cooldown".localize(cooldown(player), cooldownReduced(player))
            tooltip += "rpg.skill.casttime".localize(
                processor.trigger.castTimeTicks(
                    SkillContext(
                        player,
                        player.playerClass[this]
                    )
                )
            )
        }
        if (flagIn.isAdvanced) tooltip += stack.skillName.toString()
    }

    @SideOnly(Side.CLIENT)
    override fun showDurabilityBar(stack: ItemStack): Boolean {
        return (stack.skill ?: return false) in Minecraft().player.cooldowns
    }

    @SideOnly(Side.CLIENT)
    override fun getDurabilityForDisplay(stack: ItemStack): Double {
        return 1.0 - Minecraft().player.cooldowns[(stack.skill ?: return .0), Minecraft().renderPartialTicks]
    }

    private val ResourceLocation.modelPath get() = "$namespace:skill/$path"

    override fun texturePath(variant: String) = "skills/${variant.substringAfter('/')}"

/*override fun onPlayerStoppedUsing(stack: ItemStack, worldIn: World, entityLiving: EntityLivingBase, timeLeft: Int) {
        super.onPlayerStoppedUsing(stack, worldIn, entityLiving, timeLeft)
    }*/ // Could be used for special effect when stopping spell before the end?
}