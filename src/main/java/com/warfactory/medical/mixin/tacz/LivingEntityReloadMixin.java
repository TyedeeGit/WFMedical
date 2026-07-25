package com.warfactory.medical.mixin.tacz;

import com.tacz.guns.entity.shooter.LivingEntityReload;
import com.warfactory.medical.api.MedicalState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LivingEntityReload.class, remap = false)
public abstract class LivingEntityReloadMixin {

    @Shadow
    @Final
    private LivingEntity shooter;

    @Inject(method = "reload", at = @At("HEAD"), cancellable = true)
    private void wfmedical$blockReloadWhenDowned(CallbackInfo ci) {
        if (shooter instanceof Player player && MedicalState.isDowned(player)) {
            ci.cancel();
        }
    }
}
