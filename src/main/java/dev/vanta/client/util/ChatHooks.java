package dev.vanta.client.util;

import dev.vanta.client.VantaClient;
import dev.vanta.client.module.UtilityModules.*;
import net.minecraft.text.Text;

public final class ChatHooks {
    private ChatHooks() {}
    public static Text process(Text t) {
        ChatState.capture(t.getString());
        if (VantaClient.modules == null) return t;
        try {
            ChatNotifications cn = VantaClient.mod(ChatNotifications.class);
            if (cn != null && cn.enabled) cn.inspect(t.getString());
            ChatTimestamps ts = VantaClient.mod(ChatTimestamps.class);
            if (ts != null && ts.enabled) return ts.stamp(t);
        } catch (Throwable ignored) {}
        return t;
    }
}
