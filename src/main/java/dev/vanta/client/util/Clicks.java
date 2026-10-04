package dev.vanta.client.util;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import java.util.ArrayDeque;
import java.util.Deque;

/** Frame-accurate click tracking via GLFW polling (no mixin needed). */
public final class Clicks {
    private static final Deque<Long> L = new ArrayDeque<>(), R = new ArrayDeque<>();
    private static boolean pl, pr;
    public static long lastLeft;
    private Clicks() {}

    public static void poll(MinecraftClient mc) {
        long h = mc.getWindow().getHandle();
        boolean l = GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean r = GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        if (mc.currentScreen == null) {
            long now = System.currentTimeMillis();
            if (l && !pl) { L.add(now); lastLeft = now; }
            if (r && !pr) R.add(now);
        }
        pl = l; pr = r;
    }
    private static int count(Deque<Long> q) {
        long now = System.currentTimeMillis();
        while (!q.isEmpty() && now - q.peekFirst() > 1000) q.pollFirst();
        return q.size();
    }
    public static int left() { return count(L); }
    public static int right() { return count(R); }
}
