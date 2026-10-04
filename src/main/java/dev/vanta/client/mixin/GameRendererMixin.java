package dev.vanta.client.mixin;

import dev.vanta.client.VantaClient;
import dev.vanta.client.module.VisualModules.Fullbright;
import dev.vanta.client.module.VisualModules.Zoom;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(
            method = "getFov(Lnet/minecraft/client/render/Camera;FZ)F",
            at = @At("RETURN"),
            cancellable = true,
            require = 0
    )
    private void orbit$zoom(Camera camera, float tickProgress, boolean changingFov,
                            CallbackInfoReturnable<Float> cir) {
        Zoom zoom = VantaClient.mod(Zoom.class);
        if (zoom == null || !zoom.enabled) return;

        float factor = zoom.renderFactor();
        if (factor <= 1.001f) return;
        cir.setReturnValue(Math.max(1.0f, cir.getReturnValueF() / factor));
    }

    // Removes the renderer's extra night-darkness contribution while Fullbright is active.
    @Inject(method = "getSkyDarkness(F)F", at = @At("RETURN"), cancellable = true, require = 0)
    private void orbit$fullbrightSky(float tickProgress, CallbackInfoReturnable<Float> cir) {
        if (VantaClient.on(Fullbright.class)) cir.setReturnValue(0.0f);
    }
}
