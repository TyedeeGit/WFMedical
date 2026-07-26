package com.warfactory.medical.core.damage;

import com.warfactory.medical.WFMedical;
import com.warfactory.medical.config.MedicalConfig;
import com.warfactory.medical.core.limb.LimbType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

public final class HitLocation {

    private HitLocation() {
    }

    public static LimbType pick(LivingEntity victim, DamageSource src, DamageCategory cat, RandomSource rand) {
        if (victim != null && MedicalConfig.geometricHitLocation()) {
            LimbType g = HitGeometry.classifyHit(victim, src, cat);
            if (g != null) {
                return g;
            }
            warnUntraceable(victim, src, cat);
        }
        LimbType weighted = pickWeighted(src, cat, rand);
        if (victim != null && MedicalConfig.logHitDetection()) {
            HitDetectionDebug.logRandomFallback(victim, src, cat, weighted);
        }
        return weighted;
    }

    public static List<LimbType> pickPierced(LivingEntity victim, DamageSource src, DamageCategory cat,
                                             RandomSource rand) {
        if (victim != null && MedicalConfig.geometricHitLocation()) {
            List<LimbType> pierced = HitGeometry.classifyHitPierced(victim, src, cat);
            if (!pierced.isEmpty()) {
                return pierced;
            }
            warnUntraceable(victim, src, cat);
        }
        LimbType weighted = pickWeighted(src, cat, rand);
        if (victim != null && MedicalConfig.logHitDetection()) {
            HitDetectionDebug.logRandomFallback(victim, src, cat, weighted);
        }
        return List.of(weighted);
    }

    private static void warnUntraceable(LivingEntity victim, DamageSource src, DamageCategory cat) {
        if (src == null) {
            return;
        }
        Entity attacker = src.getEntity();
        Entity direct = src.getDirectEntity();
        boolean directional = (attacker != null && attacker != victim) || (direct != null && direct != victim);
        if (!directional) {
            return;
        }
        if (HitGeometry.resolveHitPoint(victim, src, cat) != null) {
            return;
        }
        WFMedical.LOGGER.warn(
                "Untraceable hit direction: could not reconstruct a hit position on {} for damage '{}' (category "
                        + "{}, attacker={}, projectile={}) -- fell back to weighted limb sampling.",
                victim.getName().getString(),
                src.getMsgId(),
                cat,
                attacker != null ? attacker.getName().getString() : "none",
                direct != null && direct != attacker ? direct.getName().getString() : "none");
    }

    private static LimbType pickWeighted(DamageSource source, DamageCategory cat, RandomSource rand) {
        LimbType[] limbs = LimbType.values();
        float[] weights = new float[limbs.length];
        float total = 0.0F;
        for (int i = 0; i < limbs.length; i++) {
            float w = limbs[i].getHitWeight() * categoryBias(limbs[i], cat);
            if (w < 0.0F) {
                w = 0.0F;
            }
            weights[i] = w;
            total += w;
        }
        if (total <= 0.0F || rand == null) {
            return LimbType.TORSO;
        }
        float roll = rand.nextFloat() * total;
        float acc = 0.0F;
        for (int i = 0; i < limbs.length; i++) {
            acc += weights[i];
            if (roll < acc) {
                return limbs[i];
            }
        }
        return limbs[limbs.length - 1];
    }

    private static float categoryBias(LimbType limb, DamageCategory cat) {
        if (cat == null) {
            return 1.0F;
        }
        switch (cat) {
            case BALLISTIC:
            case PIERCING:
                if (limb == LimbType.TORSO) {
                    return 1.35F;
                }
                if (limb == LimbType.HEAD) {
                    return 1.5F;
                }
                return 0.85F;
            case FALL:
                if (limb.isLeg()) {
                    return 2.5F;
                }
                if (limb == LimbType.TORSO) {
                    return 0.6F;
                }
                return 0.0F;
            case EXPLOSION:
                return limb.isVital() ? 0.9F : 1.2F;
            default:
                return 1.0F;
        }
    }
}
