package dev.vanta.client.util;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/** Draws the "cr" Vanta-style logo using plain fills (no texture needed). */
public final class Logo {
    private Logo() {}

    public static void draw(DrawContext ctx, TextRenderer tr, int x, int y, int size) {
        double r = size / 2.0;
        for (int py = 0; py < size; py++) {
            for (int px = 0; px < size; px++) {
                double dx = px + 0.5 - r, dy = py + 0.5 - r, d = Math.sqrt(dx * dx + dy * dy);
                if (d > r) continue;
                int col;
                if (d > r * 0.62) {
                    double a = Math.toDegrees(Math.atan2(dy, dx));
                    col = (a >= -150 && a < -30) ? 0xFFDB4437 : (a >= -30 && a < 90) ? 0xFFF4B400 : 0xFF0F9D58;
                } else if (d > r * 0.52) col = 0xFFFFFFFF;
                else col = 0xFF1A73E8;
                ctx.fill(x + px, y + py, x + px + 1, y + py + 1, col);
            }
        }
        ctx.drawCenteredTextWithShadow(tr, "cr", x + size / 2, y + size / 2 - 4, 0xFFFFFFFF);
    }
}
