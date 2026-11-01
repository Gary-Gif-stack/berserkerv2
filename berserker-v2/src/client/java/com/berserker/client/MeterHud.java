package com.berserker.client;

import com.berserker.Balance;
import com.berserker.BerserkerBladeItem;
import com.berserker.SyncPayload;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderTickCounter;

public final class MeterHud {
	private MeterHud() {}

	private static final int W = 110;
	private static final int H = 6;

	public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.player == null || mc.options.hudHidden || !BerserkerBladeItem.carriesBlade(mc.player)) return;

		SyncPayload m = ClientMeters.current;
		TextRenderer tr = mc.textRenderer;
		int x = ctx.getScaledWindowWidth() - W - 10;
		int y = ctx.getScaledWindowHeight() / 2 + 6;

		// ---- Rage ----
		String rk = key(BerserkerClient.RAGE_KEY);
		if (m.rageActive() > 0) {
			bar(ctx, tr, x, y, m.rageActive() / (float) Balance.RAGE_DURATION, 0xFFFF6A00,
					"RAGING " + secs(m.rageActive()) + "s");
		} else if (m.rageCooldown() > 0) {
			bar(ctx, tr, x, y, 1f - m.rageCooldown() / (float) Balance.RAGE_COOLDOWN, 0xFF666666,
					"Rage cooldown " + secs(m.rageCooldown()) + "s");
		} else if (m.rage() >= Balance.RAGE_MAX) {
			bar(ctx, tr, x, y, 1f, 0xFFFF2020, "RAGE READY - press " + rk);
		} else {
			bar(ctx, tr, x, y, m.rage() / (float) Balance.RAGE_MAX, 0xFFB01818, "Rage [" + rk + "]");
		}

		// ---- Adrenaline ----
		int y2 = y + 24;
		String ak = key(BerserkerClient.ADRENALINE_KEY);
		if (m.adrActive() > 0) {
			bar(ctx, tr, x, y2, m.adrActive() / (float) Balance.ADR_DURATION, 0xFFFFE04A,
					"ADRENALINE " + secs(m.adrActive()) + "s");
		} else if (m.adrCooldown() > 0) {
			bar(ctx, tr, x, y2, 1f - m.adrCooldown() / (float) Balance.ADR_COOLDOWN, 0xFF666666,
					"Adrenaline cooldown " + secs(m.adrCooldown()) + "s");
		} else if (m.adr() >= Balance.ADR_FILL_TICKS) {
			bar(ctx, tr, x, y2, 1f, 0xFFFFD21A, "ADRENALINE READY - press " + ak);
		} else {
			bar(ctx, tr, x, y2, m.adr() / (float) Balance.ADR_FILL_TICKS, 0xFFE0A010, "Adrenaline [" + ak + "]");
		}

		// ---- Wide Slash ----
		int y3 = y + 48;
		String sk = key(BerserkerClient.SLASH_KEY);
		if (m.slashCooldown() > 0) {
			bar(ctx, tr, x, y3, 1f - m.slashCooldown() / (float) Balance.SLASH_COOLDOWN, 0xFF666666,
					"Wide Slash cooldown " + secs(m.slashCooldown()) + "s");
		} else {
			bar(ctx, tr, x, y3, 1f, 0xFF4FC3FF, "WIDE SLASH READY - press " + sk);
		}
	}

	private static String key(KeyBinding k) {
		return k.getBoundKeyLocalizedText().getString();
	}

	private static void bar(DrawContext ctx, TextRenderer tr, int x, int y, float frac, int color, String label) {
		frac = Math.max(0f, Math.min(1f, frac));
		ctx.drawTextWithShadow(tr, label, x, y - 10, 0xFFFFFFFF);
		ctx.fill(x - 1, y - 1, x + W + 1, y + H + 1, 0xFF000000);
		ctx.fill(x, y, x + W, y + H, 0xFF3A3A3A);
		ctx.fill(x, y, x + (int) (W * frac), y + H, color);
	}

	private static int secs(int ticks) {
		return (ticks + 19) / 20;
	}
}
