package dev.vanta.client.mixin;

import dev.vanta.client.VantaClient;
import dev.vanta.client.module.PvpModules.LowFire;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameOverlayRenderer.class)
public class InGameOverlayRendererMixin {
    @Inject(method = "renderFireOverlay", at = @At("HEAD"), require = 0)
    private static void orbit$fireLow(MatrixStack matrices, VertexConsumerProvider vcp, Sprite sprite, CallbackInfo ci) {
        matrices.push();
        LowFire m = VantaClient.mod(LowFire.class);
        if (m != null && m.enabled) matrices.translate(0.0, -m.height.n, 0.0);
    }

    @Inject(method = "renderFireOverlay", at = @At("RETURN"), require = 0)
    private static void orbit$firePop(MatrixStack matrices, VertexConsumerProvider vcp, Sprite sprite, CallbackInfo ci) {
        matrices.pop();
    }
}
