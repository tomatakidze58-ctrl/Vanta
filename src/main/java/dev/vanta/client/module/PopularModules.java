package dev.vanta.client.module;

import dev.vanta.client.VantaClient;
import dev.vanta.client.util.HitTracker;
import dev.vanta.client.util.Hud;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Extra legit-client modules inspired by common HUD/QoL features across major Minecraft clients.
 * Everything here is local/client-side and intentionally avoids cheat functionality.
 */
public final class PopularModules {
    private PopularModules() {}

    private static int count(MinecraftClient mc, Item item) {
        if (mc.player == null) return 0;
        PlayerInventory inv = mc.player.getInventory();
        int c = 0;
        for (int i = 0; i < inv.size(); i++) {
            ItemStack s = inv.getStack(i);
            if (s.isOf(item)) c += s.getCount();
        }
        return c;
    }

    private static int countAny(MinecraftClient mc, Item... items) {
        int c = 0;
        for (Item item : items) c += count(mc, item);
        return c;
    }

    // --------------------------------------------------------------- HUD / INFO

    public static class HealthDisplay extends Module {
        final Setting style = mode("Style", 0, "Hearts", "HP", "Percent");
        public HealthDisplay() { super("Health Display", Category.INFO, "Compact health HUD", true, 170, 6); }
        @Override public void onHud(DrawContext ctx, MinecraftClient mc) {
            if (mc.player == null) return;
            float hp = mc.player.getHealth(), max = mc.player.getMaxHealth();
            String v = switch (style.mode) {
                case 1 -> String.format("%.1f/%.1f HP", hp, max);
                case 2 -> Math.round((hp / Math.max(1f, max)) * 100f) + "%";
                default -> String.format("%.1f ❤", hp / 2f);
            };
            Hud.one(ctx, mc, this, "Health", v);
        }
    }

    public static class ArmorCondition extends Module {
        public ArmorCondition() { super("Armor Condition", Category.INFO, "Average durability of equipped armor", true, 170, 18); }
        @Override public void onHud(DrawContext ctx, MinecraftClient mc) {
            if (mc.player == null) return;
            EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
            int total = 0, used = 0;
            for (EquipmentSlot slot : slots) {
                ItemStack s = mc.player.getEquippedStack(slot);
                if (!s.isEmpty() && s.isDamageable()) {
                    total += Math.max(0, s.getMaxDamage() - s.getDamage()) * 100 / Math.max(1, s.getMaxDamage());
                    used++;
                }
            }
            Hud.one(ctx, mc, this, "Armor", used == 0 ? "None" : (total / used) + "%");
        }
    }

    public static class ExperienceDisplay extends Module {
        final Setting progress = bool("Show Progress", true);
        public ExperienceDisplay() { super("Experience", Category.INFO, "XP level and progress", true, 170, 30); }
        @Override public void onHud(DrawContext ctx, MinecraftClient mc) {
            if (mc.player == null) return;
            String v = "Lv. " + mc.player.experienceLevel;
            if (progress.b) v += "  " + Math.round(mc.player.experienceProgress * 100f) + "%";
            Hud.one(ctx, mc, this, "XP", v);
        }
    }

    public static class DayCounter extends Module {
        public DayCounter() { super("Day Counter", Category.INFO, "Minecraft world day", true, 170, 42); }
        @Override public void onHud(DrawContext ctx, MinecraftClient mc) {
            if (mc.world == null) return;
            long day = Math.max(0L, mc.world.getTimeOfDay() / 24000L) + 1L;
            Hud.one(ctx, mc, this, "Day", Long.toString(day));
        }
    }

    public static class DimensionDisplay extends Module {
        public DimensionDisplay() { super("Dimension", Category.INFO, "Current dimension", true, 170, 54); }
        @Override public void onHud(DrawContext ctx, MinecraftClient mc) {
            if (mc.world == null) return;
            String raw = mc.world.getRegistryKey().getValue().getPath();
            String label = switch (raw) {
                case "the_nether" -> "Nether";
                case "the_end" -> "The End";
                default -> "Overworld";
            };
            Hud.one(ctx, mc, this, "Dimension", label);
        }
    }

