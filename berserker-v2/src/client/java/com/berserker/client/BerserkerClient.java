package com.berserker.client;

import com.berserker.ActivateAbilityPayload;
import com.berserker.SyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class BerserkerClient implements ClientModInitializer {
	public static KeyBinding RAGE_KEY;
	public static KeyBinding ADRENALINE_KEY;
	public static KeyBinding SLASH_KEY;

	private static KeyBinding register(String name, int key) {
		return KeyBindingHelper.registerKeyBinding(
				new KeyBinding(name, InputUtil.Type.KEYSYM, key, "category.berserker"));
	}

	private static void poll(MinecraftClient client, KeyBinding key, int ability) {
		while (key.wasPressed()) {
			if (client.player != null) {
				ClientPlayNetworking.send(new ActivateAbilityPayload(ability));
			}
		}
	}

	@Override
	public void onInitializeClient() {
		RAGE_KEY = register("key.berserker.rage", GLFW.GLFW_KEY_R);
		ADRENALINE_KEY = register("key.berserker.adrenaline", GLFW.GLFW_KEY_G);
		SLASH_KEY = register("key.berserker.slash", GLFW.GLFW_KEY_V);

		ClientPlayNetworking.registerGlobalReceiver(SyncPayload.ID,
				(payload, context) -> ClientMeters.current = payload);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			poll(client, RAGE_KEY, ActivateAbilityPayload.RAGE);
			poll(client, ADRENALINE_KEY, ActivateAbilityPayload.ADRENALINE);
			poll(client, SLASH_KEY, ActivateAbilityPayload.SLASH);
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
				ClientMeters.current = SyncPayload.EMPTY);

		HudRenderCallback.EVENT.register(MeterHud::render);
	}
}
