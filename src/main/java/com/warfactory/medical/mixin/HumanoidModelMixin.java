package com.warfactory.medical.mixin;

import com.warfactory.medical.WFMedical;
import com.warfactory.medical.client.ClientDownedTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin {

    private static final float WFMEDICAL$TILT_RANGE = 0.28F;
    @Shadow
    public ModelPart head;
    @Shadow
    public ModelPart rightArm;
    @Shadow
    public ModelPart leftArm;
    @Shadow
    public ModelPart rightLeg;
    @Shadow
    public ModelPart leftLeg;

    private static void wfmedical$sprawl(ModelPart part, Random rng) {
        part.xRot += wfmedical$tilt(rng);
        part.yRot += wfmedical$tilt(rng);
        part.zRot += wfmedical$tilt(rng);
    }

    private static float wfmedical$tilt(Random rng) {
        return (rng.nextFloat() * 2.0F - 1.0F) * WFMEDICAL$TILT_RANGE;
    }

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void wfmedical$sprawlWhenDowned(LivingEntity entity, float limbSwing, float limbSwingAmount,
                                            float ageInTicks, float netHeadYaw, float headPitch,
                                            CallbackInfo callbackInfo) {
        try {
            this.head.zRot = 0.0F;
            if (!(entity instanceof Player player) || !ClientDownedTracker.isDowned(player.getId())) {
                return;
            }
            Minecraft minecraft = Minecraft.getInstance();
            if (player == minecraft.player && minecraft.options.getCameraType().isFirstPerson()) {
                return;
            }
            Random rng = new Random(player.getId());
            this.head.xRot = wfmedical$tilt(rng) * 0.5F;
            this.head.yRot = wfmedical$tilt(rng) * 0.5F;
            this.head.zRot = wfmedical$tilt(rng);
            wfmedical$sprawl(this.rightArm, rng);
            wfmedical$sprawl(this.leftArm, rng);
            wfmedical$sprawl(this.rightLeg, rng);
            wfmedical$sprawl(this.leftLeg, rng);
        } catch (Throwable t) {
            WFMedical.LOGGER.warn("[{}] Downed limb sprawl failed; skipping this frame", WFMedical.MOD_ID, t);
        }
    }
}
