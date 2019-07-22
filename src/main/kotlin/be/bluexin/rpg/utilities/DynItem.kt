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

package be.bluexin.rpg.utilities

import be.bluexin.rpg.devutil.config
import be.bluexin.rpg.stats.StatCapability
import com.google.gson.JsonObject
import com.teamwizardry.librarianlib.core.client.ModelHandler
import com.teamwizardry.librarianlib.features.base.ICustomTexturePath
import com.teamwizardry.librarianlib.features.base.IModelGenerator
import com.teamwizardry.librarianlib.features.base.item.IModItemProvider
import com.teamwizardry.librarianlib.features.base.item.ItemMod
import com.teamwizardry.librarianlib.features.kotlin.jsonObject
import com.teamwizardry.librarianlib.features.kotlin.tagCompound
import com.teamwizardry.librarianlib.features.saving.AbstractSaveHandler
import com.teamwizardry.librarianlib.features.saving.NamedDynamic
import com.teamwizardry.librarianlib.features.saving.Savable
import com.teamwizardry.librarianlib.features.saving.Save
import com.teamwizardry.librarianlib.features.utilities.FileDsl
import com.teamwizardry.librarianlib.features.utilities.getPathForItemModel
import net.minecraft.item.Item
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack

class DynItem private constructor(suffix: String) : ItemMod("dynamic_$suffix"), IModelGenerator, ICustomTexturePath {
    override fun texturePath(variant: String) = "items/dynamic/${variant.replace("dynamic_", "")}"

    companion object {
        operator fun get(str: String) = dynamics[str]

        operator fun iterator() = dynamics.values.iterator()

        private val dynamics = config.dynamicItems.items.associate { it to DynItem(it) }
    }

    override fun generateMissingItem(item: IModItemProvider, variant: String): Boolean {
        ModelHandler.generateItemJson(this) {
            getPathForItemModel(this, variant) to
                    this@DynItem.generateBaseItemModel(this, variant)
        }
        return true
    }

    private fun generateBaseItemModel(
        item: FileDsl<Item>,
        variantName: String? = null,
        parent: String = "item/generated"
    ): JsonObject {
        val varname = variantName ?: item.key.path
        if (item is ItemBlock) return jsonObject { "parent"("${item.key.namespace}:block/$varname") }
        return generateRegularItemModel(item, variantName, parent)
    }

    private fun generateRegularItemModel(
        item: FileDsl<Item>,
        variantName: String? = null,
        parent: String = "item/generated"
    ): JsonObject {
        val varname = variantName ?: item.key.path
        val path = (item.value as? ICustomTexturePath)?.texturePath(varname) ?: "items/$varname"
        return jsonObject {
            "parent"(parent)
            "textures" {
                "layer0"("${item.key.namespace}:$path")
                "layer1"("${item.key.namespace}:${path}_overlay")
            }
        }
    }
}

data class DynamicConfig(
    val items: List<String> = listOf()
) {
    @Suppress("unused")
    private val comment: String = "List of keys to use for dynamic item generation."
}

val ItemStack.dynamicData
    get() = if (item is DynItem) {
        DynamicData().also {
            if (tagCompound != null && tagCompound!!.getCompoundTag("dynamic").hasKey(
                    "auto",
                    10
                )
            ) AbstractSaveHandler.readAutoNBT(
                it,
                tagCompound!!.getCompoundTag("dynamic").getCompoundTag("auto"),
                false
            )
        }
    } else null

@Savable
@NamedDynamic(resourceLocation = "b:dd")
data class DynamicData(
    @Save var primaryColor: Int = 0xe90d0d,
    @Save var secondaryColor: Int = 0x0e61e9
) : StatCapability {
    fun loadFrom(stack: ItemStack, other: DynamicData) {
        val tag = stack.tagCompound
        val newTag = tagCompound {
            "dynamic" to tagCompound {
                "auto" to AbstractSaveHandler.writeAutoNBT(other, false)
            }
        }
        tag?.merge(newTag)
        stack.tagCompound = tag ?: newTag
    }

    override fun copy() = this.copy(primaryColor = primaryColor)
}