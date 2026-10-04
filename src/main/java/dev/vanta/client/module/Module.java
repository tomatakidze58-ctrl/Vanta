package dev.vanta.client.module;

import dev.vanta.client.util.Notifications;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    public enum Category {
        PVP("PvP"), INFO("HUD"), PERFORMANCE("Performance"), VISUAL("Visual"), UTILITY("Utility");
        public final String label;
        Category(String l) { label = l; }
    }

    public final String name, description;
    public final Category category;
    public boolean enabled;
    public final boolean hasHud;
    public boolean editable;
    public int x, y, w = 60, h = 10;
    public final int defX, defY;
    public final List<Setting> settings = new ArrayList<>();

    // Per-module HUD styling. These are automatically added to every HUD module.
    public Setting hudBackground, hudOpacity, hudTextColor, hudAccentColor, hudShadow;

    protected Module(String name, Category cat, String description, boolean hasHud, int x, int y) {
        this.name = name; this.category = cat; this.description = description;
        this.hasHud = hasHud; this.editable = hasHud;
        this.x = defX = x; this.y = defY = y;

        if (hasHud) {
            hudBackground = bool("HUD Background", true);
            hudOpacity = num("HUD Opacity", 110, 0, 255, 5);
            hudTextColor = color("Text Color", 0xFFFFFFFF);
            hudAccentColor = color("Accent Color", 0xFF7C6CFF);
            hudShadow = bool("Text Shadow", true);
        }
    }

    protected Setting add(Setting s) { settings.add(s); return s; }
    protected Setting bool(String n, boolean d) { return add(Setting.bool(n, d)); }
    protected Setting num(String n, double d, double min, double max, double step) { return add(Setting.num(n, d, min, max, step)); }
    protected Setting color(String n, int d) { return add(Setting.color(n, d)); }
    protected Setting mode(String n, int d, String... m) { return add(Setting.mode(n, d, m)); }
    protected Module on() { enabled = true; return this; }
    protected Module noEdit() { editable = false; return this; }

    public int hudText() { return hudTextColor == null ? 0xFFFFFFFF : hudTextColor.color; }
    public int hudAccent() { return hudAccentColor == null ? 0xFF7C6CFF : hudAccentColor.color; }
    public boolean hudShadow() { return hudShadow == null || hudShadow.b; }

    public void setEnabled(boolean v, boolean notify) {
        if (v == enabled) return;
        enabled = v;
        try { if (v) onEnable(); else onDisable(); } catch (Throwable t) { t.printStackTrace(); }
        if (notify) Notifications.push(name, v ? "Enabled" : "Disabled");
    }
    public void toggle() { setEnabled(!enabled, true); }

    public void onEnable() {}
    public void onDisable() {}
    public void onTick(MinecraftClient mc) {}
    public void onHud(DrawContext ctx, MinecraftClient mc) {}
}
