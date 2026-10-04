package dev.vanta.client.mixin;

import dev.vanta.client.VantaClient;
import dev.vanta.client.module.VisualModules.CustomBlockOutline;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
    // The only int argument of drawBlockOutline is the ARGB colour.
    @ModifyVariable(method = "drawBlockOutline", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private int orbit$outline(int color) {
        CustomBlockOutline m = VantaClient.mod(CustomBlockOutline.class);
        return m != null && m.enabled ? m.argb() : color;
    }
}
