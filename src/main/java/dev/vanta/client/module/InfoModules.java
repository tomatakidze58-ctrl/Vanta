package dev.vanta.client.module;

import dev.vanta.client.VantaClient;
import dev.vanta.client.util.Hud;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class InfoModules {
    private InfoModules() {}

    public static class FpsCounter extends Module {
        final Setting label = bool("Label", true);
        public FpsCounter() { super("FPS Counter", Category.INFO, "Frames per second", true, 6, 6); on(); }
        @Override public void onHud(DrawContext ctx, MinecraftClient mc) {
            int f = mc.getCurrentFps();
            Hud.one(ctx, mc, this, label.b ? "FPS" : null, String.valueOf(f));
        }
    }

    public static class PingDisplay extends Module {
        public PingDisplay() { super("Ping Display", Category.INFO, "Latency to the server", true, 6, 18 + 12); on(); }
        @Override public void onHud(DrawContext ctx, MinecraftClient mc) {
            if (mc.player == null || mc.getNetworkHandler() == null) return;
            PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            Hud.one(ctx, mc, this, "Ping", e == null || mc.isInSingleplayer() ? "-" : e.getLatency() + "ms");
        }
    }

    public static class Coordinates extends Module {
        final Setting decimals = num("Decimals", 1, 0, 3, 1), nether = bool("Nether Conversion", true);
        public Coordinates() { super("Coordinates", Category.INFO, "XYZ position", true, 6, 54); on(); }
        @Override public void onHud(DrawContext ctx, MinecraftClient mc) {
            var p = mc.player; if (p == null) return;
            String f = "%." + decimals.i() + "f";
            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"XYZ", String.format(f + " / " + f + " / " + f, p.getX(), p.getY(), p.getZ())});
            if (nether.b && mc.world != null) {
                String dim = mc.world.getRegistryKey().getValue().getPath();
                if (dim.equals("overworld")) rows.add(new String[]{"Nether", String.format("%.0f / %.0f", p.getX() / 8, p.getZ() / 8)});
                else if (dim.equals("the_nether")) rows.add(new String[]{"Overworld", String.format("%.0f / %.0f", p.getX() * 8, p.getZ() * 8)});
            }
            Hud.rows(ctx, mc, this, rows);
        }
    }

    public static class Direction extends Module {
        final Setting degrees = bool("Show Degrees", true);
        static final String[] N = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        public Direction() { super("Direction", Category.INFO, "Facing direction", true, 6, 78); on(); }
        @Override public void onHud(DrawContext ctx, MinecraftClient mc) {
            if (mc.player == null) return;
            float yaw = net.minecraft.util.math.MathHelper.wrapDegrees(mc.player.getYaw());
            String d = N[((int) Math.floor((yaw + 180 + 22.5) / 45)) & 7];
            Hud.one(ctx, mc, this, "Facing", degrees.b ? d + String.format(" (%.0f)", yaw) : d);
        }
    }

    public static class Speedometer extends Module {
        final Setting unit = mode("Unit", 0, "blocks/s", "km/h");
        final Setting vertical = bool("Include Vertical", false);
        double px, py, pz, speed;
        public Speedometer() { super("Speedometer", Category.INFO, "Movement speed", true, 6, 90); }
        @Override public void onTick(MinecraftClient mc) {
            var p = mc.player; if (p == null) return;
            double dx = p.getX() - px, dy = p.getY() - py, dz = p.getZ() - pz;
            double d = vertical.b ? Math.sqrt(dx * dx + dy * dy + dz * dz) : Math.hypot(dx, dz);
            speed = speed * 0.6 + d * 20 * 0.4;
            px = p.getX(); py = p.getY(); pz = p.getZ();
        }
        @Override public void onHud(DrawContext ctx, MinecraftClient mc) {
            Hud.one(ctx, mc, this, "Speed", unit.mode == 0 ? String.format("%.2f b/s", speed) : String.format("%.1f km/h", speed * 3.6));
        }
    }

    public static class SessionTime extends Module {
        public SessionTime() { super("Session Time", Category.INFO, "Time since the game started", true, 6, 102); }
        @Override public void onHud(DrawContext ctx, MinecraftClient mc) {
            long s = (System.currentTimeMillis() - VantaClient.START) / 1000;
            Hud.one(ctx, mc, this, "Session", String.format("%d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60));
        }
    }

    public static class MemoryUsage extends Module {
        final Setting percent = bool("Show Percent", true);
        public MemoryUsage() { super("Memory Usage", Category.INFO, "JVM memory", true, 6, 114); }
        @Override public void onHud(DrawContext ctx, MinecraftClient mc) {
            Runtime r = Runtime.getRuntime();
            long used = (r.totalMemory() - r.freeMemory()) >> 20, max = r.maxMemory() >> 20;
            Hud.one(ctx, mc, this, "Mem", used + "/" + max + "MB" + (percent.b ? " (" + used * 100 / max + "%)" : ""));
        }
    }

    public static class Clock extends Module {
        final Setting h24 = bool("24 Hour", true), seconds = bool("Seconds", false);
        public Clock() { super("Clock", Category.INFO, "Real-world time", true, 6, 126); }
        @Override public void onHud(DrawContext ctx, MinecraftClient mc) {
            String pat = (h24.b ? "HH:mm" : "hh:mm") + (seconds.b ? ":ss" : "") + (h24.b ? "" : " a");
            Hud.one(ctx, mc, this, null, LocalTime.now().format(DateTimeFormatter.ofPattern(pat)));
        }
    }

    public static class BiomeDisplay extends Module {
        public BiomeDisplay() { super("Biome Display", Category.INFO, "Current biome", true, 6, 138); }
        @Override public void onHud(DrawContext ctx, MinecraftClient mc) {
            if (mc.world == null || mc.player == null) return;
            String id = mc.world.getBiome(mc.player.getBlockPos()).getKey().map(k -> k.getValue().getPath()).orElse("unknown");
            StringBuilder sb = new StringBuilder();
            for (String part : id.split("_")) sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(' ');
            Hud.one(ctx, mc, this, "Biome", sb.toString().trim());
        }
    }

    public static class PlayerCounter extends Module {
        public PlayerCounter() { super("Player Counter", Category.INFO, "Players online", true, 6, 150); }
        @Override public void onHud(DrawContext ctx, MinecraftClient mc) {
            if (mc.getNetworkHandler() == null) return;
            Hud.one(ctx, mc, this, "Players", String.valueOf(mc.getNetworkHandler().getPlayerList().size()));
        }
    }

    public static class ServerAddress extends Module {
        final Setting hide = bool("Hide Address (Streamer)", false);
        public ServerAddress() { super("Server Address", Category.INFO, "Server you are connected to", true, 6, 162); }
        @Override public void onHud(DrawContext ctx, MinecraftClient mc) {
            ServerInfo si = mc.getCurrentServerEntry();
            String a = si == null ? "Singleplayer" : (hide.b ? "hidden" : si.address);
            Hud.one(ctx, mc, this, "Server", a);
        }
    }

    public static class HungerSaturation extends Module {
        final Setting style = mode("Style", 0, "Vanilla Icons", "Bars", "Text");
        final Setting hungerColor = color("Hunger Color", 0xFFFFAA00);
        final Setting saturationColor = color("Saturation Color", 0xFFFFFF55);
        final Setting percent = bool("Show Percent", true);

        public HungerSaturation() {
            super("Hunger & Saturation", Category.INFO, "Hunger and saturation with vanilla-style visuals", true, 6, 174);
            on();
        }

        @Override public void onHud(DrawContext ctx, MinecraftClient mc) {
            if (mc.player == null) return;
            int food = mc.player.getHungerManager().getFoodLevel();
            float sat = mc.player.getHungerManager().getSaturationLevel();
            if (style.mode == 2) {
                Hud.rows(ctx, mc, this, List.of(
                        new String[]{"Hunger", food + "/20"},
                        new String[]{"Saturation", String.format("%.1f/20", sat)}));
                return;
            }
            if (style.mode == 1) {
                int width = 92, height = 24;
                Hud.bg(ctx, this, width, height);
                int inner = 70;
                ctx.drawText(mc.textRenderer, "H", x + 4, y + 4, hungerColor.color, hudShadow());
                ctx.fill(x + 16, y + 4, x + 16 + inner, y + 9, 0x66000000);
                ctx.fill(x + 16, y + 4, x + 16 + Math.round(inner * food / 20f), y + 9, hungerColor.color);
                ctx.drawText(mc.textRenderer, "S", x + 4, y + 14, saturationColor.color, hudShadow());
                ctx.fill(x + 16, y + 14, x + 16 + inner, y + 19, 0x66000000);
                ctx.fill(x + 16, y + 14, x + 16 + Math.round(inner * Math.min(20f, sat) / 20f), y + 19, saturationColor.color);
                return;
            }
            String a = percent.b ? Math.round(food * 5f) + "%" : food + "/20";
            String b = percent.b ? Math.round(sat * 5f) + "%" : String.format("%.1f", sat);
            int width = Math.max(72, 42 + Math.max(mc.textRenderer.getWidth(a), mc.textRenderer.getWidth(b)));
            Hud.bg(ctx, this, width, 38);
            ctx.drawItem(new ItemStack(Items.COOKED_BEEF), x + 3, y + 2);
            ctx.drawText(mc.textRenderer, a, x + 22, y + 6, hungerColor.color, hudShadow());
            ctx.drawItem(new ItemStack(Items.GLOWSTONE_DUST), x + 3, y + 20);
            ctx.drawText(mc.textRenderer, b, x + 22, y + 24, saturationColor.color, hudShadow());
        }
    }

    /** Settings for the drag-and-drop HUD editor (open with Right Ctrl or from the ClickGUI). */
    public static class CustomHudEditor extends Module {
        public static final Setting SNAP = Setting.bool("Snap To Grid", true);
        public static final Setting GRID = Setting.num("Grid Size", 4, 1, 16, 1);
        public static final Setting GUIDES = Setting.bool("Center Guides", true);
        public CustomHudEditor() {
            super("Custom HUD Editor", Category.INFO, "Drag & drop HUD layout (Right Ctrl)", false, 0, 0);
            add(SNAP); add(GRID); add(GUIDES); on();
        }
        public static int snap(int v) { return SNAP.b ? Math.round(v / (float) GRID.i()) * GRID.i() : v; }
    }
}
