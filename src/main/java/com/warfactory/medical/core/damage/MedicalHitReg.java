package com.warfactory.medical.core.damage;

import com.warfactory.medical.compat.OpenPersistenceCompat;
import com.warfactory.medical.config.MedicalConfig;
import com.warfactory.medical.core.damage.rig.HumanoidRig;
import com.warfactory.medical.core.damage.rig.RigTuning;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

public final class MedicalHitReg {

    private MedicalHitReg() {
    }

    public static boolean isEnvelopeTarget(Entity entity) {
        return entity instanceof Player || OpenPersistenceCompat.isPersistentBody(entity);
    }

    public static AABB registrationBox(Entity entity) {
        return registrationBox(entity, entity.getBoundingBox());
    }

    /**
     * Same envelope inflation as {@link #registrationBox(Entity)}, but applied to a caller-supplied base box
     * rather than the entity's live {@code getBoundingBox()}. Lets callers that already have their own
     * (e.g. lag-compensated) box for the entity widen it by the same pose-aware margins instead of
     * discarding that adjustment in favor of the plain current box.
     */
    public static AABB registrationBox(Entity entity, AABB base) {
        if (MedicalConfig.hitRegistrationMode() == HitRegMode.OFF) {
            return base;
        }
        if (!isEnvelopeTarget(entity) || !(entity instanceof LivingEntity living)) {
            return base;
        }
        RigTuning.RigPose pose = HumanoidRig.resolvePose(living);
        double h;
        double v;
        if (RigTuning.ACTIVE) {
            h = RigTuning.envReach(pose, RigTuning.EnvAxis.HORIZONTAL);
            v = RigTuning.envReach(pose, RigTuning.EnvAxis.VERTICAL);
        } else {
            h = MedicalConfig.hitEnvelopeReachHorizontal(pose);
            v = MedicalConfig.hitEnvelopeReachVertical(pose);
        }
        if (h <= 0.0 && v <= 0.0) {
            return base;
        }
        return base.inflate(h, v, h);
    }
}
