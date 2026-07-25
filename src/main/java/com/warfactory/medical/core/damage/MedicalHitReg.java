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
        AABB box = entity.getBoundingBox();
        if (MedicalConfig.hitRegistrationMode() == HitRegMode.OFF) {
            return box;
        }
        if (!isEnvelopeTarget(entity) || !(entity instanceof LivingEntity living)) {
            return box;
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
            return box;
        }
        return box.inflate(h, v, h);
    }
}
