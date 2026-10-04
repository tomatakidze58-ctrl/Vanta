package dev.vanta.client.module;

import dev.vanta.client.VantaClient;
import dev.vanta.client.util.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import java.util.ArrayList;
import java.util.List;

public final class PvpModules {
    private PvpModules() {}

    static String roman(int n) {
        String[] r = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return n >= 1 && n <= 10 ? r[n - 1] : String.valueOf(n);
    }

    static int count(MinecraftClient mc, Item item) {
        PlayerInventory inv = mc.player.getInventory();
        int c = 0;
        for (int i = 0; i < inv.size(); i++) { ItemStack s = inv.getStack(i); if (s.isOf(item)) c += s.getCount(); }
        return c;
    }

    // ---------------------------------------------------------------- Keystrokes
    public static class Keystrokes extends Module {
        final Setting mouse = bool("Mouse Buttons", true), space = bool("Space Bar", true),
                      cps = bool("Show CPS", true), pressed = color("Pressed Color", 0xFFFFFFFF);
        public Keystrokes() { super("Keystrokes", Category.PVP, "WASD, space and mouse overlay", true, 6, 90); on(); }

        private void key(DrawContext ctx, MinecraftClient mc, String label, boolean down, int kx, int ky, int kw, int kh, String sub) {
            int bg = down ? Theme.withAlpha(pressed.color, 0xCC) : 0x80000000;
            int fg = down ? 0xFF000000 : 0xFFFFFFFF;
            ctx.fill(kx, ky, kx + kw, ky + kh, bg);
            if (sub == null) ctx.drawCenteredTextWithShadow(mc.textRenderer, label, kx + kw / 2, ky + (kh - 8) / 2, fg);
            else {
                ctx.drawCenteredTextWithShadow(mc.textRenderer, label, kx + kw / 2, ky + 3, fg);
                ctx.drawCenteredTextWithShadow(mc.textRenderer, sub, kx + kw / 2, ky + 12, fg);
            }
        }

        @Override public void onHud(DrawContext ctx, MinecraftClient mc) {
            var o = mc.options; int S = 22, G = 2, total = 3 * S + 2 * G;
            key(ctx, mc, "W", o.forwardKey.isPressed(), x + S + G, y, S, S, null);
            key(ctx, mc, "A", o.leftKey.isPressed(), x, y + S + G, S, S, null);
            key(ctx, mc, "S", o.backKey.isPressed(), x + S + G, y + S + G, S, S, null);
            key(ctx, mc, "D", o.rightKey.isPressed(), x + 2 * (S + G), y + S + G, S, S, null);
            int cy = y + 2 * (S + G);
            if (space.b) { key(ctx, mc, "SPACE", o.jumpKey.isPressed(), x, cy, total, 14, null); cy += 16; }
            if (mouse.b) {
                int hw = (total - G) / 2;
                key(ctx, mc, "LMB", o.attackKey.isPressed(), x, cy, hw, S, cps.b ? Clicks.left() + " cps" : null);
                key(ctx, mc, "RMB", o.useKey.isPressed(), x + hw + G, cy, hw, S, cps.b ? Clicks.right() + " cps" : null);
                cy += S + G;
            }
            w = total; h = cy - y;
        }
    }

    // ---------------------------------------------------------------- CPS
    public static class CpsCounter extends Module {
        final Setting both = bool("Show Right Click", true);
        public CpsCounter() { super("CPS Counter", Category.PVP, "Clicks per second", true, 6, 18); on(); }
        @Override public void onHud(DrawContext ctx, MinecraftClient mc) {
            Hud.one(ctx, mc, this, "CPS", both.b ? Clicks.left() + " | " + Clicks.right() : String.valueOf(Clicks.left()));
        }
    }

