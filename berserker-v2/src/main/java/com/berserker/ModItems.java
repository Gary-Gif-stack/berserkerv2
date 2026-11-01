package com.berserker;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterials;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

public final class ModItems {
	private ModItems() {}

	public static final Item BERSERKER_BLADE = Registry.register(
			Registries.ITEM,
			Identifier.of(BerserkerMod.MOD_ID, "berserker_blade"),
			new BerserkerBladeItem(new Item.Settings()
					.rarity(Rarity.EPIC)
					.fireproof()
					// base 4 + netherite 4 (+1 hand) = 9 damage, attack speed 1.7
					.attributeModifiers(SwordItem.createAttributeModifiers(ToolMaterials.NETHERITE, 4, -2.3f))));

	public static void init() {
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> entries.add(BERSERKER_BLADE));
	}
}
