package dev.vanta.client.module;

import dev.vanta.client.module.InfoModules.*;
import dev.vanta.client.module.PerfModules.*;
import dev.vanta.client.module.PvpModules.*;
import dev.vanta.client.module.UtilityModules.*;
import dev.vanta.client.module.VisualModules.*;
import dev.vanta.client.module.PopularModules.*;
import dev.vanta.client.module.IntegratedModules.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import java.util.ArrayList;
import java.util.List;

public class ModuleManager {
    public final List<Module> all = new ArrayList<>();
    private boolean started;

    public ModuleManager() {
        // PvP
        all.add(new Keystrokes());      all.add(new CpsCounter());      all.add(new ArmorHud());
        all.add(new PotionEffects());   all.add(new DurabilityHud());   all.add(new TotemCounter());
        all.add(new ItemCounter());     all.add(new AttackIndicator()); all.add(new AttackCooldown());
        all.add(new MaceCooldown());    all.add(new CrosshairCustomizer()); all.add(new HitColor());
        all.add(new HitSound());        all.add(new LowFire());         all.add(new LowShield());
        all.add(new AutoSprint());
        // Info
        all.add(new FpsCounter());      all.add(new PingDisplay());     all.add(new Coordinates());
        all.add(new Direction());       all.add(new Speedometer());     all.add(new SessionTime());
        all.add(new MemoryUsage());     all.add(new Clock());           all.add(new BiomeDisplay());
        all.add(new PlayerCounter());   all.add(new ServerAddress());   all.add(new HungerSaturation());
        all.add(new CustomHudEditor());
        // Popular HUD / info modules
        all.add(new HealthDisplay());       all.add(new ArmorCondition());      all.add(new ExperienceDisplay());
        all.add(new DayCounter());          all.add(new DimensionDisplay());     all.add(new ChunkPosition());
        all.add(new TargetInfo());          all.add(new HeldItemInfo());         all.add(new FoodPreview());
        all.add(new GappleCounter());       all.add(new PearlCounter());         all.add(new ArrowCounter());
        all.add(new CrystalCounter());      all.add(new XpBottleCounter());      all.add(new ObsidianCounter());
        all.add(new PotionCounter());       all.add(new ShieldCounter());        all.add(new Stopwatch());
        all.add(new DateDisplay());         all.add(new PingGraph());
        // Performance
        all.add(new Particles());       all.add(new DynamicFps());
        all.add(new WeatherOptimization()); all.add(new FpsLimiter());
        all.add(new ViewDistanceCap());     all.add(new EntityDistance());
        all.add(new DistantChunks());       all.add(new ClientOptimizer());
        // Visual
        all.add(new Fullbright());      all.add(new Zoom());            all.add(new WeatherChanger());
        all.add(new NoVignette());      all.add(new NoPumpkinOverlay()); all.add(new NoPortalOverlay());
        all.add(new CustomBlockOutline()); all.add(new CustomSky());
        all.add(new FovChanger());          all.add(new ViewBobbing());          all.add(new DamageTilt());
        all.add(new DynamicFovControl());   all.add(new DistortionControl());    all.add(new GlintStrength());
        all.add(new EntityShadows());       all.add(new ChatAppearance());       all.add(new GuiScaleControl());
        all.add(new SkinLayers());
        // Utility
        all.add(new VantaSettings());   all.add(new AutoRespawn());     all.add(new ScreenshotManager());
        all.add(new ChatTimestamps());  all.add(new ChatNotifications());
        all.add(new ToggleSneakMode());     all.add(new ToggleSprintMode());
        // Extra PvP display modules
        all.add(new ComboCounter());        all.add(new HitDistance());
        // Integrated QoL / HUD features
        all.add(new ChatHeads());           all.add(new ContainerPreview());
        all.add(new Waypoint());            all.add(new TierTag());
    }

    public <T extends Module> T get(Class<T> c) {
        for (Module m : all) if (c.isInstance(m)) return c.cast(m);
        return null;
    }

    public List<Module> in(Module.Category c) {
        List<Module> l = new ArrayList<>();
        for (Module m : all) if (m.category == c) l.add(m);
        return l;
    }

    /** Runs onEnable for modules enabled by defaults/config (needs a live client). */
    public void tick(MinecraftClient mc) {
        if (!started) {
            started = true;
            for (Module m : all) if (m.enabled) { try { m.onEnable(); } catch (Throwable t) { t.printStackTrace(); } }
        }
        for (Module m : all) if (m.enabled) {
            try { m.onTick(mc); } catch (Throwable t) { t.printStackTrace(); }
        }
    }

    public void renderHud(DrawContext ctx, MinecraftClient mc) {
        for (Module m : all) if (m.enabled && m.hasHud) {
            try { m.onHud(ctx, mc); } catch (Throwable t) { t.printStackTrace(); }
        }
    }
}
