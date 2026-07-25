package com.warfactory.medical.server;

import com.warfactory.medical.capability.IMedicalData;
import com.warfactory.medical.capability.MedicalCapabilities;
import com.warfactory.medical.config.MedicalConfig;
import com.warfactory.medical.core.MedicalProfile;
import com.warfactory.medical.core.limb.Limb;
import com.warfactory.medical.core.limb.LimbType;
import com.warfactory.medical.core.trauma.Trauma;
import com.warfactory.medical.core.treatment.Treatment;
import com.warfactory.medical.core.treatment.TreatmentAction;
import net.minecraft.server.level.ServerPlayer;

public final class TreatmentService {

    private static final float DEFAULT_HEAL_MAGNITUDE = 0.25F;

    private TreatmentService() {
    }

    public static boolean apply(ServerPlayer player, Treatment treatment) {
        return applyTargeted(player, treatment, null);
    }

    public static boolean applyTargeted(ServerPlayer player, Treatment treatment, LimbType limbHint) {
        if (player == null) {
            return false;
        }
        IMedicalData data = MedicalCapabilities.get(player);
        if (data == null) {
            return false;
        }
        return applyTargeted(data, player.level().getGameTime(), treatment, limbHint);
    }

    public static boolean applyTargeted(IMedicalData data, long gameTime, Treatment treatment, LimbType limbHint) {
        if (data == null || treatment == null) {
            return false;
        }
        MedicalProfile profile = data.getProfile();
        TreatmentAction action = treatment.action();

        if (action == TreatmentAction.RESTORE_BLOOD) {
            double before = profile.getBloodMl();
            if (treatment.bloodRestoreMl() <= 0.0D || before >= profile.getMaxBloodMl()) {
                return false;
            }
            profile.setBloodMl(before + treatment.bloodRestoreMl());
            boolean changed = profile.getBloodMl() != before;
            if (changed) {
                data.bumpRevision();
            }
            return changed;
        }

        if (action == TreatmentAction.REDUCE_PAIN) {
            float mag = treatment.magnitude() > 0.0F ? treatment.magnitude() : DEFAULT_HEAL_MAGNITUDE;
            float before = profile.getPainSuppression();
            profile.setPainSuppression(Math.max(before, mag));
            boolean changed = profile.getPainSuppression() != before;
            if (changed) {
                data.bumpRevision();
            }
            return changed;
        }

        if (action == TreatmentAction.NUMB_LIMB) {
            if (limbHint == null) {
                return false;
            }
            float mag = treatment.magnitude() > 0.0F ? treatment.magnitude() : DEFAULT_HEAL_MAGNITUDE;
            Limb limb = profile.limb(limbHint);
            float before = limb.getLocalNumbing();
            limb.setLocalNumbing(Math.max(before, mag));
            if (limb.getLocalNumbing() == before) {
                return false;
            }
            profile.markDirty();
            data.bumpRevision();
            return true;
        }

        if (action == TreatmentAction.BOOST_CLOTTING) {
            float mag = treatment.magnitude() > 0.0F ? treatment.magnitude() : DEFAULT_HEAL_MAGNITUDE;
            long end = gameTime + MedicalConfig.clottingAgentDurationTicks();
            float beforeBoost = profile.getClottingBoost();
            long beforeEnd = profile.getClottingBoostEndTick();
            float newBoost = Math.max(beforeBoost, mag);
            long newEnd = Math.max(beforeEnd, end);
            if (newBoost == beforeBoost && newEnd == beforeEnd) {
                return false;
            }
            profile.setClottingBoost(newBoost);
            profile.setClottingBoostEndTick(newEnd);
            profile.markDirty();
            data.bumpRevision();
            return true;
        }

        Trauma target = pickTarget(profile, treatment, action, limbHint);
        if (target == null) {
            if (treatment.bloodRestoreMl() > 0.0D && profile.getBloodMl() < profile.getMaxBloodMl()) {
                double before = profile.getBloodMl();
                profile.setBloodMl(before + treatment.bloodRestoreMl());
                if (profile.getBloodMl() != before) {
                    data.bumpRevision();
                    return true;
                }
            }
            return false;
        }

        float magnitude = treatment.magnitude() > 0.0F ? treatment.magnitude() : DEFAULT_HEAL_MAGNITUDE;
        boolean removed = false;
        boolean changed = false;

        switch (action) {
            case REDUCE_BLEEDING -> {
                if (!target.isTreated()) {
                    target.setTreated(true);
                    changed = true;
                }
            }
            case SUTURE_WOUND -> {
                if (!target.isSutured()) {
                    target.setSutured(true);
                    target.setTreated(true);
                    changed = true;
                }
            }
            case STABILIZE_FRACTURE -> {
                if (!target.isStabilized()) {
                    target.setStabilized(true);
                    changed = true;
                }
            }
            case HEAL_TRAUMA, TREAT_BURN, TREAT_RADIATION -> {
                target.setSeverity(target.getSeverity() - magnitude);
                target.setTreated(true);
                changed = true;
                if (treatment.removesTrauma() || target.getSeverity() <= 0.0F) {
                    removed = true;
                }
            }
            default -> {
                return false;
            }
        }

        if (treatment.removesTrauma() && !removed && changed) {
            removed = true;
        }

        Limb limb = profile.limb(target.getLimb());
        if (removed) {
            limb.removeTrauma(target);
            changed = true;
        }

        if (treatment.bloodRestoreMl() > 0.0D && profile.getBloodMl() < profile.getMaxBloodMl()) {
            profile.setBloodMl(profile.getBloodMl() + treatment.bloodRestoreMl());
            changed = true;
        }

        if (!changed) {
            return false;
        }

        limb.markDirty();
        profile.markDirty();
        data.bumpRevision();
        return true;
    }

