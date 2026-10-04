package dev.vanta.client.module;

import dev.vanta.client.VantaClient;
import dev.vanta.client.util.HitTracker;
import dev.vanta.client.util.Notifications;
import dev.vanta.client.util.Theme;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

public final class UtilityModules {
    private UtilityModules() {}

    /** Global Vanta appearance/settings page shown like a normal module in the Utility category. */
    public static class VantaSettings extends Module {
        public VantaSettings() {
            super("Vanta Settings", Category.UTILITY, "Client colors, HUD style and notifications", false, 0, 0);
            settings.addAll(Theme.settings);
            on();
        }
    }

    public static class AutoRespawn extends Module {
        final Setting delay = num("Delay (ticks)", 10, 0, 60, 1);
        int t;
        public AutoRespawn() { super("Auto Respawn", Category.UTILITY, "Respawns instantly", false, 0, 0); }
        @Override public void onTick(MinecraftClient mc) {
            if (mc.player != null && mc.currentScreen instanceof DeathScreen) {
                if (++t >= delay.i()) { mc.player.requestRespawn(); t = 0; }
            } else t = 0;
        }
    }

    /** Toast when a screenshot is saved + key to open the folder + count. */
    public static class ScreenshotManager extends Module {
        final Setting toast = bool("Saved Toast", true);
        long lastSeen = -1; int ticks;
        public ScreenshotManager() { super("Screenshot Manager", Category.UTILITY, "Toasts + open folder key", false, 0, 0); on(); }
        static Path dir() { return FabricLoader.getInstance().getGameDir().resolve("screenshots"); }

        public static void openFolder() {
            try { Files.createDirectories(dir()); Util.getOperatingSystem().open(dir().toUri()); } catch (Exception e) { e.printStackTrace(); }
        }

        @Override public void onTick(MinecraftClient mc) {
            if (++ticks % 30 != 0) return;
            try (Stream<Path> s = Files.exists(dir()) ? Files.list(dir()) : Stream.empty()) {
                long newest = s.filter(p -> p.toString().endsWith(".png")).mapToLong(p -> p.toFile().lastModified()).max().orElse(0);
                if (lastSeen >= 0 && newest > lastSeen && toast.b) Notifications.push("Screenshot", "Saved to /screenshots");
                lastSeen = Math.max(newest, 0);
            } catch (Exception ignored) {}
        }
    }

    /** Applied through ChatHudMixin -> ChatHooks.process. */
    public static class ChatTimestamps extends Module {
        public final Setting h24 = bool("24 Hour", true), seconds = bool("Seconds", false);
        public ChatTimestamps() { super("Chat Timestamps", Category.UTILITY, "[HH:mm] before every message", false, 0, 0); on(); }
        public Text stamp(Text msg) {
            String pat = (h24.b ? "HH:mm" : "hh:mm") + (seconds.b ? ":ss" : "") + (h24.b ? "" : " a");
            return Text.literal("[" + LocalTime.now().format(DateTimeFormatter.ofPattern(pat)) + "] ").formatted(Formatting.GRAY).append(msg);
        }
    }

    public static class ChatNotifications extends Module {
        final Setting mentions = bool("Name Mentions", true), whispers = bool("Whispers", true),
                      sound = bool("Sound", true), toast = bool("Toast", true);
        long cooldown;
        public ChatNotifications() { super("Chat Notifications", Category.UTILITY, "Alerts on mentions and whispers", false, 0, 0); on(); }
        public void inspect(String s) {
            var mc = MinecraftClient.getInstance();
            if (mc.player == null) return;
            long now = System.currentTimeMillis();
            if (now - cooldown < 800) return;
            String me = mc.player.getName().getString();
            boolean own = s.startsWith("<" + me + ">");
            String kind = null;
            if (whispers.b && (s.contains("whispers to you") || s.startsWith("From "))) kind = "Whisper";
            else if (mentions.b && !own && s.contains(me)) kind = "Mention";
            if (kind == null) return;
            cooldown = now;
            if (sound.b) HitTracker.play(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.6f, 1f);
            if (toast.b) Notifications.push(kind, s.length() > 40 ? s.substring(0, 40) + "..." : s);
        }
    }
}
