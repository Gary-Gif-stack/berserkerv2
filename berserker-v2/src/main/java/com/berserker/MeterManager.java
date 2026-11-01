package com.berserker;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** All server-side logic: filling, activating, ticking, applying bonuses, syncing. */
public final class MeterManager {
	private MeterManager() {}

	private static final Map<UUID, MeterState> STATES = new HashMap<>();

	private static final Identifier DAMAGE_ID = Identifier.of(BerserkerMod.MOD_ID, "berserker_damage");
	private static final Identifier SPEED_ID = Identifier.of(BerserkerMod.MOD_ID, "adrenaline_speed");

	public static MeterState get(ServerPlayerEntity player) {
		return STATES.computeIfAbsent(player.getUuid(), id -> new MeterState());
	}

	public static void init() {
		PayloadTypeRegistry.playC2S().register(ActivateAbilityPayload.ID, ActivateAbilityPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(SyncPayload.ID, SyncPayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(ActivateAbilityPayload.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			switch (payload.ability()) {
				case ActivateAbilityPayload.RAGE -> activateRage(player);
				case ActivateAbilityPayload.ADRENALINE -> activateAdrenaline(player);
				case ActivateAbilityPayload.SLASH -> activateSlash(player);
				default -> { }
			}
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
				tick(player, server.getTicks());
			}
		});

		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (entity instanceof ServerPlayerEntity player
					&& amount > 0
					&& !player.isInvulnerableTo(source)
					&& player.timeUntilRegen <= 10) {
				onHurt(player, amount);
			}
			return true;
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
				STATES.remove(handler.getPlayer().getUuid()));

