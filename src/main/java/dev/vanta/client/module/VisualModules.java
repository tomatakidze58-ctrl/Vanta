package dev.vanta.client.module;

import dev.vanta.client.VantaClient;
import net.minecraft.client.MinecraftClient;

public final class VisualModules {
    private VisualModules() {}

    /**
     * Uniform fullbright.
     *
     * Vanta forces both lightmap brightness helpers to 1.0 through a mixin and
     * also pins vanilla gamma to its maximum while the module is enabled. The
     * original gamma is restored when Fullbright is disabled.
     */
    public static class Fullbright extends Module {
        private double oldGamma = 0.5;
        private boolean captured;

        public Fullbright() {
            super("Fullbright", Category.VISUAL,
                    "Makes dark and bright areas render at maximum light", false, 0, 0);
        }

        @Override
        public void onEnable() {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.options != null) {
                oldGamma = mc.options.getGamma().getValue();
                captured = true;
                mc.options.getGamma().setValue(1.0);
            }
            if (mc.gameRenderer != null) mc.gameRenderer.getLightmapTextureManager().tick();
        }

        @Override
        public void onTick(MinecraftClient mc) {
            if (mc.options.getGamma().getValue() < 1.0) mc.options.getGamma().setValue(1.0);
        }

        @Override
        public void onDisable() {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (captured && mc.options != null) mc.options.getGamma().setValue(oldGamma);
            captured = false;
            if (mc.gameRenderer != null) mc.gameRenderer.getLightmapTextureManager().tick();
        }
    }

    /** Hold C to zoom. FOV modification is handled by GameRendererMixin. */
    public static class Zoom extends Module {
        public final Setting level = num("Zoom Level", 4.0, 1.5, 12.0, 0.5);
        public final Setting smooth = bool("Smooth Zoom", true);
        public final Setting lowSens = bool("Lower Sensitivity", true);

        private float currentFactor = 1.0f;
        private long lastRenderNanos = System.nanoTime();
        private boolean sensitivityChanged;
        private double originalSensitivity;

        public Zoom() {
            super("Zoom", Category.VISUAL, "Hold C for a smooth OptiFine-style zoom", false, 0, 0);
            on();
        }

        public boolean isHeld() {
            MinecraftClient mc = MinecraftClient.getInstance();
            return enabled
                    && VantaClient.zoomKey != null
                    && mc.currentScreen == null
                    && mc.isWindowFocused()
                    && VantaClient.zoomKey.isPressed();
        }

        public float renderFactor() {
            float target = isHeld() ? Math.max(1.0f, level.f()) : 1.0f;
            long now = System.nanoTime();
            float dt = Math.min(0.10f, Math.max(0.0f, (now - lastRenderNanos) / 1_000_000_000.0f));
            lastRenderNanos = now;

            if (!smooth.b) currentFactor = target;
            else {
                float speed = target > currentFactor ? 18.0f : 14.0f;
                float blend = 1.0f - (float) Math.exp(-speed * dt);
                currentFactor += (target - currentFactor) * blend;
                if (Math.abs(currentFactor - target) < 0.002f) currentFactor = target;
            }
            return Math.max(1.0f, currentFactor);
        }

        @Override
        public void onEnable() {
            lastRenderNanos = System.nanoTime();
        }

        @Override
        public void onTick(MinecraftClient mc) {
            boolean held = isHeld();
            if (lowSens.b && held) {
                if (!sensitivityChanged) {
                    originalSensitivity = mc.options.getMouseSensitivity().getValue();
                    sensitivityChanged = true;
                }
                double factor = Math.max(1.0, level.n);
                mc.options.getMouseSensitivity().setValue(originalSensitivity / Math.sqrt(factor));
            } else if (sensitivityChanged) restoreSensitivity(mc);
        }

        private void restoreSensitivity(MinecraftClient mc) {
            mc.options.getMouseSensitivity().setValue(originalSensitivity);
            sensitivityChanged = false;
        }

        @Override
        public void onDisable() {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (sensitivityChanged) restoreSensitivity(mc);
            currentFactor = 1.0f;
            lastRenderNanos = System.nanoTime();
        }
    }

    public static class WeatherChanger extends Module {
        final Setting weather = mode("Weather", 0, "Server", "Clear", "Rain", "Thunder");
        public WeatherChanger() { super("Weather Changer", Category.VISUAL, "Changes weather only on your screen", false, 0, 0); }
        @Override public void onTick(MinecraftClient mc) {
            if (mc.world == null || weather.mode == 0) return;
            float rain = weather.mode >= 2 ? 1f : 0f;
            float thunder = weather.mode == 3 ? 1f : 0f;
            mc.world.setRainGradient(rain);
            mc.world.setThunderGradient(thunder);
        }
    }

    public static class NoVignette extends Module {
        public NoVignette() { super("No Vignette", Category.VISUAL, "Hides the dark screen-edge vignette", false, 0, 0); on(); }
    }

    public static class NoPumpkinOverlay extends Module {
        public NoPumpkinOverlay() { super("No Pumpkin Overlay", Category.VISUAL, "Hides the pumpkin blur overlay", false, 0, 0); on(); }
    }

    public static class NoPortalOverlay extends Module {
        public NoPortalOverlay() { super("No Portal Overlay", Category.VISUAL, "Hides the nether portal screen swirl", false, 0, 0); on(); }
    }

    public static class CustomBlockOutline extends Module {
        public final Setting color = color("Color", 0xFF7C6CFF);
        public final Setting alpha = num("Opacity", 255, 40, 255, 5);
        public final Setting rainbow = bool("Rainbow", false);

        public CustomBlockOutline() {
            super("Block Outline", Category.VISUAL, "Customizes the selected block outline", false, 0, 0);
        }

        public int argb() {
            int rgb = color.color & 0xFFFFFF;
            if (rainbow.b) rgb = net.minecraft.util.math.MathHelper.hsvToRgb((System.currentTimeMillis() % 3000L) / 3000f, 0.75f, 1f);
            return (alpha.i() << 24) | rgb;
        }
    }

    public static class CustomSky extends Module {
        public final Setting preset = mode("Preset", 0, "Server", "Noon", "Sunset", "Midnight", "Sunrise", "Fast Cycle");

        public CustomSky() {
            super("Custom Sky", Category.VISUAL, "Changes the client-side time-of-day look", false, 0, 0);
        }

        public float angle() {
            return switch (preset.mode) {
                case 1 -> 0f;
                case 2 -> 0.25f;
                case 3 -> 0.5f;
                case 4 -> 0.75f;
                case 5 -> (System.currentTimeMillis() % 60000L) / 60000f;
                default -> -1f;
            };
        }
    }
}
