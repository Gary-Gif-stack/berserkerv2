package com.berserker;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterials;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class BerserkerBladeItem extends SwordItem {
	public BerserkerBladeItem(Settings settings) {
		super(ToolMaterials.NETHERITE, settings);
	}

	/** True if the player has the blade in the hotbar or offhand. */
	public static boolean carriesBlade(PlayerEntity player) {
		if (player.getOffHandStack().getItem() instanceof BerserkerBladeItem) return true;
		for (int i = 0; i < 9; i++) {
			if (player.getInventory().getStack(i).getItem() instanceof BerserkerBladeItem) return true;
		}
		return false;
	}

	public static boolean holdsBlade(PlayerEntity player) {
		return player.getMainHandStack().getItem() instanceof BerserkerBladeItem;
	}

	@Override
	public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.translatable("item.berserker.berserker_blade.tooltip.rage").formatted(Formatting.RED));
		tooltip.add(Text.translatable("item.berserker.berserker_blade.tooltip.adrenaline").formatted(Formatting.GOLD));
	}
}
