package com.warfactory.medical.core.damage;

import com.warfactory.medical.core.limb.LimbType;
import com.warfactory.medical.core.trauma.Trauma;
import com.warfactory.medical.core.trauma.TraumaCategory;
import com.warfactory.medical.core.trauma.TraumaRegistry;
import com.warfactory.medical.core.trauma.TraumaType;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

public final class TraumaGenerator {

    private static final String BRUISE = "bruise";
    private static final String LACERATION_SMALL = "laceration_small";
    private static final String LACERATION_LARGE = "laceration_large";
    private static final String FRACTURE = "fracture";
    private static final String BURN = "burn";
    private static final String INTERNAL_BLEEDING = "internal_bleeding";
    private static final String PUNCTURE = "puncture";
    private static final String CRUSH_INJURY = "crush_injury";
    private static final String RADIATION_BURN = "radiation_burn";
    private static final String CHEMICAL_BURN = "chemical_burn";
    private static final String BLUNT_FORCE_TRAUMA = "blunt_force_trauma";

    // Fall damage reaching us is roughly (fallDistance - 3) health points, so ~7 energy ~= a 10-block fall:
    // below that a fall is pure blunt-force trauma (soft, self-healing, no bleed); at/above it a bone may break.
    private static final float FALL_FRACTURE_ENERGY = 7.0F;
    private static final float FALL_FRACTURE_RANGE = 16.0F;

    private static final float MAJOR_ENERGY = 4.0F;

    private TraumaGenerator() {
    }

    public static List<Trauma> generate(DamageCategory cat, ArmorEvaluation.Outcome outcome,
                                        LimbType limb, float energy, TraumaRegistry registry,
                                        long nowTick, RandomSource rand) {
        List<Trauma> out = new ArrayList<>(3);
        if (registry == null || limb == null) {
            return out;
        }
        DamageCategory category = cat == null ? DamageCategory.GENERIC : cat;
        ArmorEvaluation.Outcome result = outcome == null ? ArmorEvaluation.Outcome.FULL : outcome;
        float e = Math.max(energy, 0.0F);
        float energyFactor = clampF(e * 0.1F, 0.1F, 1.5F);

        switch (category) {
            case FIRE -> {
                add(out, registry, BURN, TraumaCategory.BURN, limb, 0.9F * energyFactor, nowTick);
                return out;
            }
            case RADIATION -> {
                add(out, registry, RADIATION_BURN, TraumaCategory.RADIATION_BURN, limb, 0.8F * energyFactor, nowTick);
                return out;
            }
            case CHEMICAL -> {
                add(out, registry, CHEMICAL_BURN, TraumaCategory.CHEMICAL_BURN, limb, 0.8F * energyFactor, nowTick);
                return out;
            }
            case EXPLOSION -> {
                add(out, registry, CRUSH_INJURY, TraumaCategory.CRUSH_INJURY, limb, energyFactor, nowTick);
                add(out, registry, BURN, TraumaCategory.BURN, limb, 0.6F * energyFactor, nowTick);
                maybeFracture(out, registry, limb, nowTick, rand, fractureChance(category, limb, energyFactor));
                return out;
            }
            case FALL -> {
                // Falls are blunt force: soft-tissue trauma that regenerates on its own and never bleeds,
                // but it costs current health (~12 HP per severity). Energy ~= fall damage ~= fallDistance-3,
                // so e*0.085 makes a 10-block fall (e~7) ~0.6 severity ~= a vanilla ~7 HP hit.
                // Only a hard enough landing (>= FALL_FRACTURE_ENERGY, ~10 blocks) risks breaking a bone.
                float blunt = clampF(e * 0.085F, 0.08F, 1.0F);
                add(out, registry, BLUNT_FORCE_TRAUMA, TraumaCategory.BRUISE, limb, blunt, nowTick);
                maybeFracture(out, registry, limb, nowTick, rand, fallFractureChance(limb, e));
                return out;
            }
            case UNARMED -> {
                // Fists are blunt force too -- a light current-health hit.
                add(out, registry, BLUNT_FORCE_TRAUMA, TraumaCategory.BRUISE, limb, 0.12F, nowTick);
                return out;
            }
            default -> {
            }
        }

        switch (result) {
            case BLOCKED -> {
                add(out, registry, BRUISE, TraumaCategory.BRUISE, limb, 0.5F, nowTick);
                return out;
            }
            case PARTIAL -> {
                add(out, registry, BRUISE, TraumaCategory.BRUISE, limb, 0.5F, nowTick);
                add(out, registry, LACERATION_SMALL, TraumaCategory.LACERATION, limb, 0.5F, nowTick);
                return out;
            }
            default -> {
                if (category == DamageCategory.BALLISTIC) {
                    add(out, registry, PUNCTURE, TraumaCategory.PUNCTURE, limb, 0.9F * energyFactor, nowTick);
                    add(out, registry, LACERATION_LARGE, TraumaCategory.LACERATION, limb, 0.7F * energyFactor, nowTick);
                    add(out, registry, INTERNAL_BLEEDING, TraumaCategory.INTERNAL_BLEEDING, limb, 0.6F * energyFactor, nowTick);
                    add(out, registry, LACERATION_SMALL, TraumaCategory.LACERATION, limb, 0.5F, nowTick);
                    maybeFracture(out, registry, limb, nowTick, rand, fractureChance(category, limb, energyFactor));
                    return out;
                }
                boolean impact = category == DamageCategory.BLUNT;
                if (e >= MAJOR_ENERGY) {
                    if (impact) {
                        add(out, registry, CRUSH_INJURY, TraumaCategory.CRUSH_INJURY, limb, 0.8F * energyFactor, nowTick);
                        add(out, registry, BRUISE, TraumaCategory.BRUISE, limb, 0.5F, nowTick);
                    } else {
                        add(out, registry, LACERATION_LARGE, TraumaCategory.LACERATION, limb, 0.8F * energyFactor, nowTick);
                        add(out, registry, INTERNAL_BLEEDING, TraumaCategory.INTERNAL_BLEEDING, limb, 0.5F * energyFactor, nowTick);
                        add(out, registry, LACERATION_SMALL, TraumaCategory.LACERATION, limb, 0.5F, nowTick);
                    }
                    maybeFracture(out, registry, limb, nowTick, rand, fractureChance(category, limb, energyFactor));
                } else if (impact) {
                    // Light blunt blows (fists, weak bonks) are blunt force, not open wounds.
                    add(out, registry, BLUNT_FORCE_TRAUMA, TraumaCategory.BRUISE, limb, 0.15F, nowTick);
                } else {
                    add(out, registry, LACERATION_SMALL, TraumaCategory.LACERATION, limb, 0.45F + 0.08F * e, nowTick);
                }
                return out;
            }
        }
    }

