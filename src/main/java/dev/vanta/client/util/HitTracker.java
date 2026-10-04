package dev.vanta.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.sound.SoundEvent;
import java.util.HashMap;
import java.util.Map;

public final class HitTracker {
    public static boolean hitThisTick;
    public static long lastHit;
    private static final Map<Integer, Integer> LAST = new HashMap<>();
    private HitTracker() {}

    public static void tick(MinecraftClient mc) {
        hitThisTick = false;
        if (mc.player == null) return;
        Entity t = mc.targetedEntity;
        if (t instanceof LivingEntity le && le != mc.player) {
            int prev = LAST.getOrDefault(le.getId(), 0);
            long now = System.currentTimeMillis();
            if (le.hurtTime > prev && le.hurtTime >= 8 && now - Clicks.lastLeft < 350) {
                hitThisTick = true; lastHit = now;
            }
            LAST.put(le.getId(), le.hurtTime);
        }
        if (LAST.size() > 64) LAST.clear();
    }

    public static void play(SoundEvent e, float pitch, float volume) {
        MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.ui(e, pitch, volume));
    }
}
