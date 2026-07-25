package com.warfactory.medical.core.substance;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SubstanceRegistry {

    public static final String MORPHINE_ITEM_ID = "wfmedical:morphine_syringe";
    public static final String NALOXONE_ITEM_ID = "wfmedical:naloxone_syringe";
    public static final String COMBAT_STIMULANT_ITEM_ID = "wfmedical:combat_stimulant_i";
    private static volatile SubstanceRegistry active = withDefaults();
    private final Map<String, Substance> byItemId = new LinkedHashMap<>();

    public SubstanceRegistry() {
    }

    public static SubstanceRegistry active() {
        return active;
    }

    public static void setActive(SubstanceRegistry registry) {
        active = registry != null ? registry : withDefaults();
    }

    public static Substance defaultMorphine() {
        return new Substance(
                "morphine", MORPHINE_ITEM_ID,
                0.95F,
                0.5F,
                1.0F,
                200,
                1.6F,
                false,
                0.0F,
                40,
                0.0D,
                0.0F,
                0.0F,
                0);
    }

    public static Substance defaultNaloxone() {
        return new Substance(
                "naloxone", NALOXONE_ITEM_ID,
                0.0F,
                0.0F,
                0.0F,
                0,
                0.0F,
                true,
                3.0F,
                30,
                0.0D,
                0.0F,
                0.0F,
                0);
    }

    public static Substance defaultCombatStimulant() {
        return new Substance(
                "combat_stimulant_i", COMBAT_STIMULANT_ITEM_ID,
                0.0F,
                1.4F,
                1.6F,
                200,
                2.6F,
                false,
                0.0F,
                40,
                0.0D,
                1.0F,
                0.97F,
                3600);
    }

    public static SubstanceRegistry withDefaults() {
        SubstanceRegistry r = new SubstanceRegistry();
        r.registerDefaults();
        return r;
    }

    public Substance register(Substance substance) {
        byItemId.put(substance.itemId(), substance);
        return substance;
    }


    public Substance get(String itemId) {
        return itemId == null ? null : byItemId.get(itemId);
    }

    public boolean contains(String itemId) {
        return byItemId.containsKey(itemId);
    }

    public Collection<Substance> all() {
        return Collections.unmodifiableCollection(byItemId.values());
    }

    public int size() {
        return byItemId.size();
    }

    public void clear() {
        byItemId.clear();
    }

    public void registerDefaults() {
        register(defaultMorphine());
        register(defaultNaloxone());
        register(defaultCombatStimulant());
    }
}
