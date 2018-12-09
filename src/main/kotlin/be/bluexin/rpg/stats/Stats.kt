/*
 * Copyright (C) 2018.  Arnaud 'Bluexin' Solé
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

package be.bluexin.rpg.stats

import be.bluexin.rpg.BlueRPG
import be.bluexin.rpg.events.StatChangeEvent
import be.bluexin.rpg.gear.GearType
import be.bluexin.rpg.gear.Rarity
import be.bluexin.rpg.util.fire
import com.teamwizardry.librarianlib.features.kotlin.localize
import com.teamwizardry.librarianlib.features.saving.NamedDynamic
import com.teamwizardry.librarianlib.features.saving.Savable
import com.teamwizardry.librarianlib.features.saving.Save
import com.teamwizardry.librarianlib.features.saving.SaveInPlace
import net.minecraft.entity.SharedMonsterAttributes
import net.minecraft.entity.ai.attributes.IAttribute
import net.minecraft.entity.ai.attributes.RangedAttribute
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.EntityEquipmentSlot
import net.minecraftforge.fml.common.eventhandler.Event
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import java.lang.ref.WeakReference
import java.text.DecimalFormat
import java.util.*

@SaveInPlace
class StatsCollection(private val reference: WeakReference<out Any>, @Save internal var collection: HashMap<Stat, Int> = HashMap()) {
    // TODO: move collection to `val` and `withDefault` (instead of getOrDefault) once liblib updates

    operator fun get(stat: Stat): Int = collection.getOrDefault(stat, 0)
    operator fun set(stat: Stat, value: Int): Boolean {
        val r = reference.get()
        val evt = if (r is EntityPlayer) {
            StatChangeEvent(r, stat, collection.getOrDefault(stat, 0), value)
        } else null

        return if (evt == null || (fire(evt) && evt.result != Event.Result.DENY)) {
            if (evt?.newValue ?: value != 0) collection[stat] = evt?.newValue ?: value
            else collection.remove(stat)
            if (r is EntityPlayer) {
                r.getEntityAttribute(stat.attribute).baseValue = evt!!.newValue.toDouble()
            }
            dirty()
            true
        } else false
    }

    operator fun invoke() = collection.asSequence()

    operator fun iterator() = (collection as Map<Stat, Int>).iterator()

    fun copy() = StatsCollection(WeakReference<Any>(null), HashMap(collection))

    fun load(other: StatsCollection) {
        this.collection = other.collection
    }

    internal var dirty = false
        private set

    internal fun clean() {
        dirty = false
    }

    internal fun dirty() {
        dirty = true
    }

    fun clear() = collection.clear()

    fun isEmpty() = collection.isEmpty()
}

/*
Modifier operations :
    - 0: flat add
    - 1: multiply
    - 2: multiply by (1.0 + x)
 */

operator fun EntityPlayer.get(stat: Stat) =
    if (stat.hasTransform) this.getEntityAttribute(stat.attribute).attributeValue / 100.0
    else this.getEntityAttribute(stat.attribute).attributeValue

@NamedDynamic(resourceLocation = "b:s")
@Savable
interface Stat {

    val name: String

    val uuid: Array<UUID>

    fun uuid(slot: EntityEquipmentSlot) = uuid[slot.slotIndex]

    val attribute: IAttribute

    val hasTransform get() = false

    val shouldRegister get() = true

    val operation get() = 0

    val baseValue get() = 0.0

    operator fun invoke(from: Int) = from.toDouble()

    @SideOnly(Side.CLIENT)
    fun longName() = "rpg.${name.toLowerCase()}.long".localize()

    @SideOnly(Side.CLIENT)
    fun shortName() = "rpg.${name.toLowerCase()}.short".localize()

    @SideOnly(Side.CLIENT)
    fun tooltip() = "rpg.${name.toLowerCase()}.tooltip".localize()

    @SideOnly(Side.CLIENT)
    fun localize(value: Int) =
        if (hasTransform) "rpg.tooltip.pctstat".localize(format.format(this(value)))
        else "rpg.tooltip.flatstat".localize(value)

