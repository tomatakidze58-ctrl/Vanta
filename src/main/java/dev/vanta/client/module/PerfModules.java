package dev.vanta.client.module;

import dev.vanta.client.VantaClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.particle.ParticlesMode;

public final class PerfModules {
    private PerfModules() {}

    /** Vanta particle control. "Off" is enforced by ParticleManagerMixin. */
    public static class Particles extends Module {
        public final Setting level = mode("Amount", 0, "Vanilla", "Reduced", "Minimal", "Off");
        private ParticlesMode old;

        public Particles() {
            super("Particles", Category.PERFORMANCE, "Controls how many particles are shown", false, 0, 0);
        }

        public boolean hidesAll() {
            return enabled && level.mode == 3;
        }

        @Override
        public void onEnable() {
            old = MinecraftClient.getInstance().options.getParticles().getValue();
            apply(MinecraftClient.getInstance());
        }

        @Override
        public void onTick(MinecraftClient mc) {
            apply(mc);
        }

        private void apply(MinecraftClient mc) {
            ParticlesMode wanted = switch (level.mode) {
                case 0 -> old == null ? ParticlesMode.ALL : old;
                case 1 -> ParticlesMode.DECREASED;
                default -> ParticlesMode.MINIMAL;
            };
            if (mc.options.getParticles().getValue() != wanted) mc.options.getParticles().setValue(wanted);
        }

        @Override
        public void onDisable() {
            if (old != null) MinecraftClient.getInstance().options.getParticles().setValue(old);
        }
    }

    public static class FpsLimiter extends Module {
        public final Setting cap = num("Max FPS", 240, 30, 260, 10);
        private int old = -1;

        public FpsLimiter() {
            super("FPS Limiter", Category.PERFORMANCE, "Sets a custom foreground FPS cap", false, 0, 0);
        }

        @Override public void onEnable() { old = MinecraftClient.getInstance().options.getMaxFps().getValue(); }
        @Override public void onTick(MinecraftClient mc) {
            if (mc.options.getMaxFps().getValue() != cap.i()) mc.options.getMaxFps().setValue(cap.i());
        }
        @Override public void onDisable() {
            if (old > 0) MinecraftClient.getInstance().options.getMaxFps().setValue(old);
        }
    }

    public static class DynamicFps extends Module {
        public final Setting bgFps = num("Unfocused FPS", 15, 1, 60, 1);
        private int original = -1;
        private boolean throttled;

        public DynamicFps() {
            super("Dynamic FPS", Category.PERFORMANCE, "Lowers FPS while Minecraft is unfocused", false, 0, 0);
            on();
        }

        @Override
        public void onTick(MinecraftClient mc) {
            boolean focused = mc.isWindowFocused();
            if (!focused && !throttled) {
                original = mc.options.getMaxFps().getValue();
                throttled = true;
            }
            if (!focused) mc.options.getMaxFps().setValue(bgFps.i());
            else if (throttled) {
                throttled = false;
                FpsLimiter limiter = VantaClient.mod(FpsLimiter.class);
                mc.options.getMaxFps().setValue(limiter != null && limiter.enabled ? limiter.cap.i() : original);
            }
        }

        @Override
        public void onDisable() {
            if (throttled && original > 0) MinecraftClient.getInstance().options.getMaxFps().setValue(original);
            throttled = false;
        }
    }

    public static class WeatherOptimization extends Module {
        public final Setting cap = num("Max Rain Intensity %", 40, 0, 100, 5);

        public WeatherOptimization() {
            super("Weather Optimization", Category.PERFORMANCE, "Reduces heavy rain and thunder rendering", false, 0, 0);
        }

        @Override
        public void onTick(MinecraftClient mc) {
            if (mc.world == null) return;
            float c = cap.f() / 100f;
            if (mc.world.getRainGradient(1f) > c) mc.world.setRainGradient(c);
            if (mc.world.getThunderGradient(1f) > c) mc.world.setThunderGradient(c);
        }
    }
}
