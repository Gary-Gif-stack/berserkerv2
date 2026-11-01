package com.berserker;

import net.fabricmc.api.ModInitializer;

public class BerserkerMod implements ModInitializer {
	public static final String MOD_ID = "berserker";

	@Override
	public void onInitialize() {
		ModItems.init();
		MeterManager.init();
	}
}
