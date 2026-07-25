package com.warfactory.medical.core;

public record DerivedStats(
        float effectiveMaxHealth,
        float healthModifier,
        float effectiveCurrentHealth,
        double totalBleeding,
        float totalPain,
        float systemicPain,
        float movementMultiplier,
        boolean sprintBlocked,
        float jumpMultiplier,
        HealthState state,
        boolean anyLegFracture,
        boolean anyArmFracture,
        boolean asphyxiating,
        boolean painKoPending,
        boolean bothArmsDisabled,
        boolean bothLegsDisabled,
        boolean anyArmTourniquet
) {
    private static final DerivedStats HEALTHY = new DerivedStats(
            30.0F, 0.0F, 30.0F, 0.0D, 0.0F, 0.0F, 1.0F, false, 1.0F,
            HealthState.HEALTHY, false, false, false, false, false, false, false);

    public static DerivedStats healthy() {
        return HEALTHY;
    }

    public boolean unconscious() {
        return state() == HealthState.UNCONSCIOUS;
    }

    @Override
    public boolean asphyxiating() {
        return asphyxiating;
    }
}
