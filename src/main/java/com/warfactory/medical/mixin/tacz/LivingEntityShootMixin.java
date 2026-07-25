package com.warfactory.medical.mixin.tacz;

import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.entity.shooter.LivingEntityShoot;
import com.warfactory.medical.api.MedicalState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LivingEntityShoot.class, remap = false)
public abstract class LivingEntityShootMixin {

    @Shadow
    @Final
    private LivingEntity shooter;

    @Inject(method = "shoot", at = @At("HEAD"), cancellable = true)
    private void wfmedical$blockShootWhenDowned(CallbackInfoReturnable<ShootResult> cir) {
        if (shooter instanceof Player player && MedicalState.isDowned(player)) {
            cir.setReturnValue(ShootResult.FORGE_EVENT_CANCEL);
        }
    }
}
