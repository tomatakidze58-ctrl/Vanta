package dev.vanta.client.gui;

import dev.vanta.client.VantaClient;
import dev.vanta.client.module.Module;
import dev.vanta.client.module.Setting;
import dev.vanta.client.util.Config;
import dev.vanta.client.util.Theme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Vanta's main module menu.
 *
 * Layout is inspired by the clean "client dashboard" style used by modern PvP
 * clients: categories on the left, module cards in the middle, selected module
 * settings on the right. All art/colors are Vanta's own.
 */
public class ClickGuiScreen extends Screen {
    private static Module.Category selectedCategory = Module.Category.PVP;
    private static Module selectedModule;
    private static float moduleScroll;
    private static float settingScroll;

    private boolean prevLeft;
    private boolean prevRight;
    private boolean prevGuiKey;
    private Setting draggingNumber;
    private int dragX;
    private int dragW;
    private String search = "";
    private boolean searchFocused;

    public ClickGuiScreen() {
        super(Text.literal("Vanta Client"));
    }

    @Override
    protected void init() {
        long handle = client.getWindow().getHandle();
        prevLeft = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        prevRight = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        prevGuiKey = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;

        if (selectedModule == null || selectedModule.category != selectedCategory) {
            List<Module> modules = VantaClient.modules.in(selectedCategory);
            selectedModule = modules.isEmpty() ? null : modules.get(0);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void removed() {
        Config.save();
    }

    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private void rect(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + h, color);
    }

    /** Cheap rounded-card look without depending on renderer internals. */
    private void card(DrawContext ctx, int x, int y, int w, int h, int color) {
        if (w <= 4 || h <= 4) {
            rect(ctx, x, y, w, h, color);
            return;
        }
        rect(ctx, x + 2, y, w - 4, h, color);
        rect(ctx, x, y + 2, w, h - 4, color);
        rect(ctx, x + 1, y + 1, w - 2, h - 2, color);
    }

    private String categoryIcon(Module.Category c) {
        return switch (c) {
            case PVP -> "P";
            case INFO -> "H";
            case PERFORMANCE -> "F";
            case VISUAL -> "V";
            case UTILITY -> "U";
        };
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        long handle = client.getWindow().getHandle();
        boolean left = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean right = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        boolean guiKey = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        boolean click = left && !prevLeft;
        boolean rightClick = right && !prevRight;

        if (guiKey && !prevGuiKey) {
            client.setScreen(null);
            return;
        }

        int accent = Theme.accent();

        // World-dimming backdrop.
        rect(ctx, 0, 0, width, height, 0xB4070A12);

        int winW = Math.min(760, Math.max(600, width - 54));
        int winH = Math.min(430, Math.max(330, height - 48));
        int wx = (width - winW) / 2;
        int wy = (height - winH) / 2;

        // Soft orbit "shadow" and main shell.
        card(ctx, wx - 3, wy - 3, winW + 6, winH + 6, Theme.withAlpha(accent, 34));
        card(ctx, wx, wy, winW, winH, 0xF20B0F19);

        int sideW = 150;
        int settingsW = 235;
        int headerH = 54;
        int contentTop = wy + headerH;
        int contentBottom = wy + winH - 16;
        int moduleX = wx + sideW + 14;
        int moduleW = winW - sideW - settingsW - 42;
        int settingsX = moduleX + moduleW + 14;

        // Header.
        ctx.drawText(textRenderer, "O", wx + 20, wy + 16, accent, true);
        ctx.drawText(textRenderer, "ORBIT", wx + 34, wy + 16, 0xFFF5F7FF, true);
        ctx.drawText(textRenderer, "CLIENT", wx + 70, wy + 16, 0xFF737C93, false);
        ctx.drawText(textRenderer, "1.21.11", wx + winW - 52, wy + 17, 0xFF697289, false);
        rect(ctx, wx + 16, wy + 39, winW - 32, 1, 0xFF171D2A);

        // Search modules in the current category.
        int searchX = wx + sideW + 14;
        int searchY = wy + 10;
        int searchW = Math.min(220, moduleW);
        boolean searchHover = inside(mx, my, searchX, searchY, searchW, 24);
        card(ctx, searchX, searchY, searchW, 24, searchFocused ? 0xFF1A2131 : (searchHover ? 0xFF151B28 : 0xFF111622));
        String searchText = search.isEmpty() ? "Search modules..." : search;
        ctx.drawText(textRenderer, searchText, searchX + 9, searchY + 8, search.isEmpty() ? 0xFF626B80 : 0xFFE8EBF5, false);
        if (searchHover && click) searchFocused = true;
        else if (click && !searchHover) searchFocused = false;

        // Sidebar.
        ctx.drawText(textRenderer, "MODULES", wx + 18, contentTop + 4, 0xFF626B80, false);
        int cy = contentTop + 22;
        for (Module.Category category : Module.Category.values()) {
            boolean selected = category == selectedCategory;
            boolean hover = inside(mx, my, wx + 12, cy, sideW - 24, 28);
            if (selected || hover) card(ctx, wx + 12, cy, sideW - 24, 28, selected ? 0xFF171B2A : 0xFF111622);
            if (selected) rect(ctx, wx + 12, cy + 5, 2, 18, accent);

            int textColor = selected ? 0xFFFFFFFF : 0xFFA1A9BA;
            ctx.drawText(textRenderer, categoryIcon(category), wx + 24, cy + 10, selected ? accent : 0xFF697289, true);
            ctx.drawText(textRenderer, category.label, wx + 42, cy + 10, textColor, false);

            if (hover && click) {
                selectedCategory = category;
                moduleScroll = 0;
                settingScroll = 0;
                List<Module> modules = VantaClient.modules.in(category);
                selectedModule = modules.isEmpty() ? null : modules.get(0);
            }
            cy += 34;
        }

        // HUD editor button.
        int hudY = wy + winH - 50;
        boolean hudHover = inside(mx, my, wx + 12, hudY, sideW - 24, 30);
        card(ctx, wx + 12, hudY, sideW - 24, 30, hudHover ? Theme.withAlpha(accent, 70) : 0xFF121725);
        ctx.drawText(textRenderer, "Edit HUD", wx + 37, hudY + 11, 0xFFE8EBF5, false);
        if (hudHover && click) {
            client.setScreen(new HudEditorScreen());
            return;
        }

        // Center: modules.
        String heading = selectedCategory.label;
        ctx.drawText(textRenderer, heading, moduleX, contentTop + 4, 0xFFF2F4FA, true);
        ctx.drawText(textRenderer, "Click a module to customize it", moduleX, contentTop + 17, 0xFF737C93, false);

        List<Module> list = filteredModules();
        int listTop = contentTop + 38;
        int listBottom = contentBottom;
        int rowH = Theme.compactGui.b ? 38 : 48;
        int gap = 7;
        int totalH = list.size() * (rowH + gap);
        float maxScroll = Math.max(0, totalH - (listBottom - listTop));
        moduleScroll = Math.max(0, Math.min(maxScroll, moduleScroll));

        ctx.enableScissor(moduleX, listTop, moduleX + moduleW, listBottom);
        int myy = listTop - (int) moduleScroll;
        for (Module module : list) {
            boolean hover = inside(mx, my, moduleX, myy, moduleW, rowH) && my >= listTop && my < listBottom;
            boolean selected = module == selectedModule;
            int bg = selected ? 0xFF171C2A : (hover ? 0xFF141A27 : 0xFF111622);
            card(ctx, moduleX, myy, moduleW, rowH, bg);
            if (selected) rect(ctx, moduleX, myy + 8, 2, rowH - 16, accent);

            ctx.drawText(textRenderer, module.name, moduleX + 12, myy + 9, module.enabled ? 0xFFFFFFFF : 0xFFD5D8E1, false);
            if (!Theme.compactGui.b) {
                String desc = module.description.length() > 34 ? module.description.substring(0, 31) + "..." : module.description;
                ctx.drawText(textRenderer, desc, moduleX + 12, myy + 24, 0xFF70798E, false);
            }

            int swX = moduleX + moduleW - 34;
            int swY = myy + (rowH - 14) / 2;
            card(ctx, swX, swY, 26, 14, module.enabled ? accent : 0xFF303746);
            card(ctx, module.enabled ? swX + 14 : swX + 2, swY + 2, 10, 10, 0xFFF4F6FB);

            if (hover && click) {
                if (inside(mx, my, swX - 4, swY - 4, 34, 22)) module.toggle();
                else {
                    selectedModule = module;
                    settingScroll = 0;
                }
            }
            if (hover && rightClick) module.toggle();
            myy += rowH + gap;
        }
        ctx.disableScissor();

        // Right: selected module settings.
        card(ctx, settingsX, contentTop, settingsW, contentBottom - contentTop, 0xFF0F141F);
        if (selectedModule != null) {
            ctx.drawText(textRenderer, selectedModule.name, settingsX + 14, contentTop + 14, 0xFFF7F8FC, true);
            String status = selectedModule.enabled ? "Enabled" : "Disabled";
            ctx.drawText(textRenderer, status, settingsX + settingsW - 14 - textRenderer.getWidth(status), contentTop + 14,
                    selectedModule.enabled ? accent : 0xFF737C93, false);

            String desc = selectedModule.description;
            if (desc.length() > 40) desc = desc.substring(0, 37) + "...";
            ctx.drawText(textRenderer, desc, settingsX + 14, contentTop + 29, 0xFF778097, false);
            rect(ctx, settingsX + 12, contentTop + 46, settingsW - 24, 1, 0xFF1A2030);

            int syTop = contentTop + 57;
            int syBottom = contentBottom - 10;
            int settingsHeight = 0;
            for (Setting setting : selectedModule.settings) settingsHeight += settingHeight(setting);
            float maxSettingScroll = Math.max(0, settingsHeight - (syBottom - syTop));
            settingScroll = Math.max(0, Math.min(maxSettingScroll, settingScroll));

            ctx.enableScissor(settingsX + 8, syTop, settingsX + settingsW - 8, syBottom);
            int sy = syTop - (int) settingScroll;
            if (selectedModule.settings.isEmpty()) {
                ctx.drawText(textRenderer, "No extra settings", settingsX + 14, sy + 6, 0xFF737C93, false);
            } else {
                for (Setting setting : selectedModule.settings) {
                    sy = drawSetting(ctx, setting, settingsX + 14, sy, settingsW - 28, mx, my, click, syTop, syBottom);
                }
            }
            ctx.disableScissor();
        } else {
            ctx.drawCenteredTextWithShadow(textRenderer, "No modules", settingsX + settingsW / 2, contentTop + 28, 0xFF737C93);
        }

        // Slider drag remains responsive while mouse is held.
        if (draggingNumber != null) {
            if (left) {
                double p = Math.max(0.0, Math.min(1.0, (mx - dragX) / (double) dragW));
                draggingNumber.setNum(draggingNumber.min + p * (draggingNumber.max - draggingNumber.min));
            } else {
                draggingNumber = null;
            }
        }

        prevLeft = left;
        prevRight = right;
        prevGuiKey = guiKey;
    }

    private static int settingHeight(Setting setting) {
        return switch (setting.type) {
            case BOOL, MODE -> 34;
            case NUM -> 45;
            case COLOR -> 50;
        };
    }

    private int drawSetting(DrawContext ctx, Setting s, int x, int y, int w, int mx, int my,
                            boolean click, int clipTop, int clipBottom) {
        int h = settingHeight(s);
        int accent = Theme.accent();
        boolean visibleHover = my >= clipTop && my < clipBottom && inside(mx, my, x, y, w, h);

        switch (s.type) {
            case BOOL -> {
                ctx.drawText(textRenderer, s.name, x, y + 10, 0xFFD5D9E3, false);
                int bx = x + w - 31;
                int by = y + 7;
                card(ctx, bx, by, 30, 16, s.b ? accent : 0xFF303746);
                card(ctx, s.b ? bx + 16 : bx + 2, by + 2, 12, 12, 0xFFF5F7FB);
                if (visibleHover && click) s.b = !s.b;
            }
            case MODE -> {
                ctx.drawText(textRenderer, s.name, x, y + 5, 0xFF9DA6B8, false);
                String mode = s.modeName();
                int pillW = Math.min(w, Math.max(50, textRenderer.getWidth(mode) + 16));
                int px = x + w - pillW;
                card(ctx, px, y + 18, pillW, 15, 0xFF1C2230);
                ctx.drawText(textRenderer, mode, px + 8, y + 22, accent, false);
                if (visibleHover && click) s.cycle();
            }
            case NUM -> {
                String value = s.step >= 1 ? Integer.toString(s.i()) : String.format("%.2f", s.n);
                ctx.drawText(textRenderer, s.name, x, y + 5, 0xFF9DA6B8, false);
                ctx.drawText(textRenderer, value, x + w - textRenderer.getWidth(value), y + 5, 0xFFE7EAF1, false);
                int by = y + 27;
                rect(ctx, x, by, w, 3, 0xFF2B3242);
                int fw = (int) (w * ((s.n - s.min) / (s.max - s.min)));
                rect(ctx, x, by, fw, 3, accent);
                card(ctx, x + fw - 3, by - 3, 7, 9, 0xFFF6F7FB);
                if (visibleHover && click) {
                    draggingNumber = s;
                    dragX = x;
                    dragW = w;
                }
            }
            case COLOR -> {
                ctx.drawText(textRenderer, s.name, x, y + 5, 0xFF9DA6B8, false);
                int sw = Math.max(12, Math.min(18, (w - 8) / Setting.PALETTE.length));
                int sx = x;
                int sy = y + 23;
                for (int c : Setting.PALETTE) {
                    boolean chosen = s.color == c;
                    card(ctx, sx, sy, sw - 2, 15, chosen ? 0xFFFFFFFF : 0xFF161C29);
                    card(ctx, sx + 2, sy + 2, sw - 6, 11, c);
                    if (click && my >= clipTop && my < clipBottom && inside(mx, my, sx, sy, sw - 2, 15)) s.color = c;
                    sx += sw;
                }
            }
        }
        return y + h;
    }

    private List<Module> filteredModules() {
        List<Module> base = VantaClient.modules.in(selectedCategory);
        if (search == null || search.isBlank()) return base;
        String q = search.toLowerCase(java.util.Locale.ROOT);
        java.util.ArrayList<Module> out = new java.util.ArrayList<>();
        for (Module m : base) {
            if (m.name.toLowerCase(java.util.Locale.ROOT).contains(q) ||
                    m.description.toLowerCase(java.util.Locale.ROOT).contains(q)) out.add(m);
        }
        return out;
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (searchFocused && input.isValidChar() && search.length() < 40) {
            search += input.asString();
            moduleScroll = 0;
            return true;
        }
        return super.charTyped(input);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (searchFocused) {
            int keyCode = input.key();
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
                int end = search.offsetByCodePoints(search.length(), -1);
                search = search.substring(0, end);
                moduleScroll = 0;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                searchFocused = false;
                return true;
            }
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int winW = Math.min(760, Math.max(600, width - 54));
        int winH = Math.min(430, Math.max(330, height - 48));
        int wx = (width - winW) / 2;
        int wy = (height - winH) / 2;
        int sideW = 150;
        int settingsW = 235;
        int moduleX = wx + sideW + 14;
        int moduleW = winW - sideW - settingsW - 42;
        int settingsX = moduleX + moduleW + 14;

        if (mouseX >= settingsX) settingScroll -= (float) verticalAmount * 18f;
        else if (mouseX >= moduleX) moduleScroll -= (float) verticalAmount * 20f;
        return true;
    }
}
