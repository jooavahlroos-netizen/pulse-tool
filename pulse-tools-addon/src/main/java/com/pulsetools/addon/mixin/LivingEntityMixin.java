package com.pulsetools.addon.mixin;

import com.pulsetools.addon.modules.SwingSpeed;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "getHandSwingDuration", at = @At("HEAD"), cancellable = true, require = 0)
    private void pulsetools$swingDuration(CallbackInfoReturnable<Integer> cir) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || (Object) this != mc.player) return;

        Modules modules = Modules.get();
        if (modules == null) return;

        SwingSpeed module = modules.get(SwingSpeed.class);
        if (module != null && module.isActive()) {
            cir.setReturnValue(module.getSwingDuration());
        }
    }
}
