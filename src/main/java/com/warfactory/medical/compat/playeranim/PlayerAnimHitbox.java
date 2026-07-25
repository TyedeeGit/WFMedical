package com.warfactory.medical.compat.playeranim;

import com.warfactory.medical.core.damage.rig.HumanoidRig;
import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.core.util.Vec3f;
import dev.kosmx.playerAnim.impl.IAnimatedPlayer;
import dev.kosmx.playerAnim.impl.animation.AnimationApplier;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class PlayerAnimHitbox implements HumanoidRig.AnimSampler {

    private PlayerAnimHitbox() {
    }

    public static void register() {
        HumanoidRig.setAnimSampler(new PlayerAnimHitbox());
    }

    @Override
    public double[] sample(LivingEntity entity, String bone,
                           double x, double y, double z, double xRot, double yRot, double zRot) {
        if (!(entity instanceof AbstractClientPlayer player)) {
            return null;
        }
        AnimationApplier anim = ((IAnimatedPlayer) player).getAnimation();
        if (anim == null || !anim.isActive()) {
            return null;
        }
        Vec3f pos = anim.get3DTransform(bone, TransformType.POSITION,
                new Vec3f((float) x, (float) y, (float) z));
        Vec3f rot = anim.get3DTransform(bone, TransformType.ROTATION,
                new Vec3f((float) xRot, (float) yRot, (float) zRot));
        return new double[] {
                pos.getX().floatValue(), pos.getY().floatValue(), pos.getZ().floatValue(),
                rot.getX().floatValue(), rot.getY().floatValue(), rot.getZ().floatValue()
        };
    }
}
