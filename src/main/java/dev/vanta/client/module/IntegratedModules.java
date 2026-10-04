package dev.vanta.client.module;

import dev.vanta.client.util.ChatState;
import dev.vanta.client.util.Hud;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Vanta-owned reimplementations of popular client/QoL ideas.
 * No third-party JAR code is bundled or copied here.
 */
public final class IntegratedModules {
    private IntegratedModules() {}

    /** Extended client-side view-distance control. It can only show chunks the client actually has. */
    public static class DistantChunks extends Module {
        public final Setting chunks = num("Client Chunks", 24, 2, 32, 1);
        private int old = -1;

        public DistantChunks() {
            super("Distant Chunks", Category.PERFORMANCE,
                    "Keeps the client render distance high for available chunks", false, 0, 0);
        }

        @Override public void onEnable() {
            var o = MinecraftClient.getInstance().options.getViewDistance();
            old = o.getValue();
            o.setValue(chunks.i());
        }

        @Override public void onTick(MinecraftClient mc) {
            if (mc.options.getViewDistance().getValue() != chunks.i()) {
                mc.options.getViewDistance().setValue(chunks.i());
            }
        }

        @Override public void onDisable() {
            if (old > 0) MinecraftClient.getInstance().options.getViewDistance().setValue(old);
            old = -1;
        }
    }

    /** Shows the most recent player's skin head and message in a compact HUD card. */
    public static class ChatHeads extends Module {
        public final Setting timeout = num("Visible Seconds", 8, 2, 30, 1);
        public final Setting message = bool("Show Message", true);
        public final Setting headSize = num("Head Size", 16, 10, 24, 1);

        public ChatHeads() {
            super("Chat Heads", Category.INFO, "Shows the latest chat speaker with their skin head", true, 300, 110);
        }

        @Override public void onHud(DrawContext ctx, MinecraftClient mc) {
            if (ChatState.lastSpeaker.isBlank()) return;
            if (System.currentTimeMillis() - ChatState.lastAt > timeout.n * 1000L) return;
            if (mc.getNetworkHandler() == null) return;

            PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(ChatState.lastSpeaker);
            int size = headSize.i();
            String msg = message.b ? ChatState.lastMessage : "";
            if (msg.length() > 32) msg = msg.substring(0, 29) + "...";

            int textW = Math.max(mc.textRenderer.getWidth(ChatState.lastSpeaker), mc.textRenderer.getWidth(msg));
            int width = size + 8 + textW + 6;
            int height = Math.max(size + 6, message.b ? 24 : 16);
            Hud.bg(ctx, this, width, height);

            if (entry != null) {
                PlayerSkinDrawer.draw(ctx, entry.getSkinTextures(), x + 3, y + (height - size) / 2, size);
            } else {
                ctx.fill(x + 3, y + 3, x + 3 + size, y + 3 + size, 0xFF252B3B);
                String i = ChatState.lastSpeaker.substring(0, 1).toUpperCase();
                ctx.drawCenteredTextWithShadow(mc.textRenderer, i, x + 3 + size / 2, y + 7, hudAccent());
            }

            int tx = x + size + 7;
            ctx.drawText(mc.textRenderer, ChatState.lastSpeaker, tx, y + 4, hudAccent(), hudShadow());
            if (message.b && !msg.isBlank()) ctx.drawText(mc.textRenderer, msg, tx, y + 14, hudText(), hudShadow());
        }
    }

    /** Mini inventory preview for container items such as shulker boxes. */
    public static class ContainerPreview extends Module {
        public final Setting slots = num("Max Items", 27, 9, 27, 9);
        public final Setting offhand = bool("Check Offhand", true);

        public ContainerPreview() {
            super("Shulker Preview", Category.INFO, "Shows held container contents in a compact grid", true, 300, 145);
        }

        private ItemStack find(MinecraftClient mc) {
            if (mc.player == null) return ItemStack.EMPTY;
            ItemStack main = mc.player.getMainHandStack();
            if (main.get(DataComponentTypes.CONTAINER) != null) return main;
            if (offhand.b) {
                ItemStack off = mc.player.getOffHandStack();
                if (off.get(DataComponentTypes.CONTAINER) != null) return off;
            }
            return ItemStack.EMPTY;
        }

        @Override public void onHud(DrawContext ctx, MinecraftClient mc) {
            ItemStack stack = find(mc);
            if (stack.isEmpty()) return;
            ContainerComponent container = stack.get(DataComponentTypes.CONTAINER);
            if (container == null) return;

            List<ItemStack> items = new ArrayList<>();
            for (ItemStack s : container.iterateNonEmptyCopy()) {
                if (items.size() >= slots.i()) break;
                items.add(s);
            }
            if (items.isEmpty()) return;

            int cols = 9;
            int rows = (items.size() + cols - 1) / cols;
            int width = cols * 18 + 6;
            int height = 14 + rows * 18 + 4;
            Hud.bg(ctx, this, width, height);
            ctx.drawText(mc.textRenderer, stack.getName().getString(), x + 3, y + 3, hudAccent(), hudShadow());
            for (int i = 0; i < items.size(); i++) {
                int sx = x + 3 + (i % cols) * 18;
                int sy = y + 14 + (i / cols) * 18;
                ItemStack s = items.get(i);
                ctx.drawItem(s, sx, sy);
                ctx.drawStackOverlay(mc.textRenderer, s, sx, sy);
            }
        }
    }

