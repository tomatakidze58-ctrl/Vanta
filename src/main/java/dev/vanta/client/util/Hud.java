package dev.vanta.client.util;

import dev.vanta.client.module.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import java.util.List;

public final class Hud {
    private Hud() {}

    public static void bg(DrawContext ctx, Module m, int w, int h) {
        boolean show = m.hudBackground == null ? Theme.hudBg.b : m.hudBackground.b;
        int alpha = m.hudOpacity == null ? Theme.bgAlpha.i() : m.hudOpacity.i();
        if (show && alpha > 0) ctx.fill(m.x, m.y, m.x + w, m.y + h, alpha << 24);
        m.w = w; m.h = h;
    }

    public static void one(DrawContext ctx, MinecraftClient mc, Module m, String label, String value) {
        rows(ctx, mc, m, List.<String[]>of(new String[]{label, value}));
    }

    public static void rows(DrawContext ctx, MinecraftClient mc, Module m, List<String[]> rows) {
        TextRenderer tr = mc.textRenderer;
        int pad = 3, lh = 10, width = 0;
        for (String[] r : rows) width = Math.max(width, tr.getWidth(r[0] == null ? "" : r[0] + ": ") + tr.getWidth(r[1]));
        bg(ctx, m, width + pad * 2, rows.size() * lh + pad * 2 - 1);
        int yy = m.y + pad;
        for (String[] r : rows) {
            int xx = m.x + pad;
            if (r[0] != null) {
                String l = r[0] + ": ";
                ctx.drawText(tr, l, xx, yy, m.hudAccent(), m.hudShadow());
                xx += tr.getWidth(l);
            }
            ctx.drawText(tr, r[1], xx, yy, m.hudText(), m.hudShadow());
            yy += lh;
        }
    }

    public static int gradient(float pct) {
        return 0xFF000000 | net.minecraft.util.math.MathHelper.hsvToRgb(Math.max(0, Math.min(1, pct)) * 0.33f, 0.9f, 1f);
    }
}