    @SideOnly(Side.CLIENT)
    fun localize(value: Double) =
        if (hasTransform) "rpg.tooltip.pctstat".localize(format.format(value))
        else "rpg.tooltip.flatstat".localize(value.toInt())

    fun getRoll(ilvl: Int, rarity: Rarity, gearType: GearType, slot: EntityEquipmentSlot): Int
}

private val format = DecimalFormat("#.##")

@NamedDynamic(resourceLocation = "b:ps")
enum class PrimaryStat(uuid: Array<String>) : Stat {
    STRENGTH(
        arrayOf(
            "b31893e3-08c7-463a-9240-cef6e89a7bc0",
            "85edcb6d-cd6a-4dc5-8f9b-c58a44ee9ca7",
            "b42e78d9-07e7-4b49-9712-f12c8b9f3825",
            "fdc7f52a-0b5b-4fd6-a59c-ad8799341e10",
            "2d91cf71-186a-4395-9d95-23294bfee38b",
            "ed4f1ee7-afcf-4879-be1c-d009dc0c0aab"
        )
    ),
    CONSTITUTION(
        arrayOf(
            "2e83cd49-1166-45a8-95b7-a08a44e2768d",
            "b8775d3b-cc9e-454a-9592-257666bb928c",
            "aa9385aa-2104-4524-b360-ee1fae77fe0c",
            "42d44b03-d001-4a0a-af29-4eebe1d3df02",
            "13d3fadb-0a7f-4bee-9763-d87790397580",
            "970813d3-7704-4c51-a559-cd74e2be6da7"
        )
    ),
    DEXTERITY(
        arrayOf(
            "9df6b30f-e0b1-43c4-9008-69024116e8df",
            "8de7d267-5a06-4511-837a-20ef368bd088",
            "471f114c-6521-48ce-8b13-1e6dce4aea02",
            "db4667db-b133-4293-8ec1-587d6955ded5",
            "6f9659cb-b50a-4bd9-aa8c-24f5a58ce75e",
            "2699519d-0f9f-493b-82b4-d3c90167ef87"
        )
    ),
    INTELLIGENCE(
        arrayOf(
            "b7ac00d8-8a30-4e60-a651-1132fdaa5a27",
            "f0ad0105-e57a-4714-89c1-ce28277218e5",
            "b1b6effb-3a07-43eb-aed7-4279c0bc2bb7",
            "221117fe-1a96-4175-9ece-e031d2119107",
            "6469bd46-4307-4251-80b4-352b52cece93",
            "cc93037d-94ef-425f-a487-ce4fb6ed0652"
        )
    ),
    WISDOM(
        arrayOf(
            "63f17582-142d-4a82-bd1c-0848e27d51b0",
            "a63dd4b9-5783-4bac-ab29-77803624254d",
            "38016a19-84ed-4fbe-ad21-f0b70d1adaeb",
            "e89cf1ba-8aae-4b93-8b2e-baad3b7da3cf",
            "5a49481f-d521-434c-9439-07f65a4ab2ac",
            "66b1929f-0186-4202-b2ec-df4457f9ce44"
        )
    ),
    CHARISMA(
        arrayOf(
            "2f7e523b-728d-4a87-aea0-3bfbc07652f4",
            "c221bd4f-bf24-4d2f-bd8a-255ccdc8181d",
            "1840d568-b793-497a-a439-4e2118d642e7",
            "0e752198-d170-47a5-b5f6-8875b46d2434",
            "a3a64f77-3779-4ded-b1a6-883572d30bc6",
            "bb9c6505-a00c-49fb-bfb2-f25d2f8db3fe"
        )
    );

    override val attribute: IAttribute by lazy {
        RangedAttribute(
            null,
            "${BlueRPG.MODID}.${this.name.toLowerCase()}",
            baseValue,
            0.0,
            Double.MAX_VALUE
        ).setShouldWatch(true)
    }

    override val uuid = uuid.map { UUID.fromString(it) }.toTypedArray()

    override fun getRoll(ilvl: Int, rarity: Rarity, gearType: GearType, slot: EntityEquipmentSlot) =
        FormulaeConfiguration(this, ilvl, rarity, gearType, slot).roll()
}

