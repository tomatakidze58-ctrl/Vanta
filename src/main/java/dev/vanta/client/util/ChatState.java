package dev.vanta.client.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Lightweight chat state used by Vanta's Chat Heads HUD. */
public final class ChatState {
    private static final Pattern ANGLE = Pattern.compile("^<([^>]{1,32})>\\s*(.*)$");
    private static final Pattern COLON = Pattern.compile("^([A-Za-z0-9_]{1,16}):\\s*(.*)$");

    public static String lastSpeaker = "";
    public static String lastMessage = "";
    public static long lastAt;

    private ChatState() {}

    public static void capture(String raw) {
        if (raw == null || raw.isBlank()) return;
        Matcher m = ANGLE.matcher(raw);
        if (!m.find()) {
            m = COLON.matcher(raw);
            if (!m.find()) return;
        }
        lastSpeaker = m.group(1);
        lastMessage = m.group(2);
        lastAt = System.currentTimeMillis();
    }
}
