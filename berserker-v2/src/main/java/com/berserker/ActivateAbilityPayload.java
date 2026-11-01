package com.berserker;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Client -> server: "I pressed an ability key". */
public record ActivateAbilityPayload(int ability) implements CustomPayload {
	public static final int RAGE = 0;
	public static final int ADRENALINE = 1;
	public static final int SLASH = 2;

	public static final CustomPayload.Id<ActivateAbilityPayload> ID =
			new CustomPayload.Id<>(Identifier.of(BerserkerMod.MOD_ID, "activate_ability"));
	public static final PacketCodec<RegistryByteBuf, ActivateAbilityPayload> CODEC = PacketCodec.tuple(
			PacketCodecs.VAR_INT, ActivateAbilityPayload::ability,
			ActivateAbilityPayload::new);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