@NamedDynamic(resourceLocation = "b:ss")
enum class SecondaryStat(uuid: Array<String>, attribute: IAttribute? = null) : Stat {
    REGEN(
        arrayOf(
            "6bd4b8bf-d010-4a12-8cff-326bd4f9a377",
            "3ac37444-6228-4a66-8bde-f5917f164df3",
            "8ab77c60-14fc-4a0e-8834-2f4d26d5fffc",
            "d6f13896-287b-4d42-8a3f-47d12b12441e",
            "82bb57a2-f46e-434d-b078-ed24cfc362b1",
            "3cc65b37-8cac-4677-ab1b-d2fd5703a30e"
        )
    ) {
        override val hasTransform = false
        override fun invoke(from: Int) = from.toDouble()
        override val baseValue: Double get() = 2.0
    },
    SPIRIT(
        arrayOf(
            "57b18a8e-3999-4704-9f2e-af95d4a000fe",
            "a17e6e2e-bd75-4bf5-baf1-a0d8a02c1eee",
            "92bcccfa-94e2-4d1e-b668-7b5072b7ccf9",
            "bee3503f-14ec-4032-9844-5b15b0654c06",
            "ea0c929a-da3e-40df-88c0-8c8e17648073",
            "fe36dd53-9dd5-43b0-9ef2-ab2e91e177e3"
        )
    ) {
        override val hasTransform = false
        override fun invoke(from: Int) = from.toDouble()
        override val baseValue: Double get() = 1.0
    },
    REFLECT(
        arrayOf(
            "984d0c7a-b406-47ce-b956-2193d0146184",
            "21e8de74-b983-4949-8415-bbfb42816d2f",
            "c22565af-a5ec-4635-9b09-ea9cc2dbf8b0",
            "fa6f2aff-7e4a-46b4-8a4c-fad1653a1840",
            "6a37c0f5-d53c-4749-a901-9324a28b69af",
            "dae6972d-5722-4bf2-a85b-1917e1350535"
        )
    ),
    BLOCK(
        arrayOf(
            "05a6f1a0-20e7-4cc0-97fe-4700f4647f23",
            "91e17900-377c-490a-86ac-a8a349bf183e",
            "d4fa48e5-c03d-4073-9690-e9253f5386c3",
            "70a243e7-d9d3-423a-bb7e-ee28f2189eb6",
            "53d80a13-143d-4ddf-bdb8-eb4c652c483e",
            "beac044d-822a-4e98-b050-e6f4620bc44c"
        )
    ),
    DODGE(
        arrayOf(
            "676fef05-9038-4bb1-8fd7-037c2f124b4e",
            "84a9c875-4067-46ab-ba51-6a539ea61da6",
            "53c5cead-888c-428b-b4b8-4b2e2710d19c",
            "450b04eb-6a64-42f3-8fbe-7b1f59692231",
            "6bef0de3-a540-4cb1-9257-0b9a00b36565",
            "b3e605ff-18c3-4455-8799-a18d1f4a6e76"
        )
    ),
    PARRY(
        arrayOf(
            "1dae30c2-49db-402b-aaab-b229f44405a0",
            "a8ae835a-bbce-48cf-8bd5-abeace6b6a51",
            "a30cd452-95cc-4f36-a67b-9da26864729d",
            "08e11150-06f8-41e3-84bb-7c8c8fa618fa",
            "ac99c4b0-ef00-4d59-96ef-edacefd91791",
            "cac0ca1b-ad3a-4497-b6d3-fdadd696e647"
        )
    ),
    SPEED(
        arrayOf(
            "ffb8012b-289a-45ed-b8cb-a477366b58b0",
            "44f0e78f-b319-47b8-b489-47227056ea4e",
            "d46ceb87-57c9-4c82-9ac2-204280628c0d",
            "1b87c3ae-ac43-4e97-98b5-fb21a5d7a5dd",
            "b3969cc1-de3f-484e-bea2-b4107706f191",
            "fdf24012-758d-4c7b-8672-d2f67751c4c8"
        ), SharedMonsterAttributes.MOVEMENT_SPEED
    ) {
        override val operation = 1
        override fun localize(value: Double) = "rpg.tooltip.pctstat".localize(format.format(value * 1_000))
    },
    LIFE_STEAL_CHANCE(
        arrayOf(
            "b70eaf8a-2237-4310-bb03-84f3a174119f",
            "dc420c58-5f59-4809-acd5-6f1f4cc5a611",
            "6d6945cc-9d4d-4c65-91f5-37d2f30df1ac",
            "5ea503a6-ab12-4562-9ae0-ee9844cd7d29",
            "105e4c8f-5e25-4198-b0ac-8f33902d6843",
            "58bbd385-f64f-4798-b9a2-5ecd6465662f"
        )
    ),
    LIFE_STEAL(
        arrayOf(
            "c294068d-66e8-43ec-a4f9-e09e4a6ef18c",
            "e853b15a-f5cf-4b05-9df9-083366a2cdf0",
            "e6518c6a-b2c2-46a6-b3c6-eb1a61dd5238",
            "46aebbed-8392-4f23-94ab-923dd8bb45c2",
            "6a97bbcb-3c98-4c23-9169-818c0d00706f",
            "8f753b5a-1b3d-44f5-ba78-356af8092463"
        )
    ),
    MANA_LEECH_CHANCE(
        arrayOf(
            "7b0414b7-f7ac-4f14-acd1-4f05dc80b409",
            "2eb18cf9-ba4a-451f-89d0-d7fac4a4ac7a",
            "de67156a-3b47-492e-abba-aff362475a25",
            "e58e8669-fe93-4dc6-ad73-1e5ca0eda273",
            "a2078044-46ca-4678-ba0d-48d9d72504eb",
            "300636a6-781a-430e-90dd-b7f8ece76af3"
        )
    ),
    MANA_LEECH(
        arrayOf(
            "6cbedbfb-f646-4d52-bb36-3f9c0700a6dd",
            "fea17355-3191-41db-aa7c-d630de48db19",
            "c412512d-bd43-4073-8ace-f2c35a6f03de",
            "21dc8175-9d69-4e19-88fb-5aaa08e36ca6",
            "1cf5d2c7-3a1a-4f7d-8bb3-744ee612f236",
            "9e470c3c-c6e5-4d3c-8275-133842e22c38"
        )
    ),
    CRIT_CHANCE(
        arrayOf(
            "a7d38693-a05a-4897-99a9-abb6197af11f",
            "c7c29237-ee03-44d0-b4e4-28d5943962cb",
            "af7e8480-4480-41a9-b794-dabdfaa96078",
            "a269aabb-228e-4c81-8fbd-f1bb01fedf3b",
            "7015dcee-1b3c-49cb-8c3b-2469e1cbc645",
            "100b159f-d86c-48d2-ba15-79e083ae5665"
        )
    ) {
        override val baseValue = 5.0
    },
    CRIT_DAMAGE(
        arrayOf(
            "4d760921-f4be-47a6-b002-3fac0a668afa",
            "b362a556-95e6-4c4d-8be2-68968f1cf4e2",
            "d757b41f-6680-40b5-91f4-aea0c8e8a074",
            "18477507-b7f3-4035-b083-1602f6662528",
            "b222a806-4534-420a-b1b6-f981f63164cf",
            "b73977f3-4bc2-4bae-98fc-d44da61610f2"
        )
    ) {
        override val baseValue = 20.0
    },
    /*BONUS_TO_SKILL(
        arrayOf(
            "f0653db6-8aad-4476-970e-f0d8f85ab83c",
            "f4ff68f9-b44f-4242-adfd-c584fb2fb493",
            "0c7ab09c-e0e9-4ed6-ac41-e77fa0bdb0a8",
            "03924abc-22b4-406a-9662-41d1fd6d2e69",
            "844f40b9-d9fc-46e4-a9c6-88db501891f1",
            "50dc62d1-9d19-421e-aeac-500f131e5527"
        )
    ),*/ // TODO: this stat should be a bonus to a random skill
    BONUS_DAMAGE(
        arrayOf(
            "30c2c5fb-00a4-4a56-b0bc-5dcecb7a58b2",
            "a1c68b27-983a-406a-be29-b6ebba4cb6bc",
            "fafb013a-74f6-45a6-8388-3493bfeab27b",
            "69349aa5-b2ee-49d6-b82d-16defa8d2cf0",
            "e8efd1ca-ef46-4801-8fe6-16cb558213c8",
            "eb9bc66f-f665-46d5-8edc-42e6b7773de6"
        ), SharedMonsterAttributes.ATTACK_DAMAGE
    ) {
        override val hasTransform = false
        override fun invoke(from: Int) = from.toDouble()
    },
    INCREASED_DAMAGE(
        arrayOf(
            "a38ca36c-5671-4785-82f8-c713b86f2dca",
            "33324987-0f12-4971-90aa-db20bc48bb47",
            "02f5a9c3-54b1-4cf0-a4db-4179f49e88f9",
            "d1bc5bd9-a1ce-425e-adee-2d3b53d03722",
            "1c73ea3e-466b-44c6-851d-64f48453adc7",
            "b2d44f09-bf35-46b2-a592-f3d3b4e8b09d"
        )
    ),
    RESISTANCE(
        arrayOf(
            "d9a1485c-1c8a-43ca-9055-f59665064402",
            "dad33cc5-ccba-4db7-82cb-955260261e4b",
            "866c3b01-881d-4f87-b99d-ce5bc503f6ad",
            "1ae9c08c-967f-4fb0-af8f-b34bb1d61d4e",
            "81b44d11-d95a-4e4e-acad-8cd09dca83f5",
            "09130b59-4136-4187-9f93-f87cc5726c69"
        )
    ),
    ROOT(
        arrayOf(
            "f35a9dd9-e88b-4e0b-b915-dd49ec1b0c3d",
            "15a600b3-d2a1-4f7e-9406-d9ce1648d92c",
            "5c83c42d-49e9-4d8e-951f-b025de59ee4b",
            "97a593eb-0d66-4057-861e-9f92c210f542",
            "4e74d178-096f-4a17-856d-4de7dd6fbd4e",
            "1e0cc3a1-44de-4768-bf87-40b38e2cecfe"
        )
    ),
    SLOW(
        arrayOf(
            "03aee4f1-b4fb-44aa-9024-fa7a3a052121",
            "b063c763-c5d4-4011-bc7f-63d7d01a1d12",
            "37be1c6e-8682-4c3c-a4c5-1385f1bbc41c",
            "26fb4414-ff43-442f-8af2-1eb899749fa2",
            "373cd837-6597-494c-b09b-2c6cccdfa2ab",
            "e2414446-bd51-44eb-b7a0-b6a05cad159d"
        )
    ),
    COOLDOWN_REDUCTION(
        arrayOf(
            "fe29948b-176f-49ba-b36b-3dbd3be1136e",
            "a4158d5f-b7d8-4802-a91e-86318e2d0774",
            "7f3dc837-21cc-4cd9-af7c-d1d13cfb3fb5",
            "61183fa7-788e-4c7e-95ca-be20da54e24c",
            "e589ab2a-df8a-4514-ba0a-f212c8935c65",
            "963e0750-a4bf-40b9-a35f-1e859e20f789"
        )
    ),
    MANA_REDUCTION(
        arrayOf(
            "8fdf1c14-7677-4622-8979-5a639df7b258",
            "23d5fad1-2b3f-4d06-a318-364cebc67e82",
            "ca810c86-30f4-44dd-a763-a03109be778d",
            "1cace5d1-2970-4b83-b6d8-e9386fb61644",
            "26514f55-6b5b-4384-ad3a-10f48ece3c13",
            "8942fb83-0b0f-4d59-a306-90032ebb7cd9"
        )
    );

