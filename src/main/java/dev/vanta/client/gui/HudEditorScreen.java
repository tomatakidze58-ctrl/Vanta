package dev.vanta.client.gui;

import dev.vanta.client.VantaClient;
import dev.vanta.client.module.InfoModules.CustomHudEditor;
import dev.vanta.client.module.Module;
import dev.vanta.client.util.Config;
import dev.vanta.client.util.Theme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** Vanta HUD editor: drag enabled HUD modules, right-click to reset, Right Ctrl/ESC to close. */
public class HudEditorScreen extends Screen {
    private Module dragging;
    private int offsetX, offsetY;
    private boolean prevLeft, prevRight, prevKey;

    public HudEditorScreen() {
        super(Text.literal("Vanta HUD Editor"));
    }

    @Override
    protected void init() {
        long h = client.getWindow().getHandle();
        prevLeft = GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        prevRight = GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        prevKey = GLFW.glfwGetKey(h, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
    }

    @Override public boolean shouldPause() { return false; }
    @Override public void removed() { Config.save(); }

    private Module under(int mx, int my) {
        var list = VantaClient.modules.all;
        for (int i = list.size() - 1; i >= 0; i--) {
            Module m = list.get(i);
            if (m.enabled && m.editable && mx >= m.x - 3 && mx <= m.x + m.w + 3
                    && my >= m.y - 3 && my <= m.y + m.h + 3) return m;
        }
        return null;
    }

    private void frame(DrawContext ctx, Module m, int color) {
        ctx.fill(m.x - 3, m.y - 3, m.x + m.w + 3, m.y - 2, color);
        ctx.fill(m.x - 3, m.y + m.h + 2, m.x + m.w + 3, m.y + m.h + 3, color);
        ctx.fill(m.x - 3, m.y - 3, m.x - 2, m.y + m.h + 3, color);
        ctx.fill(m.x + m.w + 2, m.y - 3, m.x + m.w + 3, m.y + m.h + 3, color);
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        long h = client.getWindow().getHandle();
        boolean left = GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean right = GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        boolean key = GLFW.glfwGetKey(h, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
        boolean click = left && !prevLeft;
        boolean rightClick = right && !prevRight;

        if (key && !prevKey) {
            client.setScreen(null);
            return;
        }

        int accent = Theme.accent();
        ctx.fill(0, 0, width, height, 0x99070A12);

        if (CustomHudEditor.SNAP.b) {
            int grid = Math.max(2, CustomHudEditor.GRID.i());
            for (int x = 0; x < width; x += grid) ctx.fill(x, 0, x + 1, height, 0x0FFFFFFF);
            for (int y = 0; y < height; y += grid) ctx.fill(0, y, width, y + 1, 0x0FFFFFFF);
        }

        if (CustomHudEditor.GUIDES.b) {
            ctx.fill(width / 2, 0, width / 2 + 1, height, 0x557C6CFF);
            ctx.fill(0, height / 2, width, height / 2 + 1, 0x557C6CFF);
        }

        int boxW = 320;
        int bx = (width - boxW) / 2;
        ctx.fill(bx, 8, bx + boxW, 38, 0xEE0D121D);
        ctx.fill(bx, 37, bx + boxW, 38, accent);
        ctx.drawCenteredTextWithShadow(textRenderer, "ORBIT HUD EDITOR", width / 2, 14, 0xFFF4F6FC);
        ctx.drawCenteredTextWithShadow(textRenderer, "Drag to move  •  Right-click to reset  •  Right Ctrl to close",
                width / 2, 25, 0xFF858EA3);

        Module hovered = under(mx, my);
        if (click && hovered != null) {
            dragging = hovered;
            offsetX = mx - hovered.x;
            offsetY = my - hovered.y;
        }
        if (rightClick && hovered != null) {
            hovered.x = hovered.defX;
            hovered.y = hovered.defY;
            Config.save();
        }

        if (dragging != null) {
            if (left) {
                dragging.x = Math.max(0, Math.min(width - dragging.w, CustomHudEditor.snap(mx - offsetX)));
                dragging.y = Math.max(0, Math.min(height - dragging.h, CustomHudEditor.snap(my - offsetY)));
            } else {
                dragging = null;
                Config.save();
            }
        }

        for (Module m : VantaClient.modules.all) {
            if (!m.enabled || !m.editable) continue;
            int color = m == dragging ? accent : (m == hovered ? 0xFFFFFFFF : 0x887C6CFF);
            frame(ctx, m, color);
            if (m == hovered || m == dragging) {
                ctx.drawText(textRenderer, m.name, m.x, Math.max(2, m.y - 12), accent, true);
            }
        }

        prevLeft = left;
        prevRight = right;
        prevKey = key;
    }
}