		// Dying resets everything.
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			if (!alive) STATES.remove(oldPlayer.getUuid());
		});
	}

	// ------------------------------------------------------------------

	private static void onHurt(ServerPlayerEntity player, float damage) {
		MeterState s = get(player);

		// Adrenaline bar vanishes instantly when you get hit (while charging or waiting to be used).
		if (s.adrActive == 0 && s.adrCooldown == 0 && s.adr > 0) {
			s.adr = 0;
			s.dirty = true;
		}

		// Rage fills from damage taken.
		if (BerserkerBladeItem.carriesBlade(player) && s.rageActive == 0 && s.rage < Balance.RAGE_MAX) {
			int gain = Math.max(1, Math.round(damage * Balance.RAGE_PER_DAMAGE));
			s.rage = Math.min(Balance.RAGE_MAX, s.rage + gain);
			s.dirty = true;
			if (s.rage >= Balance.RAGE_MAX && s.rageCooldown == 0) {
				player.sendMessage(Text.literal("Rage is ready!").formatted(Formatting.RED, Formatting.BOLD), true);
				player.getServerWorld().playSound(null, player.getBlockPos(),
						SoundEvents.ENTITY_BLAZE_AMBIENT, SoundCategory.PLAYERS, 1f, 0.6f);
			}
		}
	}

	// ---------------------------- Abilities ---------------------------

	public static void activateRage(ServerPlayerEntity player) {
		if (!BerserkerBladeItem.carriesBlade(player)) return;
		MeterState s = get(player);
		if (s.rageActive > 0) return;

		if (s.rageCooldown > 0) {
			player.sendMessage(Text.literal("Rage on cooldown: " + secs(s.rageCooldown) + "s").formatted(Formatting.GRAY), true);
			return;
		}
		if (s.rage < Balance.RAGE_MAX) {
			player.sendMessage(Text.literal("Rage meter isn't full yet").formatted(Formatting.GRAY), true);
			return;
		}

		s.rage = 0;
		s.rageActive = Balance.RAGE_DURATION;
		s.dirty = true;

		ServerWorld world = player.getServerWorld();
		world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_RAVAGER_ROAR, SoundCategory.PLAYERS, 1.2f, 0.9f);
		world.spawnParticles(ParticleTypes.ANGRY_VILLAGER, player.getX(), player.getY() + 1.0, player.getZ(),
				20, 0.5, 0.7, 0.5, 0.02);
		player.sendMessage(Text.literal("RAGE!").formatted(Formatting.DARK_RED, Formatting.BOLD), true);
	}

	public static void activateAdrenaline(ServerPlayerEntity player) {
		if (!BerserkerBladeItem.carriesBlade(player)) return;
		MeterState s = get(player);
		if (s.adrActive > 0) return;

		if (s.adrCooldown > 0) {
			player.sendMessage(Text.literal("Adrenaline on cooldown: " + secs(s.adrCooldown) + "s").formatted(Formatting.GRAY), true);
			return;
		}
		if (s.adr < Balance.ADR_FILL_TICKS) {
			player.sendMessage(Text.literal("Adrenaline isn't full yet").formatted(Formatting.GRAY), true);
			return;
		}

		s.adrActive = Balance.ADR_DURATION;
		s.dirty = true;

		ServerWorld world = player.getServerWorld();
		world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_WARDEN_HEARTBEAT,
				SoundCategory.PLAYERS, 1.5f, 1.0f);
		world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1.0, player.getZ(),
				25, 0.5, 0.8, 0.5, 0.2);
		player.sendMessage(Text.literal("ADRENALINE!").formatted(Formatting.GOLD, Formatting.BOLD), true);
	}

	public static void activateSlash(ServerPlayerEntity player) {
		if (!BerserkerBladeItem.holdsBlade(player)) return;
		MeterState s = get(player);

		if (s.slashCooldown > 0) {
			player.sendMessage(Text.literal("Wide Slash on cooldown: " + secs(s.slashCooldown) + "s").formatted(Formatting.GRAY), true);
			return;
		}
		s.slashCooldown = Balance.SLASH_COOLDOWN;
		s.dirty = true;

		ServerWorld world = player.getServerWorld();

		// Horizontal forward direction from yaw (works even when looking up or down).
		double yaw = Math.toRadians(player.getYaw());
		double fx = -Math.sin(yaw);
		double fz = Math.cos(yaw);
		double cosHalf = Math.cos(Math.toRadians(Balance.SLASH_HALF_ANGLE_DEG));

		// Damage scales with your current attack damage, so rage / adrenaline / potions all boost it.
		float damage = (float) player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE) * Balance.SLASH_DAMAGE_MULT;

		Box box = player.getBoundingBox().expand(Balance.SLASH_RANGE, 1.5, Balance.SLASH_RANGE);
		for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, box, e ->
				e != player
						&& e.isAlive()
						&& !(e instanceof ArmorStandEntity)
						&& !e.isTeammate(player)
						&& !(e instanceof TameableEntity pet && pet.isOwner(player)))) {
			double dx = target.getX() - player.getX();
			double dz = target.getZ() - player.getZ();
			double dist = Math.sqrt(dx * dx + dz * dz);
			if (dist > Balance.SLASH_RANGE + target.getWidth() / 2.0) continue;
			if (dist > 0.01 && (dx * fx + dz * fz) / dist < cosHalf) continue;
			target.damage(world.getDamageSources().playerAttack(player), damage);
		}

		// Visual: a fan of sweep particles across the arc.
		for (int k = -4; k <= 4; k++) {
			double a = Math.toRadians(Balance.SLASH_HALF_ANGLE_DEG) * (k / 4.0);
			double dirX = fx * Math.cos(a) - fz * Math.sin(a);
			double dirZ = fx * Math.sin(a) + fz * Math.cos(a);
			for (double r : new double[]{2.0, 3.5, 5.0}) {
				world.spawnParticles(ParticleTypes.SWEEP_ATTACK,
						player.getX() + dirX * r, player.getY() + 1.0, player.getZ() + dirZ * r,
						1, 0, 0, 0, 0);
			}
		}
		world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,
				SoundCategory.PLAYERS, 1.6f, 0.7f);
		player.swingHand(Hand.MAIN_HAND, true);
		player.sendMessage(Text.literal("WIDE SLASH!").formatted(Formatting.AQUA, Formatting.BOLD), true);
	}

	// ------------------------------------------------------------------

	private static void tick(ServerPlayerEntity player, int serverTick) {
		MeterState s = get(player);
		boolean carrying = BerserkerBladeItem.carriesBlade(player);
		ServerWorld world = player.getServerWorld();

		// ---- Rage timers ----
		if (s.rageActive > 0) {
			s.rageActive--;
			if (s.rageActive == 0) {
				s.rageCooldown = Balance.RAGE_COOLDOWN;
				s.dirty = true;
			} else if (serverTick % 10 == 0) {
				world.spawnParticles(ParticleTypes.ANGRY_VILLAGER, player.getX(), player.getY() + 1.2, player.getZ(),
						1, 0.4, 0.5, 0.4, 0.0);
			}
		} else if (s.rageCooldown > 0) {
			s.rageCooldown--;
			if (s.rageCooldown == 0) s.dirty = true;
		}

		// ---- Adrenaline timers ----
		if (s.adrActive > 0) {
			s.adrActive--;
			if (s.adrActive == 0) {
				s.adr = 0;
				s.adrCooldown = Balance.ADR_COOLDOWN;
				s.dirty = true;
			} else if (serverTick % 4 == 0) {
				world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1.0, player.getZ(),
						2, 0.4, 0.6, 0.4, 0.1);
			}
		} else if (s.adrCooldown > 0) {
			s.adrCooldown--;
			if (s.adrCooldown == 0) s.dirty = true;
		} else if (carrying && s.adr < Balance.ADR_FILL_TICKS) {
			s.adr++; // fills slowly on its own, then waits for the activation key
			if (s.adr >= Balance.ADR_FILL_TICKS) {
				s.adr = Balance.ADR_FILL_TICKS;
				s.dirty = true;
				world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_BEACON_POWER_SELECT,
						SoundCategory.PLAYERS, 1f, 1.4f);
				player.sendMessage(Text.literal("Adrenaline is ready!").formatted(Formatting.GOLD, Formatting.BOLD), true);
			}
		}

		// ---- Wide slash cooldown ----
		if (s.slashCooldown > 0) {
			s.slashCooldown--;
			if (s.slashCooldown == 0) s.dirty = true;
		}

		// ---- Attribute bonuses ----
		// Damage only applies while the blade is in hand. Adrenaline (x2) and rage (x1.4) do not
		// stack with each other - the higher one wins. Both stack normally with Strength potions.
		double damageBonus = 0;
		if (BerserkerBladeItem.holdsBlade(player)) {
			if (s.adrActive > 0) damageBonus = Balance.ADR_DAMAGE_BONUS;
			else if (s.rageActive > 0) damageBonus = Balance.RAGE_DAMAGE_BONUS;
		}
		applyModifier(player.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE),
				DAMAGE_ID, damageBonus, damageBonus > 0);
		applyModifier(player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED),
				SPEED_ID, Balance.ADR_SPEED_BONUS, s.adrActive > 0);

		// ---- Sync to client ----
		SyncPayload now = s.toPayload();
		if (s.dirty || (serverTick % 2 == 0 && !now.equals(s.lastSent))) {
			ServerPlayNetworking.send(player, now);
			s.lastSent = now;
			s.dirty = false;
		}
	}

	private static void applyModifier(EntityAttributeInstance inst, Identifier id, double value, boolean want) {
		if (inst == null) return;
		EntityAttributeModifier current = inst.getModifier(id);
		if (!want) {
			if (current != null) inst.removeModifier(id);
			return;
		}
		if (current != null && current.value() == value) return;
		if (current != null) inst.removeModifier(id);
		inst.addTemporaryModifier(new EntityAttributeModifier(id, value,
				EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
	}

	private static int secs(int ticks) {
		return (ticks + 19) / 20;
	}
}