    override val shouldRegister = attribute == null

    override val attribute: IAttribute by lazy {
        attribute
            ?: RangedAttribute(
                null,
                "${BlueRPG.MODID}.${this.name.toLowerCase()}",
                baseValue,
                0.0,
                Double.MAX_VALUE
            ).setShouldWatch(true)
    }

    override val uuid = uuid.map { UUID.fromString(it) }.toTypedArray()

    override fun getRoll(ilvl: Int, rarity: Rarity, gearType: GearType, slot: EntityEquipmentSlot) =
        FormulaeConfiguration(this, ilvl, rarity, gearType, slot).roll()

    override val hasTransform = true

    override fun invoke(from: Int) = from / 100.0
}

@NamedDynamic(resourceLocation = "b:fs")
enum class FixedStat(uuid: Array<String>, attribute: IAttribute? = null) : Stat {
    HEALTH(
        arrayOf(
            "2936978a-93f4-4aad-b96c-9dd558f09323",
            "e2abbc4f-6b45-47d1-8a70-df6d26cfbc3e",
            "895b051d-d387-4e6e-acd7-cc7e72d70785",
            "d77f78b3-6660-41e4-ad4a-ac03aa1b2dae",
            "b5b44c14-e9a8-4a1a-959f-946afbcf8cc8",
            "1c770de6-adf9-4a81-b1d8-baf0002334ec"
        ), SharedMonsterAttributes.MAX_HEALTH
    ),
    PSYCHE(
        arrayOf(
            "596b8658-6e31-479b-9d8c-75312bf7fde9",
            "3a44ded3-8f78-4e0f-a889-9a3943235bbb",
            "f502aa88-f9e7-4140-b257-a8d5470537c3",
            "cf9d3c18-5acf-4e78-8464-5c3840a2b952",
            "45779a50-cf0b-44fa-b972-17dda742287c",
            "48c52ea4-9a41-461e-a565-75b7d8aa987a"
        )
    ) {
        override val baseValue = 20.0
    },
    ARMOR(
        arrayOf(
            "1293882c-9486-4c18-a569-6a0918e440fb",
            "8c8c8869-fcfc-4094-88c6-2cad8a1977b4",
            "c9c1095d-b3ec-47db-b141-4f9e78d74735",
            "d7cacca9-a5a4-40ea-b9d1-b8b515d68a19",
            "d69de06a-57bd-4eaf-a57d-7c0ee9107b95",
            "2b5352df-631f-4a9e-84ba-3da4991b710d"
        )
    ) {
        override val hasTransform = true
        override fun invoke(from: Int) = from / 100.0
    },
    BASE_DAMAGE(
        arrayOf(
            "7cf9450f-e5f9-4187-890a-0eeaf334df30",
            "9c213a49-915a-40ea-8310-a7a55ddc0738",
            "cb535958-064d-4355-bca9-b50aec7ba414",
            "42a79bc6-9008-478f-a49f-d02f56430369",
            "9eea25e6-61b3-4626-9700-e6f1e5c8b64b",
            "d95e73c0-bb66-41e9-9107-182f3b8707cd"
        )
    ),
    MAX_DAMAGE(
        arrayOf(
            "42552520-b1d3-4162-9402-894b2fdaaa42",
            "d25ed4fd-c86c-41c3-aa45-3cab05c8dc29",
            "61076768-17b3-45f4-83ef-fd7742dbab8f",
            "d6fbfbfb-9e4e-43d2-aae3-537a8e490aac",
            "984c2e1d-f3e3-4b84-a89b-c8d55b299a98",
            "c7499cf3-8fe5-4db2-ad70-aac8b8bd3439"
        )
    ),
    F_CRIT_CHANCE(
        arrayOf(
            "284dd4e5-51d9-4b95-81ac-901c1a1aaf40",
            "fe2f7dfa-02f0-4669-a927-cdaf4b558733",
            "02271477-d16f-4e46-b233-6159e3c6a78e",
            "8a653af6-0522-448d-8c0b-003303278329",
            "40eddb9d-3f59-4226-890d-203b340a2315",
            "8d7c671b-d3c1-4fe7-944f-a495bedd7277"
        ), SecondaryStat.CRIT_CHANCE.attribute
    ) {
        override val hasTransform = true
        override fun invoke(from: Int) = from / 100.0
    },
    F_PARRY(
        arrayOf(
            "ac805367-b1a0-4072-8a8f-19d64b60fa88",
            "b2fd1831-4678-409e-b487-8c83e4cd8618",
            "068bb180-4d3e-49d0-b185-7ac94b2992b5",
            "6b361f13-2fdd-4a8e-989b-62fb7b4ac0ac",
            "c5211798-006b-4ace-b73d-7e39c67e346e",
            "849136e6-b673-4b71-bba4-61b118b275a1"
        ), SecondaryStat.PARRY.attribute
    ) {
        override val hasTransform = true
        override fun invoke(from: Int) = from / 100.0
    },
    F_BLOCK(
        arrayOf(
            "8f2672ec-5aef-4b19-9428-e04a4cf9f470",
            "7b0cc484-0848-4b42-b0ef-664a754107f6",
            "0fa3e038-3358-4efe-a7ab-b8bbc60e3354",
            "54e42766-fb1c-4602-a5fc-53eeeb40b215",
            "dfef3323-e0f5-486f-9fb6-8adba23a486d",
            "29a047e0-b6bc-4fef-a02e-f95dbd7e6cf9"
        ), SecondaryStat.BLOCK.attribute
    ) {
        override val hasTransform = true
        override fun invoke(from: Int) = from / 100.0
    };

