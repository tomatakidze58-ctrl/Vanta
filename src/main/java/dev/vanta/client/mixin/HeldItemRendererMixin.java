package dev.vanta.client.mixin;

import dev.vanta.client.VantaClient;
import dev.vanta.client.module.PvpModules.LowShield;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {
    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"), require = 0)
    private void orbit$shieldPush(AbstractClientPlayerEntity player, float tickProgress, float pitch, Hand hand,
                                     float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices,
                                     OrderedRenderCommandQueue queue, int light, CallbackInfo ci) {
        matrices.push();
        LowShield m = VantaClient.mod(LowShield.class);
        if (m != null && m.enabled && item.isOf(Items.SHIELD)) {
            matrices.translate(0.0, -m.amount.n, 0.0);
        }
    }

    @Inject(method = "renderFirstPersonItem", at = @At("RETURN"), require = 0)
    private void orbit$shieldPop(AbstractClientPlayerEntity player, float tickProgress, float pitch, Hand hand,
                                    float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices,
                                    OrderedRenderCommandQueue queue, int light, CallbackInfo ci) {
        matrices.pop();
    }
}
