package dev.vanta.client.mixin;

import dev.vanta.client.VantaClient;
import dev.vanta.client.module.PerfModules.Particles;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.particle.ParticleEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleManager.class)
public class ParticleManagerMixin {
    private static boolean orbit$hideParticles() {
        Particles module = VantaClient.mod(Particles.class);
        return module != null && module.hidesAll();
    }

    @Inject(
            method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;",
            at = @At("HEAD"), cancellable = true, require = 0
    )
    private void orbit$cancelCreatedParticle(ParticleEffect effect, double x, double y, double z,
                                             double velocityX, double velocityY, double velocityZ,
                                             CallbackInfoReturnable<Particle> cir) {
        if (orbit$hideParticles()) cir.setReturnValue(null);
    }

    @Inject(
            method = "addParticle(Lnet/minecraft/client/particle/Particle;)V",
            at = @At("HEAD"), cancellable = true, require = 0
    )
    private void orbit$cancelQueuedParticle(Particle particle, CallbackInfo ci) {
        if (orbit$hideParticles()) ci.cancel();
    }
}
