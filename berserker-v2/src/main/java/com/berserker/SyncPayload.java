package com.berserker;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Server -> client: current meter values for the HUD. */
public record SyncPayload(int rage, int rageActive, int rageCooldown,
						  int adr, int adrActive, int adrCooldown,
						  int slashCooldown) implements CustomPayload {
	public static final CustomPayload.Id<SyncPayload> ID =
			new CustomPayload.Id<>(Identifier.of(BerserkerMod.MOD_ID, "sync"));

	public static final PacketCodec<RegistryByteBuf, SyncPayload> CODEC = new PacketCodec<>() {
		@Override
		public SyncPayload decode(RegistryByteBuf buf) {
			return new SyncPayload(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
					buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
		}

		@Override
		public void encode(RegistryByteBuf buf, SyncPayload p) {
			buf.writeVarInt(p.rage());
			buf.writeVarInt(p.rageActive());
			buf.writeVarInt(p.rageCooldown());
			buf.writeVarInt(p.adr());
			buf.writeVarInt(p.adrActive());
			buf.writeVarInt(p.adrCooldown());
			buf.writeVarInt(p.slashCooldown());
		}
	};

	public static final SyncPayload EMPTY = new SyncPayload(0, 0, 0, 0, 0, 0, 0);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