    override val shouldRegister = attribute == null

    override val attribute: IAttribute by lazy {
        attribute ?: RangedAttribute(
            null,
            "${BlueRPG.MODID}.${this.name.toLowerCase()}",
            baseValue,
            0.0,
            Double.MAX_VALUE
        ).setShouldWatch(true)
    }

    override val uuid = uuid.map { UUID.fromString(it) }.toTypedArray()

    override fun getRoll(ilvl: Int, rarity: Rarity, gearType: GearType, slot: EntityEquipmentSlot) =
        FormulaeConfiguration(this, ilvl, rarity, gearType, slot).roll()
}

/**
 *  These stats do not go on gear, and are used to easily hook into formulae loader
 */
enum class TrickStat : Stat {
    DURABILITY,
    REQUIREMENT_CHANCE,
    REQUIREMENT_MULTIPLIER;

    override val uuid: Array<UUID>
        get() = throw IllegalStateException("$this is not a true stat!")
    override val attribute: IAttribute
        get() = throw IllegalStateException("$this is not a true stat!")

    override fun getRoll(ilvl: Int, rarity: Rarity, gearType: GearType, slot: EntityEquipmentSlot) =
        FormulaeConfiguration(this, ilvl, rarity, gearType, slot).roll()
}

@Savable
@NamedDynamic(resourceLocation = "b:sc")
interface StatCapability {
    fun copy(): StatCapability
}