    private static float fractureChance(DamageCategory cat, LimbType limb, float energyFactor) {
        float base = switch (cat) {
            case BALLISTIC -> 0.35F;
            case EXPLOSION -> 0.5F;
            case BLUNT -> 0.4F;
            default -> 0.2F;
        };
        return clampF(base * energyFactor, 0.0F, 0.85F);
    }

    private static float fallFractureChance(LimbType limb, float energy) {
        if (energy < FALL_FRACTURE_ENERGY) {
            return 0.0F;
        }
        float t = clampF((energy - FALL_FRACTURE_ENERGY) / FALL_FRACTURE_RANGE, 0.0F, 1.0F);
        float chance = 0.15F + 0.7F * t;
        if (!limb.isLeg()) {
            chance *= 0.4F;
        }
        return clampF(chance, 0.0F, 0.9F);
    }

    private static void maybeFracture(List<Trauma> out, TraumaRegistry registry, LimbType limb,
                                      long nowTick, RandomSource rand, float chance) {
        if (!(limb.isArm() || limb.isLeg())) {
            return;
        }
        if (rand != null && chance > 0.0F && rand.nextFloat() < chance) {
            add(out, registry, FRACTURE, TraumaCategory.FRACTURE, limb, 1.0F, nowTick);
        }
    }

    private static void add(List<Trauma> out, TraumaRegistry registry, String id, TraumaCategory category,
                            LimbType limb, float severity, long nowTick) {
        TraumaType type = resolve(registry, id, category);
        if (type == null) {
            return;
        }
        float sev = severity * type.getSeverityContribution();
        if (sev <= 0.0F) {
            sev = 0.01F;
        }
        out.add(new Trauma(type, limb, sev, nowTick));
    }

    private static TraumaType resolve(TraumaRegistry registry, String id, TraumaCategory category) {
        TraumaType type = registry.get(id);
        if (type == null) {
            type = registry.firstOfCategory(category);
        }
        return type;
    }

    private static float clampF(float v, float lo, float hi) {
        return v < lo ? lo : (Math.min(v, hi));
    }
}
