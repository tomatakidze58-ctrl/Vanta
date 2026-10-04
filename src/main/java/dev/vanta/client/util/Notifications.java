package dev.vanta.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class Notifications {
    private record Toast(String title, String msg, long time) {}
    private static final List<Toast> LIST = new ArrayList<>();
    private Notifications() {}

    public static void push(String title, String msg) {
        if (!Theme.toasts.b) return;
        synchronized (LIST) {
            LIST.add(new Toast(title, msg, System.currentTimeMillis()));
            if (LIST.size() > 4) LIST.remove(0);
        }
    }

    public static void render(DrawContext ctx, MinecraftClient mc) {
        long now = System.currentTimeMillis();
        int sw = mc.getWindow().getScaledWidth(), y = 6;
        synchronized (LIST) {
            for (Iterator<Toast> it = LIST.iterator(); it.hasNext();) {
                Toast t = it.next();
                long age = now - t.time;
                if (age > 3200) { it.remove(); continue; }
                int w = Math.max(110, Math.max(mc.textRenderer.getWidth(t.title), mc.textRenderer.getWidth(t.msg)) + 14);
                double slide = age < 250 ? 1 - age / 250.0 : (age > 2950 ? (age - 2950) / 250.0 : 0);
                int x = sw - w - 6 + (int) (slide * (w + 10));
                ctx.fill(x, y, x + w, y + 24, 0xDD101014);
                ctx.fill(x, y, x + 2, y + 24, Theme.accent());
                ctx.drawText(mc.textRenderer, t.title, x + 7, y + 4, Theme.accent(), true);
                ctx.drawText(mc.textRenderer, t.msg, x + 7, y + 14, 0xFFFFFFFF, true);
                y += 27;
            }
        }
    }
}