/**
 * Generates us some UUIDs
 */
fun main(args: Array<String>) {
    println(PrimaryStat.values().joinToString(separator = ",\n", postfix = ";") {
        """$it(arrayOf(
        |"${UUID.randomUUID()}",
        |"${UUID.randomUUID()}",
        |"${UUID.randomUUID()}",
        |"${UUID.randomUUID()}",
        |"${UUID.randomUUID()}",
        |"${UUID.randomUUID()}"
        |))
    """.trimMargin()
    })
    println(SecondaryStat.values().joinToString(separator = ",\n", postfix = ";") {
        """$it(arrayOf(
        |"${UUID.randomUUID()}",
        |"${UUID.randomUUID()}",
        |"${UUID.randomUUID()}",
        |"${UUID.randomUUID()}",
        |"${UUID.randomUUID()}",
        |"${UUID.randomUUID()}"
        |))
    """.trimMargin()
    })
    println(FixedStat.values().joinToString(separator = ",\n", postfix = ";") {
        """$it(arrayOf(
        |"${UUID.randomUUID()}",
        |"${UUID.randomUUID()}",
        |"${UUID.randomUUID()}",
        |"${UUID.randomUUID()}",
        |"${UUID.randomUUID()}",
        |"${UUID.randomUUID()}"
        |))
    """.trimMargin()
    })
}