    public static class ChunkPosition extends Module {
        final Setting region = bool("Show Region", false);
        public ChunkPosition() { super("Chunk Position", Category.INFO, "Chunk and optional region coordinates", true, 170, 66); }
        @Override public void onHud(DrawContext ctx, MinecraftClient mc) {
            if (mc.player == null) return;
            int cx = mc.player.getBlockX() >> 4, cz = mc.player.getBlockZ() >> 4;
            String v = cx + ", " + cz;
            if (region.b) v += "  R " + Math.floorDiv(cx, 32) + "," + Math.floorDiv(cz, 32);
            Hud.one(ctx, mc, this, "Chunk", v);
        }
    }

    public static class TargetInfo extends Module {
        final Setting distance = bool("Distance", true);
        public TargetInfo() { super("Target Info", Category.INFO, "Name, health and distance of looked-at entity", true, 170, 78); }
        @Override public void onHud(DrawContext ctx, MinecraftClient mc) {
            if (!(mc.targetedEntity instanceof LivingEntity e) || mc.player == null) return;
            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"Target", e.getName().getString()});
            rows.add(new String[]{"Health", String.format("%.1f/%.1f", e.getHealth(), e.getMaxHealth())});
            if (distance.b) rows.add(new String[]{"Distance", String.format("%.2fm", mc.player.distanceTo(e))});
            Hud.rows(ctx, mc, this, rows);
        }
    }

    public static class HeldItemInfo extends Module {
        final Setting durability = bool("Durability", true), count = bool("Count", true);
        public HeldItemInfo() { super("Held Item Info", Category.INFO, "Selected item name, count and durability", true, 170, 116); }
        @Override public void onHud(DrawContext ctx, MinecraftClient mc) {
            if (mc.player == null) return;
            ItemStack s = mc.player.getMainHandStack();
            if (s.isEmpty()) return;
            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"Item", s.getName().getString()});
            if (count.b && s.getMaxCount() > 1) rows.add(new String[]{"Count", Integer.toString(s.getCount())});
            if (durability.b && s.isDamageable()) {
                int left = Math.max(0, s.getMaxDamage() - s.getDamage());
                rows.add(new String[]{"Durability", left + "/" + s.getMaxDamage()});
            }
            Hud.rows(ctx, mc, this, rows);
        }
    }

    public static class FoodPreview extends Module {
        final Setting saturation = bool("Show Saturation", true);
        public FoodPreview() { super("Food Preview", Category.INFO, "Held-food nutrition and saturation preview", true, 170, 150); }
        @Override public void onHud(DrawContext ctx, MinecraftClient mc) {
            if (mc.player == null) return;
            ItemStack s = mc.player.getMainHandStack();
            FoodComponent food = s.get(DataComponentTypes.FOOD);
            if (food == null) return;
            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"Food", "+" + food.nutrition()});
            if (saturation.b) rows.add(new String[]{"Saturation", "+" + String.format("%.1f", food.saturation())});
            Hud.rows(ctx, mc, this, rows);
        }
    }

    public static class GappleCounter extends Module {
        public GappleCounter() { super("Golden Apple Counter", Category.INFO, "Golden + enchanted golden apples", true, 170, 174); }
        @Override public void onHud(DrawContext ctx, MinecraftClient mc) { Hud.one(ctx, mc, this, "Gapples", Integer.toString(countAny(mc, Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE))); }
    }

    public static class PearlCounter extends Module {
        public PearlCounter() { super("Pearl Counter", Category.INFO, "Ender pearls in inventory", true, 170, 186); }
        @Override public void onHud(DrawContext ctx, MinecraftClient mc) { Hud.one(ctx, mc, this, "Pearls", Integer.toString(count(mc, Items.ENDER_PEARL))); }
    }

    public static class ArrowCounter extends Module {
        public ArrowCounter() { super("Arrow Counter", Category.INFO, "Arrows in inventory", true, 170, 198); }
        @Override public void onHud(DrawContext ctx, MinecraftClient mc) { Hud.one(ctx, mc, this, "Arrows", Integer.toString(countAny(mc, Items.ARROW, Items.SPECTRAL_ARROW, Items.TIPPED_ARROW))); }
    }

    public static class CrystalCounter extends Module {
        public CrystalCounter() { super("Crystal Counter", Category.INFO, "End crystals in inventory", true, 170, 210); }
        @Override public void onHud(DrawContext ctx, MinecraftClient mc) { Hud.one(ctx, mc, this, "Crystals", Integer.toString(count(mc, Items.END_CRYSTAL))); }
    }

    public static class XpBottleCounter extends Module {
        public XpBottleCounter() { super("XP Bottle Counter", Category.INFO, "Experience bottles in inventory", true, 170, 222); }
        @Override public void onHud(DrawContext ctx, MinecraftClient mc) { Hud.one(ctx, mc, this, "XP Bottles", Integer.toString(count(mc, Items.EXPERIENCE_BOTTLE))); }
    }

    public static class ObsidianCounter extends Module {
        public ObsidianCounter() { super("Obsidian Counter", Category.INFO, "Obsidian in inventory", true, 170, 234); }
        @Override public void onHud(DrawContext ctx, MinecraftClient mc) { Hud.one(ctx, mc, this, "Obsidian", Integer.toString(count(mc, Items.OBSIDIAN))); }
    }

    public static class PotionCounter extends Module {
        public PotionCounter() { super("Potion Counter", Category.INFO, "Drinkable, splash and lingering potions", true, 170, 246); }
        @Override public void onHud(DrawContext ctx, MinecraftClient mc) { Hud.one(ctx, mc, this, "Potions", Integer.toString(countAny(mc, Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION))); }
    }

    public static class ShieldCounter extends Module {
        public ShieldCounter() { super("Shield Counter", Category.INFO, "Shields in inventory", true, 170, 258); }
        @Override public void onHud(DrawContext ctx, MinecraftClient mc) { Hud.one(ctx, mc, this, "Shields", Integer.toString(count(mc, Items.SHIELD))); }
    }

    public static class Stopwatch extends Module {
        long start;
        public Stopwatch() { super("Stopwatch", Category.INFO, "Simple session stopwatch", true, 300, 6); }
        @Override public void onEnable() { start = System.currentTimeMillis(); }
        @Override public void onHud(DrawContext ctx, MinecraftClient mc) {
            long ms = Math.max(0L, System.currentTimeMillis() - start), s = ms / 1000L;
            Hud.one(ctx, mc, this, "Stopwatch", String.format("%02d:%02d.%01d", s / 60, s % 60, (ms % 1000) / 100));
        }
    }

    public static class DateDisplay extends Module {
        final Setting format = mode("Format", 0, "YYYY-MM-DD", "DD/MM/YYYY", "MM/DD/YYYY");
        public DateDisplay() { super("Date", Category.INFO, "Real-world date", true, 300, 18); }
        @Override public void onHud(DrawContext ctx, MinecraftClient mc) {
            DateTimeFormatter f = switch (format.mode) {
                case 1 -> DateTimeFormatter.ofPattern("dd/MM/yyyy");
                case 2 -> DateTimeFormatter.ofPattern("MM/dd/yyyy");
                default -> DateTimeFormatter.ISO_LOCAL_DATE;
            };
            Hud.one(ctx, mc, this, null, LocalDate.now().format(f));
        }
    }

    public static class PingGraph extends Module {
        final Setting samples = num("Samples", 30, 10, 60, 5);
        private final int[] history = new int[60];
        private int index, tick;
        public PingGraph() { super("Ping Graph", Category.INFO, "Small live latency graph", true, 300, 30); }
        @Override public void onTick(MinecraftClient mc) {
            if (++tick % 10 != 0 || mc.player == null || mc.getNetworkHandler() == null) return;
            PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            history[index++ % history.length] = e == null ? 0 : Math.max(0, e.getLatency());
        }
        @Override public void onHud(DrawContext ctx, MinecraftClient mc) {
            int n = samples.i(), width = n + 8, height = 34;
            Hud.bg(ctx, this, width, height);
            int max = 1;
            for (int i = 0; i < n; i++) max = Math.max(max, history[Math.floorMod(index - 1 - i, history.length)]);
            for (int i = 0; i < n; i++) {
                int v = history[Math.floorMod(index - n + i, history.length)];
                int bar = Math.min(24, Math.round(v / (float) max * 24f));
                int color = v < 80 ? 0xFF55FF88 : v < 160 ? 0xFFFFCC55 : 0xFFFF6666;
                ctx.fill(x + 4 + i, y + 28 - bar, x + 5 + i, y + 28, color);
            }
            ctx.drawText(mc.textRenderer, "Ping", x + 4, y + 3, hudAccent(), hudShadow());
        }
    }

    // --------------------------------------------------------------- PvP / combat HUD (display-only)

    public static class ComboCounter extends Module {
        final Setting resetMs = num("Reset After (ms)", 2200, 500, 5000, 100);
        int combo;
        long last;
        public ComboCounter() { super("Combo Counter", Category.PVP, "Counts consecutive landed hits", true, 300, 66); }
        @Override public void onTick(MinecraftClient mc) {
            long now = System.currentTimeMillis();
            if (HitTracker.hitThisTick) { combo++; last = now; }
            if (combo > 0 && now - last > resetMs.n) combo = 0;
        }
        @Override public void onHud(DrawContext ctx, MinecraftClient mc) { Hud.one(ctx, mc, this, "Combo", combo + "x"); }
    }

    public static class HitDistance extends Module {
        final Setting decimals = num("Decimals", 2, 1, 3, 1);
        double distance;
        long last;
        public HitDistance() { super("Hit Distance", Category.PVP, "Displays distance of your last landed hit", true, 300, 78); }
        @Override public void onTick(MinecraftClient mc) {
            if (HitTracker.hitThisTick && mc.player != null && mc.targetedEntity != null) {
                distance = mc.player.distanceTo(mc.targetedEntity);
                last = System.currentTimeMillis();
            }
        }
        @Override public void onHud(DrawContext ctx, MinecraftClient mc) {
            if (last == 0) return;
            Hud.one(ctx, mc, this, "Hit", String.format("%." + decimals.i() + "fm", distance));
        }
    }

    // --------------------------------------------------------------- Visual controls using vanilla options

    public static class FovChanger extends Module {
        public final Setting fov = num("FOV", 90, 30, 110, 1);
        private int old;
        private boolean captured;
        public FovChanger() { super("FOV Changer", Category.VISUAL, "Locks a custom normal FOV", false, 0, 0); }
        @Override public void onEnable() { var o = MinecraftClient.getInstance().options.getFov(); old = o.getValue(); captured = true; o.setValue(fov.i()); }
        @Override public void onTick(MinecraftClient mc) { if (mc.options.getFov().getValue() != fov.i()) mc.options.getFov().setValue(fov.i()); }
        @Override public void onDisable() { if (captured) MinecraftClient.getInstance().options.getFov().setValue(old); captured = false; }
    }

    public static class ViewBobbing extends Module {
        final Setting enabledBob = bool("View Bobbing", false);
        private boolean old;
        private boolean captured;
        public ViewBobbing() { super("View Bobbing", Category.VISUAL, "Quick control for vanilla view bobbing", false, 0, 0); }
        @Override public void onEnable() { var o = MinecraftClient.getInstance().options.getBobView(); old = o.getValue(); captured = true; o.setValue(enabledBob.b); }
        @Override public void onTick(MinecraftClient mc) { if (mc.options.getBobView().getValue() != enabledBob.b) mc.options.getBobView().setValue(enabledBob.b); }
        @Override public void onDisable() { if (captured) MinecraftClient.getInstance().options.getBobView().setValue(old); captured = false; }
    }

    public static class DamageTilt extends Module {
        final Setting strength = num("Strength", 0.0, 0.0, 1.0, 0.05);
        private double old;
        private boolean captured;
        public DamageTilt() { super("Damage Tilt", Category.VISUAL, "Controls the vanilla hurt-camera tilt", false, 0, 0); }
        @Override public void onEnable() { var o = MinecraftClient.getInstance().options.getDamageTiltStrength(); old = o.getValue(); captured = true; o.setValue(strength.n); }
        @Override public void onTick(MinecraftClient mc) { if (Math.abs(mc.options.getDamageTiltStrength().getValue() - strength.n) > 0.001) mc.options.getDamageTiltStrength().setValue(strength.n); }
        @Override public void onDisable() { if (captured) MinecraftClient.getInstance().options.getDamageTiltStrength().setValue(old); captured = false; }
    }

    public static class DynamicFovControl extends Module {
        final Setting strength = num("FOV Effect", 0.0, 0.0, 1.0, 0.05);
        private double old;
        private boolean captured;
        public DynamicFovControl() { super("Dynamic FOV", Category.VISUAL, "Controls sprint/speed FOV effects", false, 0, 0); }
        @Override public void onEnable() { var o = MinecraftClient.getInstance().options.getFovEffectScale(); old = o.getValue(); captured = true; o.setValue(strength.n); }
        @Override public void onTick(MinecraftClient mc) { if (Math.abs(mc.options.getFovEffectScale().getValue() - strength.n) > 0.001) mc.options.getFovEffectScale().setValue(strength.n); }
        @Override public void onDisable() { if (captured) MinecraftClient.getInstance().options.getFovEffectScale().setValue(old); captured = false; }
    }

    public static class DistortionControl extends Module {
        final Setting strength = num("Portal/Nausea Strength", 0.0, 0.0, 1.0, 0.05);
        private double old;
        private boolean captured;
        public DistortionControl() { super("Distortion Control", Category.VISUAL, "Controls portal/nausea screen distortion", false, 0, 0); }
        @Override public void onEnable() { var o = MinecraftClient.getInstance().options.getDistortionEffectScale(); old = o.getValue(); captured = true; o.setValue(strength.n); }
        @Override public void onTick(MinecraftClient mc) { if (Math.abs(mc.options.getDistortionEffectScale().getValue() - strength.n) > 0.001) mc.options.getDistortionEffectScale().setValue(strength.n); }
        @Override public void onDisable() { if (captured) MinecraftClient.getInstance().options.getDistortionEffectScale().setValue(old); captured = false; }
    }

    public static class GlintStrength extends Module {
        final Setting strength = num("Glint Strength", 0.5, 0.0, 1.0, 0.05);
        private double old;
        private boolean captured;
        public GlintStrength() { super("Enchant Glint", Category.VISUAL, "Controls vanilla enchant-glint intensity", false, 0, 0); }
        @Override public void onEnable() { var o = MinecraftClient.getInstance().options.getGlintStrength(); old = o.getValue(); captured = true; o.setValue(strength.n); }
        @Override public void onTick(MinecraftClient mc) { if (Math.abs(mc.options.getGlintStrength().getValue() - strength.n) > 0.001) mc.options.getGlintStrength().setValue(strength.n); }
        @Override public void onDisable() { if (captured) MinecraftClient.getInstance().options.getGlintStrength().setValue(old); captured = false; }
    }

    public static class EntityShadows extends Module {
        final Setting shadows = bool("Entity Shadows", false);
        private boolean old;
        private boolean captured;
        public EntityShadows() { super("Entity Shadows", Category.VISUAL, "Toggles vanilla entity shadows", false, 0, 0); }
        @Override public void onEnable() { var o = MinecraftClient.getInstance().options.getEntityShadows(); old = o.getValue(); captured = true; o.setValue(shadows.b); }
        @Override public void onTick(MinecraftClient mc) { if (mc.options.getEntityShadows().getValue() != shadows.b) mc.options.getEntityShadows().setValue(shadows.b); }
        @Override public void onDisable() { if (captured) MinecraftClient.getInstance().options.getEntityShadows().setValue(old); captured = false; }
    }

    public static class ChatAppearance extends Module {
        final Setting opacity = num("Opacity", 90, 10, 100, 5);
        final Setting scale = num("Scale", 100, 50, 100, 5);
        private double oldOpacity, oldScale;
        private boolean captured;
        public ChatAppearance() { super("Chat Appearance", Category.VISUAL, "Chat opacity and scale", false, 0, 0); }
        @Override public void onEnable() {
            var mc = MinecraftClient.getInstance();
            oldOpacity = mc.options.getChatOpacity().getValue(); oldScale = mc.options.getChatScale().getValue(); captured = true;
            apply(mc);
        }
        private void apply(MinecraftClient mc) {
            mc.options.getChatOpacity().setValue(opacity.n / 100.0);
            mc.options.getChatScale().setValue(scale.n / 100.0);
        }
        @Override public void onTick(MinecraftClient mc) { apply(mc); }
        @Override public void onDisable() {
            if (!captured) return;
            var mc = MinecraftClient.getInstance(); mc.options.getChatOpacity().setValue(oldOpacity); mc.options.getChatScale().setValue(oldScale); captured = false;
        }
    }

    public static class GuiScaleControl extends Module {
        final Setting scale = num("GUI Scale", 3, 0, 8, 1);
        private int old;
        private boolean captured;
        public GuiScaleControl() { super("GUI Scale", Category.VISUAL, "Quick vanilla GUI-scale control", false, 0, 0); }
        @Override public void onEnable() { var o = MinecraftClient.getInstance().options.getGuiScale(); old = o.getValue(); captured = true; o.setValue(scale.i()); }
        @Override public void onTick(MinecraftClient mc) { if (mc.options.getGuiScale().getValue() != scale.i()) mc.options.getGuiScale().setValue(scale.i()); }
        @Override public void onDisable() { if (captured) MinecraftClient.getInstance().options.getGuiScale().setValue(old); captured = false; }
    }

    // --------------------------------------------------------------- Performance controls

    public static class ViewDistanceCap extends Module {
        final Setting chunks = num("Chunks", 12, 2, 32, 1);
        private int old;
        private boolean captured;
        public ViewDistanceCap() { super("View Distance Cap", Category.PERFORMANCE, "Locks render distance to a chosen value", false, 0, 0); }
        @Override public void onEnable() { var o = MinecraftClient.getInstance().options.getViewDistance(); old = o.getValue(); captured = true; o.setValue(chunks.i()); }
        @Override public void onTick(MinecraftClient mc) { if (mc.options.getViewDistance().getValue() != chunks.i()) mc.options.getViewDistance().setValue(chunks.i()); }
        @Override public void onDisable() { if (captured) MinecraftClient.getInstance().options.getViewDistance().setValue(old); captured = false; }
    }

    public static class EntityDistance extends Module {
        final Setting percent = num("Entity Distance %", 75, 50, 500, 25);
        private double old;
        private boolean captured;
        public EntityDistance() { super("Entity Distance", Category.PERFORMANCE, "Controls vanilla entity render distance", false, 0, 0); }
        @Override public void onEnable() { var o = MinecraftClient.getInstance().options.getEntityDistanceScaling(); old = o.getValue(); captured = true; o.setValue(percent.n / 100.0); }
        @Override public void onTick(MinecraftClient mc) { double v = percent.n / 100.0; if (Math.abs(mc.options.getEntityDistanceScaling().getValue() - v) > 0.001) mc.options.getEntityDistanceScaling().setValue(v); }
        @Override public void onDisable() { if (captured) MinecraftClient.getInstance().options.getEntityDistanceScaling().setValue(old); captured = false; }
    }

    // --------------------------------------------------------------- Utility / accessibility

    public static class ToggleSneakMode extends Module {
        private boolean old;
        private boolean captured;
        public ToggleSneakMode() { super("Toggle Sneak", Category.UTILITY, "Enables vanilla toggle-sneak mode", false, 0, 0); }
        @Override public void onEnable() { var o = MinecraftClient.getInstance().options.getSneakToggled(); old = o.getValue(); captured = true; o.setValue(true); }
        @Override public void onTick(MinecraftClient mc) { if (!mc.options.getSneakToggled().getValue()) mc.options.getSneakToggled().setValue(true); }
        @Override public void onDisable() { if (captured) MinecraftClient.getInstance().options.getSneakToggled().setValue(old); captured = false; }
    }

    public static class ToggleSprintMode extends Module {
        private boolean old;
        private boolean captured;
        public ToggleSprintMode() { super("Toggle Sprint", Category.UTILITY, "Enables vanilla toggle-sprint mode", false, 0, 0); }
        @Override public void onEnable() { var o = MinecraftClient.getInstance().options.getSprintToggled(); old = o.getValue(); captured = true; o.setValue(true); }
        @Override public void onTick(MinecraftClient mc) { if (!mc.options.getSprintToggled().getValue()) mc.options.getSprintToggled().setValue(true); }
        @Override public void onDisable() { if (captured) MinecraftClient.getInstance().options.getSprintToggled().setValue(old); captured = false; }
    }
}