    // ---------------------------------------------------------------- Armor HUD
    public static class ArmorHud extends Module {
        final Setting horizontal = bool("Horizontal", false), text = bool("Durability Text", true),
                      percent = bool("Show Percent", false), hand = bool("Show Held Item", true);
        public ArmorHud() { super("Armor HUD", Category.PVP, "Armor pieces with durability", true, 6, 170); on(); }
        @Override public void onHud(DrawContext ctx, MinecraftClient mc) {
            if (mc.player == null) return;
            EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND};
            int cx = x, cy = y, maxW = 18;
            for (EquipmentSlot sl : slots) {
                if (sl == EquipmentSlot.MAINHAND && !hand.b) continue;
                ItemStack s = mc.player.getEquippedStack(sl);
                if (s.isEmpty()) continue;
                ctx.drawItem(s, cx, cy);
                ctx.drawStackOverlay(mc.textRenderer, s, cx, cy);
                int rowW = 18;
                if (text.b && !horizontal.b && s.isDamageable()) {
                    int left = s.getMaxDamage() - s.getDamage();
                    String t = percent.b ? Math.round(left * 100f / s.getMaxDamage()) + "%" : String.valueOf(left);
                    ctx.drawText(mc.textRenderer, t, cx + 20, cy + 4, Hud.gradient(left / (float) s.getMaxDamage()), true);
                    rowW = 20 + mc.textRenderer.getWidth(t);
                }
                maxW = Math.max(maxW, rowW);
                if (horizontal.b) cx += 18; else cy += 18;
            }
            w = horizontal.b ? Math.max(18, cx - x) : maxW; h = horizontal.b ? 18 : Math.max(18, cy - y);
        }
    }

    // ---------------------------------------------------------------- Potion Effects
    public static class PotionEffects extends Module {
        final Setting hideIcon = bool("Hide Hidden Effects", true), showTimer = bool("Show Timer", true), showLevel = bool("Show Level", true), vanillaIcon = bool("Vanilla Potion Icon", true);
        public PotionEffects() { super("Potion Effects", Category.PVP, "Active effects with timers", true, 6, 260); on(); }
        @Override public void onHud(DrawContext ctx, MinecraftClient mc) {
            if (mc.player == null) return;
            List<String[]> rows = new ArrayList<>(); List<Integer> cols = new ArrayList<>();
            for (StatusEffectInstance e : mc.player.getStatusEffects()) {
                if (hideIcon.b && !e.shouldShowIcon()) continue;
                String name = e.getEffectType().value().getName().getString() + (showLevel.b && e.getAmplifier() > 0 ? " " + roman(e.getAmplifier() + 1) : "");
                int secs = e.getDuration() / 20;
                String time = e.isInfinite() ? "**:**" : String.format("%d:%02d", secs / 60, secs % 60);
                rows.add(new String[]{name, showTimer.b ? time : ""});
                cols.add(e.getEffectType().value().isBeneficial() ? 0xFF55FF55 : 0xFFFF5555);
            }
            if (rows.isEmpty()) { w = 60; h = 10; return; }
            int w0 = 0;
            for (String[] r : rows) w0 = Math.max(w0, mc.textRenderer.getWidth(r[0]) + 10 + mc.textRenderer.getWidth(r[1]));
            Hud.bg(ctx, this, w0 + 6, rows.size() * 10 + 5);
            int yy = y + 3;
            for (int i = 0; i < rows.size(); i++) {
                ctx.drawText(mc.textRenderer, rows.get(i)[0], x + 3, yy, cols.get(i), Theme.shadow.b);
                String t = rows.get(i)[1];
                ctx.drawText(mc.textRenderer, t, x + w - 3 - mc.textRenderer.getWidth(t), yy, 0xFFFFFFFF, Theme.shadow.b);
                yy += 10;
            }
        }
    }

    // ---------------------------------------------------------------- Durability HUD
    public static class DurabilityHud extends Module {
        final Setting both = bool("Show Offhand", false);
        public DurabilityHud() { super("Durability HUD", Category.PVP, "Held item durability", true, 6, 240); }
        @Override public void onHud(DrawContext ctx, MinecraftClient mc) {
            if (mc.player == null) return;
            ItemStack[] stacks = both.b ? new ItemStack[]{mc.player.getMainHandStack(), mc.player.getOffHandStack()}
                                        : new ItemStack[]{mc.player.getMainHandStack()};
            int yy = y; int ww = 0;
            for (ItemStack s : stacks) {
                if (s.isEmpty() || !s.isDamageable()) continue;
                int left = s.getMaxDamage() - s.getDamage(); float p = left / (float) s.getMaxDamage();
                String t = left + "/" + s.getMaxDamage() + " (" + Math.round(p * 100) + "%)";
                ctx.drawItem(s, x, yy);
                ctx.drawText(mc.textRenderer, t, x + 20, yy + 4, Hud.gradient(p), true);
                ww = Math.max(ww, 20 + mc.textRenderer.getWidth(t));
                yy += 18;
            }
            w = Math.max(18, ww); h = Math.max(18, yy - y);
        }
    }

    // ---------------------------------------------------------------- Totem Counter
    public static class TotemCounter extends Module {
        final Setting hideZero = bool("Hide When Zero", false);
        public TotemCounter() { super("Totem Counter", Category.PVP, "Totems of Undying in inventory", true, 6, 300); }
        @Override public void onHud(DrawContext ctx, MinecraftClient mc) {
            if (mc.player == null) return;
            int c = count(mc, Items.TOTEM_OF_UNDYING);
            if (c == 0 && hideZero.b) return;
            String t = "x" + c;
            Hud.bg(ctx, this, 24 + mc.textRenderer.getWidth(t), 20);
            ctx.drawItem(new ItemStack(Items.TOTEM_OF_UNDYING), x + 2, y + 2);
            ctx.drawText(mc.textRenderer, t, x + 22, y + 6, c == 0 ? 0xFFFF5555 : 0xFFFFFFFF, true);
        }
    }

    // ---------------------------------------------------------------- Item Counter
    public static class ItemCounter extends Module {
        final Setting gapple = bool("Golden Apples", true), pearl = bool("Ender Pearls", true),
                      crystal = bool("End Crystals", true), arrow = bool("Arrows", false),
                      xp = bool("XP Bottles", false), firework = bool("Fireworks", false),
                      obsidian = bool("Obsidian", false), horizontal = bool("Horizontal", false);
        public ItemCounter() { super("Item Counter", Category.PVP, "Counts PvP items", true, 6, 322); }
        @Override public void onHud(DrawContext ctx, MinecraftClient mc) {
            if (mc.player == null) return;
            List<Item> items = new ArrayList<>();
            if (gapple.b) items.add(Items.GOLDEN_APPLE);
            if (pearl.b) items.add(Items.ENDER_PEARL);
            if (crystal.b) items.add(Items.END_CRYSTAL);
            if (arrow.b) items.add(Items.ARROW);
            if (xp.b) items.add(Items.EXPERIENCE_BOTTLE);
            if (firework.b) items.add(Items.FIREWORK_ROCKET);
            if (obsidian.b) items.add(Items.OBSIDIAN);
            int cx = x, cy = y, maxW = 18;
            for (Item it : items) {
                int c = count(mc, it); String t = String.valueOf(c);
                ctx.drawItem(new ItemStack(it), cx, cy);
                ctx.drawText(mc.textRenderer, t, cx + 19, cy + 4, c == 0 ? 0xFFFF5555 : 0xFFFFFFFF, true);
                int rw = 19 + mc.textRenderer.getWidth(t) + 4;
                maxW = Math.max(maxW, rw);
                if (horizontal.b) cx += rw + 4; else cy += 18;
            }
            w = horizontal.b ? Math.max(18, cx - x) : maxW; h = horizontal.b ? 18 : Math.max(18, cy - y);
        }
    }

    // ---------------------------------------------------------------- Attack Indicator (bar under crosshair)
    public static class AttackIndicator extends Module {
        final Setting width = num("Width", 30, 10, 80, 2), hideFull = bool("Hide When Full", true);
        public AttackIndicator() { super("Attack Indicator", Category.PVP, "Cooldown bar under the crosshair", true, 0, 0); noEdit(); }
        @Override public void onHud(DrawContext ctx, MinecraftClient mc) {
            if (mc.player == null || !mc.options.getPerspective().isFirstPerson()) return;
            float p = mc.player.getAttackCooldownProgress(0f);
            if (hideFull.b && p >= 1f) return;
            int cx = mc.getWindow().getScaledWidth() / 2, cy = mc.getWindow().getScaledHeight() / 2 + 14, bw = width.i();
            ctx.fill(cx - bw / 2 - 1, cy - 1, cx + bw / 2 + 1, cy + 4, 0xAA000000);
            ctx.fill(cx - bw / 2, cy, cx - bw / 2 + (int) (bw * p), cy + 3, Theme.accent());
        }
    }

    // ---------------------------------------------------------------- Attack Cooldown (numeric)
    public static class AttackCooldown extends Module {
        final Setting hideFull = bool("Hide When Ready", false);
        public AttackCooldown() { super("Attack Cooldown", Category.PVP, "Cooldown as a percentage", true, 6, 30); }
        @Override public void onHud(DrawContext ctx, MinecraftClient mc) {
            if (mc.player == null) return;
            float p = mc.player.getAttackCooldownProgress(0f);
            if (hideFull.b && p >= 1f) return;
            Hud.one(ctx, mc, this, "Cooldown", p >= 1f ? "READY" : Math.round(p * 100) + "%");
        }
    }

    // ---------------------------------------------------------------- Mace Cooldown
    public static class MaceCooldown extends Module {
        final Setting onlyMace = bool("Only When Holding Mace", true);
        public MaceCooldown() { super("Mace Cooldown", Category.PVP, "Mace readiness + smash bonus damage", true, 6, 42); }
        @Override public void onHud(DrawContext ctx, MinecraftClient mc) {
            if (mc.player == null) return;
            boolean holding = mc.player.getMainHandStack().isOf(Items.MACE);
            if (onlyMace.b && !holding) return;
            float p = mc.player.getAttackCooldownProgress(0f);
            double fall = mc.player.fallDistance, bonus = 0;
            if (fall > 1.5) bonus = fall <= 3 ? 4 * fall : fall <= 8 ? 12 + 2 * (fall - 3) : 22 + (fall - 8);
            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"Mace", p >= 1f ? "READY" : Math.round(p * 100) + "%"});
            rows.add(new String[]{"Fall", String.format("%.1f", fall)});
            rows.add(new String[]{"Bonus", bonus > 0 ? String.format("+%.1f", bonus) : "-"});
            Hud.rows(ctx, mc, this, rows);
        }
    }

    // ---------------------------------------------------------------- Crosshair Customizer
    public static class CrosshairCustomizer extends Module {
        final Setting style = mode("Style", 0, "Cross", "Dot", "Circle", "Square", "T Cross", "X Cross");
        final Setting size = num("Size", 5, 1, 24, 1), gap = num("Gap", 2, 0, 12, 1), thick = num("Thickness", 1, 1, 5, 1);
        final Setting col = color("Color", 0xFFFFFFFF);
        final Setting outline = bool("Outline", true), dynamic = bool("Dynamic Spread", false);
        final Setting centerDot = bool("Center Dot", false), dotSize = num("Dot Size", 1, 1, 4, 1);
        public CrosshairCustomizer() { super("Crosshair Customizer", Category.PVP, "Custom crosshair with advanced Vanta controls", false, 0, 0); on(); }

        private void arm(DrawContext ctx, int x1, int y1, int x2, int y2, int c, int g) { ctx.fill(x1 - g, y1 - g, x2 + g, y2 + g, c); }

        private void shapes(DrawContext ctx, int cx, int cy, int c, int g) {
            int s = size.i(), t = thick.i(), gp = gap.i(), t0 = t / 2, t1 = t - t0;
            if (dynamic.b) {
                var p = MinecraftClient.getInstance().player;
                if (p != null) {
                    if (p.getVelocity().horizontalLengthSquared() > 0.003) gp += 2;
                    if (!p.isOnGround()) gp += 2;
                }
            }
            switch (style.mode) {
                case 0 -> {
                    arm(ctx, cx - gp - s, cy - t0, cx - gp, cy + t1, c, g);
                    arm(ctx, cx + gp, cy - t0, cx + gp + s, cy + t1, c, g);
                    arm(ctx, cx - t0, cy - gp - s, cx + t1, cy - gp, c, g);
                    arm(ctx, cx - t0, cy + gp, cx + t1, cy + gp + s, c, g);
                }
                case 1 -> arm(ctx, cx - t, cy - t, cx + t, cy + t, c, g);
                case 2 -> {
                    int r = s + gp, steps = Math.max(24, r * 8);
                    for (int i = 0; i < steps; i++) {
                        double a = i * Math.PI * 2 / steps;
                        int px = cx + (int)Math.round(Math.cos(a) * r), py = cy + (int)Math.round(Math.sin(a) * r);
                        ctx.fill(px - g, py - g, px + t + g, py + t + g, c);
                    }
                }
                case 3 -> {
                    int r = s + gp;
                    arm(ctx, cx-r, cy-r, cx+r, cy-r+t, c, g); arm(ctx, cx-r, cy+r-t, cx+r, cy+r, c, g);
                    arm(ctx, cx-r, cy-r, cx-r+t, cy+r, c, g); arm(ctx, cx+r-t, cy-r, cx+r, cy+r, c, g);
                }
                case 4 -> {
                    arm(ctx, cx-gp-s, cy-t0, cx-gp, cy+t1, c, g); arm(ctx, cx+gp, cy-t0, cx+gp+s, cy+t1, c, g);
                    arm(ctx, cx-t0, cy+gp, cx+t1, cy+gp+s, c, g);
                }
                default -> {
                    for (int i = gp; i < gp+s; i++) {
                        int d = i; ctx.fill(cx-d-g, cy-d-g, cx-d+t+g, cy-d+t+g, c);
                        ctx.fill(cx+d-g, cy-d-g, cx+d+t+g, cy-d+t+g, c);
                        ctx.fill(cx-d-g, cy+d-g, cx-d+t+g, cy+d+t+g, c);
                        ctx.fill(cx+d-g, cy+d-g, cx+d+t+g, cy+d+t+g, c);
                    }
                }
            }
            if (centerDot.b) { int d = dotSize.i(); ctx.fill(cx-d, cy-d, cx+d+1, cy+d+1, c); }
        }

        public void drawCrosshair(DrawContext ctx) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (!mc.options.getPerspective().isFirstPerson()) return;
            int cx = mc.getWindow().getScaledWidth()/2, cy = mc.getWindow().getScaledHeight()/2, c = col.color;
            HitColor hc = VantaClient.mod(HitColor.class);
            if (hc != null && hc.enabled && hc.active()) c = hc.color.color;
            if (outline.b) shapes(ctx, cx, cy, 0xFF000000, 1);
            shapes(ctx, cx, cy, c, 0);
        }
    }

    // ---------------------------------------------------------------- Hit Color (hit marker + crosshair flash)
    public static class HitColor extends Module {
        final Setting color = color("Hit Color", 0xFFFF5555), duration = num("Duration (ms)", 250, 50, 1000, 10),
                      marker = bool("Hit Marker", true);
        public HitColor() { super("Hit Color", Category.PVP, "Crosshair flashes when you land a hit", true, 0, 0); noEdit(); }
        boolean active() { return System.currentTimeMillis() - HitTracker.lastHit < duration.n; }
        @Override public void onHud(DrawContext ctx, MinecraftClient mc) {
            if (!marker.b || !active()) return;
            int cx = mc.getWindow().getScaledWidth() / 2, cy = mc.getWindow().getScaledHeight() / 2;
            for (int i = 4; i <= 8; i++) {
                for (int sx = -1; sx <= 1; sx += 2) for (int sy = -1; sy <= 1; sy += 2)
                    ctx.fill(cx + sx * i, cy + sy * i, cx + sx * i + 1, cy + sy * i + 1, color.color);
            }
        }
    }

    // ---------------------------------------------------------------- Hit Sound
    public static class HitSound extends Module {
        final Setting sound = mode("Sound", 0, "Orb", "Arrow Ding", "Anvil", "Level Up", "Chime", "Crit");
        final Setting pitch = num("Pitch", 1.2, 0.5, 2.0, 0.1), volume = num("Volume", 0.8, 0.1, 1.0, 0.1);
        public HitSound() { super("Hit Sound", Category.PVP, "Plays a sound when you hit", false, 0, 0); }
        @Override public void onTick(MinecraftClient mc) {
            if (!HitTracker.hitThisTick) return;
            SoundEvent e = switch (sound.mode) {
                case 1 -> SoundEvents.ENTITY_ARROW_HIT_PLAYER;
                case 2 -> SoundEvents.BLOCK_ANVIL_LAND;
                case 3 -> SoundEvents.ENTITY_PLAYER_LEVELUP;
                case 4 -> SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME;
                case 5 -> SoundEvents.ENTITY_PLAYER_ATTACK_CRIT;
                default -> SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP;
            };
            HitTracker.play(e, pitch.f(), volume.f());
        }
    }

    // ---------------------------------------------------------------- Low Fire / Low Shield (applied by mixins)
    public static class LowFire extends Module {
        public final Setting height = num("Lower By", 0.3, 0.0, 0.6, 0.05);
        public LowFire() { super("Low Fire", Category.PVP, "Lowers the first-person fire overlay", false, 0, 0); on(); }
    }

    public static class LowShield extends Module {
        public final Setting amount = num("Lower By", 0.25, 0.0, 0.6, 0.05);
        public LowShield() { super("Low Shield", Category.PVP, "Lowers the first-person shield", false, 0, 0); on(); }
    }

    // ---------------------------------------------------------------- Auto Sprint
    public static class AutoSprint extends Module {
        final Setting hunger = bool("Respect Hunger", true);
        public AutoSprint() { super("Auto Sprint", Category.PVP, "Sprints automatically while moving forward", false, 0, 0); on(); }
        @Override public void onTick(MinecraftClient mc) {
            var p = mc.player;
            if (p == null || mc.currentScreen != null) return;
            if (mc.options.forwardKey.isPressed() && !p.isSneaking() && !p.horizontalCollision && !p.isUsingItem()
                    && (!hunger.b || p.getHungerManager().getFoodLevel() > 6)) p.setSprinting(true);
        }
    }
}