    private static Trauma pickTarget(MedicalProfile profile, Treatment treatment, TreatmentAction action,
                                     LimbType limbHint) {
        Trauma best = null;
        float bestScore = Float.NEGATIVE_INFINITY;
        for (LimbType lt : LimbType.VALUES) {
            if (limbHint != null && lt != limbHint) {
                continue;
            }
            Limb limb = profile.limb(lt);
            java.util.List<Trauma> traumas = limb.getTraumas();
            for (int i = 0; i < traumas.size(); i++) {
                Trauma t = traumas.get(i);
                if (!treatment.appliesTo(t.getType().getCategory())) {
                    continue;
                }
                if (!t.getType().respondsTo(action)) {
                    continue;
                }
                float score = priority(action, t) + t.getSeverity();
                if (score > bestScore) {
                    bestScore = score;
                    best = t;
                }
            }
        }
        return best;
    }

    public static boolean canTreatLimb(MedicalProfile profile, Treatment treatment, LimbType limbType) {
        if (profile == null || treatment == null || limbType == null) {
            return false;
        }
        TreatmentAction action = treatment.action();
        if (action.isGlobal()) {
            return true;
        }
        Limb limb = profile.limb(limbType);
        if (action == TreatmentAction.NUMB_LIMB) {
            return limb.getCachedPain() > 0.0F;
        }
        if (action == TreatmentAction.APPLY_TOURNIQUET) {
            return (limbType.isArm() || limbType.isLeg()) && !limb.hasTourniquet();
        }
        for (Trauma t : limb.getTraumas()) {
            if (treatment.appliesTo(t.getType().getCategory()) && t.getType().respondsTo(action)) {
                return true;
            }
        }
        return false;
    }

    public static int treatableMask(MedicalProfile profile, Treatment treatment) {
        int mask = 0;
        for (LimbType lt : LimbType.VALUES) {
            if (canTreatLimb(profile, treatment, lt)) {
                mask |= (1 << lt.ordinal());
            }
        }
        return mask;
    }

    private static float priority(TreatmentAction action, Trauma t) {
        return switch (action) {
            case REDUCE_BLEEDING, SUTURE_WOUND -> {
                float bonus = t.bleeding() > 0.0F ? 2.0F : 0.0F;
                if (t.isTreated() || t.isSutured()) {
                    bonus -= 1.0F;
                }
                yield bonus;
            }
            case STABILIZE_FRACTURE -> {
                float bonus = t.isFracture() ? 2.0F : 0.0F;
                if (t.isStabilized()) {
                    bonus -= 1.0F;
                }
                yield bonus;
            }
            case HEAL_TRAUMA, TREAT_BURN, TREAT_RADIATION, REDUCE_PAIN -> t.isTreated() ? -0.5F : 0.5F;
            default -> 0.0F;
        };
    }
}