    /** Keeps vanilla outer skin layers enabled. */
    public static class SkinLayers extends Module {
        public final Setting hat = bool("Hat", true);
        public final Setting jacket = bool("Jacket", true);
        public final Setting sleeves = bool("Sleeves", true);
        public final Setting pants = bool("Pants", true);
        public final Setting cape = bool("Cape", true);

        public SkinLayers() {
            super("Skin Layers", Category.VISUAL, "Controls visible outer player skin layers", false, 0, 0);
        }

        @Override public void onTick(MinecraftClient mc) {
            mc.options.setPlayerModelPart(PlayerModelPart.HAT, hat.b);
            mc.options.setPlayerModelPart(PlayerModelPart.JACKET, jacket.b);
            mc.options.setPlayerModelPart(PlayerModelPart.LEFT_SLEEVE, sleeves.b);
            mc.options.setPlayerModelPart(PlayerModelPart.RIGHT_SLEEVE, sleeves.b);
            mc.options.setPlayerModelPart(PlayerModelPart.LEFT_PANTS_LEG, pants.b);
            mc.options.setPlayerModelPart(PlayerModelPart.RIGHT_PANTS_LEG, pants.b);
            mc.options.setPlayerModelPart(PlayerModelPart.CAPE, cape.b);
        }
    }

    /** Simple client-side waypoint with distance and direction. */
    public static class Waypoint extends Module {
        public final Setting wx = num("X", 0, -30000000, 30000000, 1);
        public final Setting wy = num("Y", 64, -64, 512, 1);
        public final Setting wz = num("Z", 0, -30000000, 30000000, 1);
        public final Setting showY = bool("Use Y", true);
        public final Setting direction = bool("Direction Arrow", true);

        public Waypoint() {
            super("Waypoint", Category.INFO, "Configurable waypoint with distance and direction", true, 300, 190);
        }

        @Override public void onHud(DrawContext ctx, MinecraftClient mc) {
            if (mc.player == null) return;
            double dx = wx.n - mc.player.getX();
            double dz = wz.n - mc.player.getZ();
            double dy = showY.b ? wy.n - mc.player.getY() : 0;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

            String arrow = "";
            if (direction.b) {
                double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
                double diff = wrap(targetYaw - mc.player.getYaw());
                arrow = diff > 45 && diff < 135 ? " >" : diff < -45 && diff > -135 ? " <" : Math.abs(diff) >= 135 ? " v" : " ^";
            }
            Hud.one(ctx, mc, this, "Waypoint" + arrow, String.format("%.1fm  (%.0f, %.0f, %.0f)", dist, wx.n, wy.n, wz.n));
        }

        private static double wrap(double a) {
            while (a <= -180) a += 360;
            while (a > 180) a -= 360;
            return a;
        }
    }

    /** Local tier label for target display; no external service is contacted. */
    public static class TierTag extends Module {
        public final Setting tier = mode("Tier", 2, "LT5", "HT5", "LT4", "HT4", "LT3", "HT3", "LT2", "HT2", "LT1", "HT1");
        public final Setting showTarget = bool("Show Looked-at Player", true);

        public TierTag() {
            super("Tier Tag", Category.INFO, "Local PvP tier tag display", true, 300, 220);
        }

        @Override public void onHud(DrawContext ctx, MinecraftClient mc) {
            String name = "You";
            if (showTarget.b && mc.targetedEntity != null) name = mc.targetedEntity.getName().getString();
            Hud.one(ctx, mc, this, name, tier.modeName());
        }
    }

    /** Convenient performance preset made only from vanilla options already controlled by Vanta. */
    public static class ClientOptimizer extends Module {
        public final Setting particles = mode("Particles", 2, "Vanilla", "Reduced", "Minimal");
        public final Setting entityDistance = num("Entity Distance %", 75, 50, 200, 25);
        private double oldEntity = 1.0;
        private net.minecraft.particle.ParticlesMode oldParticles;

        public ClientOptimizer() {
            super("Client Optimizer", Category.PERFORMANCE, "Lightweight vanilla-safe performance preset", false, 0, 0);
        }

        @Override public void onEnable() {
            MinecraftClient mc = MinecraftClient.getInstance();
            oldEntity = mc.options.getEntityDistanceScaling().getValue();
            oldParticles = mc.options.getParticles().getValue();
            apply(mc);
        }

        @Override public void onTick(MinecraftClient mc) { apply(mc); }

        private void apply(MinecraftClient mc) {
            mc.options.getEntityDistanceScaling().setValue(entityDistance.n / 100.0);
            var p = switch (particles.mode) {
                case 1 -> net.minecraft.particle.ParticlesMode.DECREASED;
                case 2 -> net.minecraft.particle.ParticlesMode.MINIMAL;
                default -> oldParticles == null ? net.minecraft.particle.ParticlesMode.ALL : oldParticles;
            };
            mc.options.getParticles().setValue(p);
        }

        @Override public void onDisable() {
            MinecraftClient mc = MinecraftClient.getInstance();
            mc.options.getEntityDistanceScaling().setValue(oldEntity);
            if (oldParticles != null) mc.options.getParticles().setValue(oldParticles);
        }
    }
}
