package dev.vanta.client;

import dev.vanta.client.gui.ClickGuiScreen;
import dev.vanta.client.gui.HudEditorScreen;
import dev.vanta.client.module.ModuleManager;
import dev.vanta.client.module.PvpModules.CrosshairCustomizer;
import dev.vanta.client.module.UtilityModules;
import dev.vanta.client.util.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class VantaClient implements ClientModInitializer {
    public static final String NAME = "Vanta Client";
    public static final long START = System.currentTimeMillis();
    public static ModuleManager modules;
    public static KeyBinding guiKey, hudKey, zoomKey, folderKey;

    public static <T extends dev.vanta.client.module.Module> T mod(Class<T> c) {
        return modules == null ? null : modules.get(c);
    }

    public static boolean on(Class<? extends dev.vanta.client.module.Module> c) {
        dev.vanta.client.module.Module m = mod(c);
        return m != null && m.enabled;
    }

    @Override
    public void onInitializeClient() {
        modules = new ModuleManager();
        Config.load();

        KeyBinding.Category cat = KeyBinding.Category.create(Identifier.of("vantaclient", "main"));
        guiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.vantaclient.gui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, cat));
        hudKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.vantaclient.hud", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_CONTROL, cat));
        zoomKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.vantaclient.zoom", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, cat));
        folderKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.vantaclient.folder", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, cat));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (guiKey.wasPressed()) mc.setScreen(new ClickGuiScreen());
            while (hudKey.wasPressed()) mc.setScreen(new HudEditorScreen());
            while (folderKey.wasPressed()) UtilityModules.ScreenshotManager.openFolder();
            if (mc.player == null) return;
            HitTracker.tick(mc);
            modules.tick(mc);
        });

        HudElementRegistry.addLast(Identifier.of("vantaclient", "hud"), (ctx, tick) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            Clicks.poll(mc);
            if (mc.options.hudHidden || mc.player == null) return;

            if (Theme.watermark.b && !mc.getDebugHud().shouldShowDebugHud()) {
                String label = "O  Vanta";
                int x = mc.getWindow().getScaledWidth() - 5 - mc.textRenderer.getWidth(label);
                int y = mc.getWindow().getScaledHeight() - 13;
                ctx.drawText(mc.textRenderer, "O", x, y, Theme.accent(), true);
                ctx.drawText(mc.textRenderer, "  Vanta", x + mc.textRenderer.getWidth("O"), y, 0xFFB8BECC, true);
            }

            if (!mc.getDebugHud().shouldShowDebugHud()) modules.renderHud(ctx, mc);
            Notifications.render(ctx, mc);
        });

        HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, original -> (ctx, tick) -> {
            CrosshairCustomizer crosshair = mod(CrosshairCustomizer.class);
            if (crosshair != null && crosshair.enabled) crosshair.drawCrosshair(ctx);
            else original.render(ctx, tick);
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(c -> Config.save());
    }
}
