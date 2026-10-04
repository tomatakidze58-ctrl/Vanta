package dev.vanta.client.util;

import dev.vanta.client.module.Setting;
import java.util.ArrayList;
import java.util.List;

/** Shared Vanta visual settings. Kept deliberately small so every HUD module can still override its own colors. */
public final class Theme {
    public static final List<Setting> settings = new ArrayList<>();

    public static final Setting accent     = add(Setting.color("Vanta Accent", 0xFF9B5CFF));
    public static final Setting hudBg      = add(Setting.bool("HUD Background", true));
    public static final Setting bgAlpha    = add(Setting.num("Background Opacity", 115, 0, 255, 5));
    public static final Setting shadow     = add(Setting.bool("Text Shadow", true));
    public static final Setting toasts     = add(Setting.bool("Notifications", true));
    public static final Setting watermark  = add(Setting.bool("Watermark", true));
    public static final Setting rainbow    = add(Setting.bool("Rainbow Accent", false));
    public static final Setting compactGui = add(Setting.bool("Compact Module Cards", false));

    private Theme() {}
    private static Setting add(Setting s) { settings.add(s); return s; }

    public static int accent() {
        if (rainbow.b) {
            float h = (System.currentTimeMillis() % 5000L) / 5000f;
            return 0xFF000000 | net.minecraft.util.math.MathHelper.hsvToRgb(h, 0.58f, 1f);
        }
        return accent.color;
    }

    public static int withAlpha(int argb, int a) {
        return ((a & 255) << 24) | (argb & 0xFFFFFF);
    }
}
